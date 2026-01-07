package ru.mokrischev.vendingsupply.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String imageUrl;

    private List<CategoryDTO> categories;
}