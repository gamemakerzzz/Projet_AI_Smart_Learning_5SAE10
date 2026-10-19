package com.smartlearn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.FormationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:merged-modules;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "smartlearn.admin.password=test-admin-password", "IA_SERVICE_URL=http://127.0.0.1:1"
})
@AutoConfigureMockMvc
@Transactional
class MergedModulesTests {
    @Autowired MockMvc mvc;
    @Autowired ApprenantRepository users;
    @Autowired FormationRepository formations;
    @Autowired ObjectMapper mapper;

    @Test void learnerCanUseBranchModulesAndFlashcardsTogether() throws Exception {
        long learner = users.findByEmailIgnoreCase("ahmed@example.com").orElseThrow().getId();
        for (String module : new String[]{"examens", "seances", "notifications", "statistiques", "chapitres-revoir", "planification", "analyse-ia"}) {
            mvc.perform(get("/api/" + module + "/apprenant/" + learner).with(user("ahmed@example.com").roles("USER")))
                .andExpect(status().isOk());
        }
        mvc.perform(get("/api/flashcards/status").with(user("ahmed@example.com").roles("USER")))
            .andExpect(status().isOk());
    }

    @Test void examsAndPlanningPersistAndEnforceOwnership() throws Exception {
        long learner = users.findByEmailIgnoreCase("ahmed@example.com").orElseThrow().getId();
        long course = formations.findAll().get(0).getId();
        String exam = "{\"titre\":\"Merged exam\",\"dateExamen\":\"2027-01-12T10:00:00\",\"dureeMinutes\":60,\"formationId\":" + course + "}";
        var response = mvc.perform(post("/api/examens/apprenant/" + learner).with(user("admin").roles("ADMIN")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(exam)).andExpect(status().isCreated()).andReturn();
        long examId = mapper.readTree(response.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(delete("/api/examens/" + examId).with(user("sarah@example.com").roles("USER")).with(csrf()))
            .andExpect(status().isForbidden());
        String plan = "{\"apprenantId\":" + learner + ",\"dateDebut\":\"2027-01-10\",\"dateFin\":\"2027-01-11\",\"formationIds\":[" + course + "]}";
        mvc.perform(post("/api/planification").with(user("ahmed@example.com").roles("USER")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(plan)).andExpect(status().isCreated()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/seances/apprenant/" + learner).with(user("ahmed@example.com").roles("USER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(delete("/api/examens/" + examId).with(user("admin").roles("ADMIN")).with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Autowired com.smartlearn.service.NotificationRappelScheduler reminders;
    @Autowired com.smartlearn.repository.NotificationRepository notifications;

    @Test void updatedExamPermissionsAndNotificationReadState() throws Exception {
        long learner = users.findByEmailIgnoreCase("ahmed@example.com").orElseThrow().getId();
        String exam = "{\"titre\":\"Permission exam\",\"dateExamen\":\"2027-02-12T10:00:00\",\"dureeMinutes\":60}";
        mvc.perform(post("/api/examens/apprenant/" + learner).with(user("ahmed@example.com").roles("USER")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(exam)).andExpect(status().isForbidden());
        mvc.perform(get("/api/examens").with(user("ahmed@example.com").roles("USER"))).andExpect(status().isForbidden());
        long before = notifications.countByApprenantIdAndStatut(learner, "NON_LU");
        var response = mvc.perform(post("/api/examens/apprenant/" + learner).with(user("admin").roles("ADMIN")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(exam)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.apprenantId").value(learner)).andReturn();
        org.junit.jupiter.api.Assertions.assertEquals(before + 1, notifications.countByApprenantIdAndStatut(learner, "NON_LU"));
        mvc.perform(patch("/api/notifications/apprenant/" + learner + "/mark-all-read")
            .with(user("ahmed@example.com").roles("USER")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/notifications/apprenant/" + learner + "/unread-count")
            .with(user("ahmed@example.com").roles("USER"))).andExpect(jsonPath("$.count").value(0));
        mvc.perform(get("/api/notifications/apprenant/" + learner + "/unread-count")
            .with(user("sarah@example.com").roles("USER"))).andExpect(status().isForbidden());
    }

    @Test void aiPlanningValidatesDatesAndOwnershipAndProducesRevisionSessions() throws Exception {
        long learner = users.findByEmailIgnoreCase("ahmed@example.com").orElseThrow().getId();
        long course = formations.findAll().get(0).getId();
        String exam = "{\"titre\":\"AI planning exam\",\"dateExamen\":\"2027-03-12T10:00:00\",\"dureeMinutes\":60,\"formationId\":" + course + "}";
        mvc.perform(post("/api/examens/apprenant/" + learner).with(user("admin").roles("ADMIN")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(exam)).andExpect(status().isCreated());
        String plan = "{\"apprenantId\":" + learner + ",\"dateDebut\":\"2027-03-10\",\"dateFin\":\"2027-03-11\",\"formationIds\":[" + course + "]}";
        mvc.perform(post("/api/planification-ia/generer").with(user("ahmed@example.com").roles("USER")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(plan)).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSeances").value(2));
        mvc.perform(post("/api/planification-ia/generer").with(user("sarah@example.com").roles("USER")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(plan)).andExpect(status().isForbidden());
        mvc.perform(post("/api/planification-ia/generer").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(plan.replace("2027-03-11", "2027-03-09")))
            .andExpect(status().isBadRequest());
        String slots = "[{\"heure\":10,\"jourSemaine\":1,\"dureeMin\":60,\"joursAvantExamen\":3,\"seancesMemeJour\":0}]";
        mvc.perform(post("/api/seance/probabilite/" + learner).with(user("ahmed@example.com").roles("USER")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(slots)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].probabilite").isNumber());
        mvc.perform(post("/api/seance/probabilite/" + learner).with(user("sarah@example.com").roles("USER")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(slots)).andExpect(status().isForbidden());
    }

    @Test void automaticRemindersAreNotDuplicated() throws Exception {
        long learner = users.findByEmailIgnoreCase("ahmed@example.com").orElseThrow().getId();
        String date = java.time.LocalDateTime.now().plusHours(24).toString();
        String exam = "{\"titre\":\"Reminder exam\",\"dateExamen\":\"" + date + "\",\"dureeMinutes\":60}";
        mvc.perform(post("/api/examens/apprenant/" + learner).with(user("admin").roles("ADMIN")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(exam)).andExpect(status().isCreated());
        long before = notifications.countByApprenantId(learner);
        reminders.checkRappels();
        org.junit.jupiter.api.Assertions.assertEquals(before + 1, notifications.countByApprenantId(learner));
        reminders.checkRappels();
        org.junit.jupiter.api.Assertions.assertEquals(before + 1, notifications.countByApprenantId(learner));
    }
}
