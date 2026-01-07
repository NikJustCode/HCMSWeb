package ru.mokrischev.vendingsupply.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.mokrischev.vendingsupply.dto.RegistrationDTO;
import ru.mokrischev.vendingsupply.model.entity.User;
import ru.mokrischev.vendingsupply.model.enums.Role;
import ru.mokrischev.vendingsupply.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void registerUser(RegistrationDTO registrationDTO) {
        if (userRepository.findByEmail(registrationDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        if (!registrationDTO.getPassword().equals(registrationDTO.getConfirmPassword())) {
            throw new RuntimeException("Пароли не совпадают");
        }

        User user = new User();
        user.setEmail(registrationDTO.getEmail());
        user.setFullName(registrationDTO.getFullName());
        user.setPhoneNumber(registrationDTO.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));

        user.setRole(Role.FRANCHISEE);

        userRepository.save(user);
    }

    public java.util.Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public java.util.List<User> findAllFranchisees() {
        return userRepository.findByRole(Role.FRANCHISEE);
    }
}
