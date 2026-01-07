package ru.mokrischev.vendingsupply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mokrischev.vendingsupply.model.entity.VendingMachine;

import java.util.List;

@Repository
public interface VendingMachineRepository extends JpaRepository<VendingMachine, Long> {
    List<VendingMachine> findByFranchiseeEmail(String email);

    List<VendingMachine> findByFranchiseeEmailAndActiveTrue(String email);
}
