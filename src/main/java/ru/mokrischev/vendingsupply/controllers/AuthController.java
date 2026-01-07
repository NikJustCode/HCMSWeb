package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import ru.mokrischev.vendingsupply.dto.RegistrationDTO;
import ru.mokrischev.vendingsupply.services.UserService;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registration")
    public String registration(Model model) {
        model.addAttribute("registrationDTO", new RegistrationDTO());
        return "registration";
    }

    @PostMapping("/registration")
    public String registerUser(@jakarta.validation.Valid RegistrationDTO registrationDTO,
            org.springframework.validation.BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "registration";
        }
        try {
            userService.registerUser(registrationDTO);
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "registration";
        }
    }
}