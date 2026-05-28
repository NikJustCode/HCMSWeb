package ru.mokrischev.vendingsupply.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "machine_reviews")
@Getter
@Setter
public class MachineReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private VendingMachine machine;

    private String customerName;
    private String contactNumber;

    @Column(columnDefinition = "TEXT")
    private String reviewText;

    private LocalDateTime createdAt = LocalDateTime.now();
}
