package com.smartlearn.config;

import com.smartlearn.entity.Apprenant;
import com.smartlearn.entity.Role;
import com.smartlearn.repository.ApprenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {
    private final ApprenantRepository users;
    private final PasswordEncoder encoder;
    @Value("${smartlearn.admin.email:admin@smartlearn.local}") private String email;
    @Value("${smartlearn.admin.password:}") private String password;
    public AdminInitializer(ApprenantRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }
    @Override
    public void run(String... args) {
        if (users.findAll().stream().anyMatch(a -> a.getRole() == Role.ADMIN)) return;
        if (users.existsByEmailIgnoreCase(email))
            throw new IllegalStateException("Admin email is already used by a learner; configure SMARTLEARN_ADMIN_EMAIL");
        boolean generated = password.isBlank();
        String initialPassword = generated ? java.util.UUID.randomUUID().toString() : password;
        Apprenant admin = new Apprenant();
        admin.setNom("Admin"); admin.setPrenom("SmartLearn"); admin.setEmail(email);
        admin.setPassword(encoder.encode(initialPassword)); admin.setRole(Role.ADMIN);
        users.save(admin);
        if (generated) org.slf4j.LoggerFactory.getLogger(AdminInitializer.class)
            .warn("Initial administrator account: {} / {} (save this generated password)", email, initialPassword);
    }
}
