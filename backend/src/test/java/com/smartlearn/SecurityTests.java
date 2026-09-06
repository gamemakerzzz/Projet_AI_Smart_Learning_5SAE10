package com.smartlearn;

import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.CompetenceRepository;
import com.smartlearn.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:smartlearn-security;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "smartlearn.admin.password=test-admin-password"
})
@AutoConfigureMockMvc
@Transactional
class SecurityTests {
    @Autowired MockMvc mvc;
    @Autowired ApprenantRepository users;
    @Autowired CompetenceRepository competences;
    long id(String email) { return users.findByEmailIgnoreCase(email).orElseThrow().getId(); }

    @Test void anonymousCannotReadData() throws Exception {
        mvc.perform(get("/api/formations")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/apprenants")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @org.springframework.test.annotation.DirtiesContext(methodMode = org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD)
    @Test void loginSessionAndLogoutWithRealCsrfCookie() throws Exception {
        var csrfResponse = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var cookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        var login = mvc.perform(post("/api/auth/login").cookie(cookie)
                .header("X-XSRF-TOKEN", cookie.getValue())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "ahmed@example.com").param("password", "password123"))
            .andExpect(status().isNoContent()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.password").doesNotExist());
        var refreshed = mvc.perform(get("/api/auth/csrf").session(session)).andReturn()
            .getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(refreshed);
        mvc.perform(post("/api/auth/logout").session(session).cookie(refreshed)
                .header("X-XSRF-TOKEN", refreshed.getValue())).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void invalidPasswordAndMissingCsrfAreRejected() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).param("email", "ahmed@example.com")
            .param("password", "wrong")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").param("email", "ahmed@example.com")
            .param("password", "password123")).andExpect(status().isForbidden());
        mvc.perform(post("/api/formations").with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test void adminCanLogin() throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(csrf())
            .param("email", "admin@smartlearn.local").param("password", "test-admin-password"))
            .andExpect(status().isNoContent()).andReturn();
        mvc.perform(get("/api/auth/me").session((MockHttpSession) result.getRequest().getSession(false)))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test void userCanReadCatalogueAndEditOwnProfile() throws Exception {
        var learner = user("ahmed@example.com").roles("USER");
        mvc.perform(get("/api/formations").with(learner)).andExpect(status().isOk());
        mvc.perform(get("/api/apprenants/" + id("ahmed@example.com")).with(learner)).andExpect(status().isOk());
        mvc.perform(put("/api/apprenants/" + id("ahmed@example.com") + "/profil")
            .with(learner).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"objectif\":\"Learn Java\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.objectif").value("Learn Java"));
    }

    @Test void userCannotManageCatalogueOrAccounts() throws Exception {
        var learner = user("ahmed@example.com").roles("USER");
        mvc.perform(get("/api/apprenants").with(learner)).andExpect(status().isForbidden());
        for (String path : new String[]{"/api/formations", "/api/categories", "/api/apprenants"}) {
            mvc.perform(post(path).with(learner).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
        }
        mvc.perform(delete("/api/chapitres/1").with(learner).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/apprenants/" + id("ahmed@example.com")).with(learner).with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test void userCannotAccessAnotherLearner() throws Exception {
        var learner = user("ahmed@example.com").roles("USER");
        long other = id("sarah@example.com");
        for (String path : new String[]{"/api/apprenants/" + other, "/api/apprenants/" + other + "/profil",
            "/api/apprenants/" + other + "/competences", "/api/recommendations/apprenant/" + other}) {
            mvc.perform(get(path).with(learner)).andExpect(status().isForbidden());
        }
        mvc.perform(put("/api/apprenants/" + other + "/profil").with(learner).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        long skill = competences.findByApprenantId(other).get(0).getId();
        mvc.perform(delete("/api/competences/" + skill).with(learner).with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test void createdAccountsCannotChooseAdminRole() throws Exception {
        mvc.perform(post("/api/apprenants").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nom\":\"Test\",\"prenom\":\"User\",\"email\":\"new@example.com\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isCreated());
        assertEquals(Role.USER, users.findByEmailIgnoreCase("new@example.com").orElseThrow().getRole());
    }
    @Test void dashboardIsAdminOnlyAndExcludesAdminFromLearnerCount() throws Exception {
        mvc.perform(get("/api/dashboard").with(user("ahmed@example.com").roles("USER")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apprenants").value(2))
            .andExpect(jsonPath("$.profils").value(2))
            .andExpect(jsonPath("$.competences").value(5));
    }
}
