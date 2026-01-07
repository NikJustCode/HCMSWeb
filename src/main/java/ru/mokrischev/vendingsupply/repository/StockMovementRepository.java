package ru.mokrischev.vendingsupply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mokrischev.vendingsupply.model.entity.StockMovement;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByFranchiseeEmailOrderByOperationDateDesc(String email);

    List<StockMovement> findByVendingMachineIdOrderByOperationDateDesc(Long machineId);
}
