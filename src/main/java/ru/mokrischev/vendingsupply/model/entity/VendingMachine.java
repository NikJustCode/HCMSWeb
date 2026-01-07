package ru.mokrischev.vendingsupply.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vending_machines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendingMachine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotBlank(message = "Название автомата обязательно")
    private String name;

    @Column(name = "address_text")
    @jakarta.validation.constraints.NotBlank(message = "Адрес обязателен")
    private String addressText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "franchisee_id", nullable = false)
    private User franchisee;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;

    // Need to exclude from toString/Equals to avoid circular dependencies with
    // employees if bidirectional
    // But Employee has List<VendingMachine>, Machine usually doesn't own the
    // relationship?
    // Wait, let's see Employee entity first to be sure about mappedBy.
    // User snippet showed: "machine.getEmployees().clear()". So Machine probably
    // has List<Employee>.
}
