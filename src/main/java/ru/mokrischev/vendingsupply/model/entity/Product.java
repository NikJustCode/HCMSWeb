package ru.mokrischev.vendingsupply.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotBlank(message = "Название товара обязательно")
    private String name;

    @Column(columnDefinition = "TEXT")
    @jakarta.validation.constraints.Size(max = 1000, message = "Описание слишком длинное")
    private String description;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotNull(message = "Цена обязательна")
    @jakarta.validation.constraints.PositiveOrZero(message = "Цена не может быть отрицательной")
    private BigDecimal price;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotBlank(message = "Единица измерения обязательна")
    private String unit;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;
}
