package ru.mokrischev.vendingsupply.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDTO {

    @jakarta.validation.constraints.NotBlank(message = "Email обязателен")
    @jakarta.validation.constraints.Email(message = "Некорректный формат email")
    private String email;

    @jakarta.validation.constraints.NotBlank(message = "Пароль обязателен")
    @jakarta.validation.constraints.Size(min = 6, message = "Пароль должен быть не менее 6 символов")
    private String password;

    @jakarta.validation.constraints.NotBlank(message = "Подтверждение пароля обязательно")
    private String confirmPassword;

    @jakarta.validation.constraints.NotBlank(message = "ФИО обязательно")
    private String fullName;

    @jakarta.validation.constraints.NotBlank(message = "Телефон обязателен")
    private String phoneNumber;
}
