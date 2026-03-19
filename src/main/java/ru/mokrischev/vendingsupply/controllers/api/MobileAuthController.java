package ru.mokrischev.vendingsupply.controllers.api;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.mokrischev.vendingsupply.model.entity.Employee;
import ru.mokrischev.vendingsupply.repository.EmployeeRepository;
import ru.mokrischev.vendingsupply.security.JwtUtil;

@RestController
@RequestMapping("/api/mobile/v1")
@RequiredArgsConstructor
public class MobileAuthController {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Employee employee = employeeRepository.findByEmail(loginRequest.getLogin())
                .orElseGet(() -> employeeRepository.findByPhone(loginRequest.getLogin()).orElse(null));

        if (employee == null) {
            return ResponseEntity.status(401).body("Пользователь не найден");
        }

        if (employee.getPassword() == null || !passwordEncoder.matches(loginRequest.getPassword(), employee.getPassword())) {
            return ResponseEntity.status(401).body("Неверный пароль");
        }

        String token = jwtUtil.generateToken(employee);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @Data
    public static class LoginRequest {
        private String login; // email or phone
        private String password;
    }

    @Data
    public static class JwtResponse {
        private final String token;
    }
}
