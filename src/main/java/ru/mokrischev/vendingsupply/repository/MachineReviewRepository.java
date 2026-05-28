package ru.mokrischev.vendingsupply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mokrischev.vendingsupply.model.entity.MachineReview;
import java.util.List;

public interface MachineReviewRepository extends JpaRepository<MachineReview, Long> {
    List<MachineReview> findByMachineIdOrderByCreatedAtDesc(Long machineId);
}
