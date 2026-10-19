package com.smartlearn.config;

import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Categorie;
import com.smartlearn.entity.Chapitre;
import com.smartlearn.entity.Competence;
import com.smartlearn.entity.Formation;
import com.smartlearn.entity.Profil;
import com.smartlearn.repository.ApprenantRepository;
import com.smartlearn.repository.CategorieRepository;
import com.smartlearn.repository.ChapitreRepository;
import com.smartlearn.repository.CompetenceRepository;
import com.smartlearn.repository.FormationRepository;
import com.smartlearn.repository.ProfilRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategorieRepository categorieRepository;
    private final FormationRepository formationRepository;
    private final ChapitreRepository chapitreRepository;
    private final ApprenantRepository apprenantRepository;
    private final ProfilRepository profilRepository;
    private final CompetenceRepository competenceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CategorieRepository categorieRepository,
                           FormationRepository formationRepository,
                           ChapitreRepository chapitreRepository,
                           ApprenantRepository apprenantRepository,
                           ProfilRepository profilRepository,
                           CompetenceRepository competenceRepository,
                           PasswordEncoder passwordEncoder) {
        this.categorieRepository = categorieRepository;
        this.formationRepository = formationRepository;
        this.chapitreRepository = chapitreRepository;
        this.apprenantRepository = apprenantRepository;
        this.profilRepository = profilRepository;
        this.competenceRepository = competenceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (categorieRepository.count() > 0) {
            return;
        }

        // 1. Catégories
        Categorie catIt = new Categorie();
        catIt.setNom("IT");
        catIt.setDescription("Formations en informatique et développement logiciel");
        catIt = categorieRepository.save(catIt);

        Categorie catMgmt = new Categorie();
        catMgmt.setNom("Management");
        catMgmt.setDescription("Formations en gestion d'équipe et leadership");
        catMgmt = categorieRepository.save(catMgmt);

        Categorie catLangues = new Categorie();
        catLangues.setNom("Langues");
        catLangues.setDescription("Formations en langues étrangères et communication");
        catLangues = categorieRepository.save(catLangues);

        Categorie catDevops = new Categorie();
        catDevops.setNom("Cloud / DevOps");
        catDevops.setDescription("Formations en infrastructure, Docker et Cloud");
        catDevops = categorieRepository.save(catDevops);

        // 2. Formations
        Formation f1 = new Formation();
        f1.setTitre("Java pour débutants");
        f1.setDescription("Introduction à Java et à la programmation orientée objet.");
        f1.setPrix(new BigDecimal("150.00"));
        f1.setNiveau("Débutant");
        f1.setMotsCles("java programmation backend poo");
        f1.setCategorie(catIt);
        f1 = formationRepository.save(f1);

        Formation f2 = new Formation();
        f2.setTitre("Spring Boot Avancé");
        f2.setDescription("Développement d'API REST professionnelles avec Java et Spring Boot.");
        f2.setPrix(new BigDecimal("299.00"));
        f2.setNiveau("Avancé");
        f2.setMotsCles("java spring springboot backend rest api");
        f2.setCategorie(catIt);
        f2 = formationRepository.save(f2);

        Formation f3 = new Formation();
        f3.setTitre("Angular Fundamentals");
        f3.setDescription("Développement d'interfaces web modernes avec Angular.");
        f3.setPrix(new BigDecimal("220.00"));
        f3.setNiveau("Intermédiaire");
        f3.setMotsCles("angular typescript frontend web");
        f3.setCategorie(catIt);
        f3 = formationRepository.save(f3);

        Formation f4 = new Formation();
        f4.setTitre("DevOps Fundamentals");
        f4.setDescription("Introduction à Docker, CI/CD et aux pratiques DevOps.");
        f4.setPrix(new BigDecimal("250.00"));
        f4.setNiveau("Intermédiaire");
        f4.setMotsCles("docker devops cicd cloud");
        f4.setCategorie(catDevops);
        f4 = formationRepository.save(f4);

        Formation f5 = new Formation();
        f5.setTitre("Management d'équipe");
        f5.setDescription("Leadership, communication et gestion des équipes agiles.");
        f5.setPrix(new BigDecimal("180.00"));
        f5.setNiveau("Débutant");
        f5.setMotsCles("management communication leadership");
        f5.setCategorie(catMgmt);
        f5 = formationRepository.save(f5);

        // 3. Chapitres
        Chapitre c1 = new Chapitre();
        c1.setTitre("Syntaxe et Types de base");
        c1.setContenu("Variables, opérateurs, structures conditionnelles et boucles en Java.");
        c1.setOrdre(1);
        c1.setFormation(f1);
        chapitreRepository.save(c1);

        Chapitre c2 = new Chapitre();
        c2.setTitre("Programmation Orientée Objet");
        c2.setContenu("Classes, objets, encapsulation, héritage et polymorphisme.");
        c2.setOrdre(2);
        c2.setFormation(f1);
        chapitreRepository.save(c2);

        Chapitre c3 = new Chapitre();
        c3.setTitre("Architecture Spring Boot & Inversion de Contrôle");
        c3.setContenu("Beans, Injection de dépendances et configuration d'une application Spring Boot.");
        c3.setOrdre(1);
        c3.setFormation(f2);
        chapitreRepository.save(c3);

        Chapitre c4 = new Chapitre();
        c4.setTitre("Services Web REST & Spring Data JPA");
        c4.setContenu("Création des endpoints REST, mapping d'entités et gestion des transactions.");
        c4.setOrdre(2);
        c4.setFormation(f2);
        chapitreRepository.save(c4);

        // 4. Apprenants & Profils & Compétences
        Apprenant ahmed = new Apprenant();
        ahmed.setNom("Ahmed");
        ahmed.setPrenom("Benali");
        ahmed.setEmail("ahmed@example.com");
        ahmed.setPassword(passwordEncoder.encode("password123"));
        ahmed = apprenantRepository.save(ahmed);

        Profil profilAhmed = new Profil();
        profilAhmed.setNiveau("Intermédiaire");
        profilAhmed.setObjectif("Développeur Backend");
        profilAhmed.setDomaineInteret("Développement Web");
        profilAhmed.setApprenant(ahmed);
        profilRepository.save(profilAhmed);

        Competence comp1 = new Competence();
        comp1.setNom("Java");
        comp1.setNiveau(80);
        comp1.setApprenant(ahmed);

        Competence comp2 = new Competence();
        comp2.setNom("Spring");
        comp2.setNiveau(50);
        comp2.setApprenant(ahmed);

        Competence comp3 = new Competence();
        comp3.setNom("SQL");
        comp3.setNiveau(70);
        comp3.setApprenant(ahmed);

        competenceRepository.saveAll(List.of(comp1, comp2, comp3));

        Apprenant sarah = new Apprenant();
        sarah.setNom("Sarah");
        sarah.setPrenom("Martin");
        sarah.setEmail("sarah@example.com");
        sarah.setPassword(passwordEncoder.encode("password123"));
        sarah = apprenantRepository.save(sarah);

        Profil profilSarah = new Profil();
        profilSarah.setNiveau("Débutant");
        profilSarah.setObjectif("Développeur Frontend");
        profilSarah.setDomaineInteret("Design Web & Interfaces UI");
        profilSarah.setApprenant(sarah);
        profilRepository.save(profilSarah);

        Competence compSarah1 = new Competence();
        compSarah1.setNom("Angular");
        compSarah1.setNiveau(60);
        compSarah1.setApprenant(sarah);

        Competence compSarah2 = new Competence();
        compSarah2.setNom("TypeScript");
        compSarah2.setNiveau(55);
        compSarah2.setApprenant(sarah);

        competenceRepository.saveAll(List.of(compSarah1, compSarah2));
    }
}
