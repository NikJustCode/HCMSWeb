package ru.mokrischev.vendingsupply.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.mokrischev.vendingsupply.model.entity.User;
import ru.mokrischev.vendingsupply.model.enums.Role;
import ru.mokrischev.vendingsupply.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setEmail("admin@admin.ru");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setFullName("Главный Владелец");
            admin.setRole(Role.OWNER);

            userRepository.save(admin);
            System.out.println("Default admin created: email=admin@admin.ru, password=admin");
        }
    }
}
