package ru.mokrischev.vendingsupply.dto;

import java.util.List;
import lombok.Data;

@Data
public class BatchServiceForm {
    private List<ServiceItem> items;

    @Data
    public static class ServiceItem {
        private Long productId;
        private Integer amount;
    }
}
