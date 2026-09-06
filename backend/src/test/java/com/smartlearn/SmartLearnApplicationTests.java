package com.smartlearn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.dto.ApprenantRequest;
import com.smartlearn.dto.CategorieRequest;
import com.smartlearn.dto.FormationRequest;
import com.smartlearn.entity.Categorie;
import com.smartlearn.repository.CategorieRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartlearn-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "smartlearn.admin.password=test-admin-password"
})
@org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
@AutoConfigureMockMvc
@org.springframework.transaction.annotation.Transactional
public class SmartLearnApplicationTests {

    @Autowired
    private com.smartlearn.repository.ApprenantRepository apprenantRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategorieRepository categorieRepository;

    @Test
    @DisplayName("Test 1: Récupération de la liste des formations")
    void testGetFormations() throws Exception {
        mockMvc.perform(get("/api/formations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Test 2: Création d'une nouvelle formation")
    void testCreateFormation() throws Exception {
        Categorie cat = new Categorie();
        cat.setNom("Test Catégorie");
        cat.setDescription("Description test");
        cat = categorieRepository.save(cat);

        FormationRequest req = new FormationRequest();
        req.setTitre("Nouvelle Formation Unit Test");
        req.setDescription("Description de test unitaire");
        req.setPrix(new BigDecimal("99.99"));
        req.setNiveau("Débutant");
        req.setMotsCles("test unit java");
        req.setCategorieId(cat.getId());

        mockMvc.perform(post("/api/formations").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Nouvelle Formation Unit Test"))
                .andExpect(jsonPath("$.prix").value(99.99));
    }

    @Test
    @DisplayName("Test 3: Création d'un apprenant")
    void testCreateApprenant() throws Exception {
        ApprenantRequest req = new ApprenantRequest();
        req.setNom("Durand");
        req.setPrenom("Pierre");
        req.setEmail("pierre.durand.test@example.com");
        req.setPassword("password123");

        mockMvc.perform(post("/api/apprenants").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("pierre.durand.test@example.com"))
                .andExpect(jsonPath("$.nom").value("Durand"));
    }

    @Test
    @DisplayName("Test 4: Endpoint de recommandation MLA")
    void testGetRecommendations() throws Exception {
        mockMvc.perform(get("/api/recommendations/apprenant/" + apprenantRepository.findByEmailIgnoreCase("ahmed@example.com").orElseThrow().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}
