package ru.mokrischev.vendingsupply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mokrischev.vendingsupply.model.entity.WarehouseItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseItemRepository extends JpaRepository<WarehouseItem, Long> {
    List<WarehouseItem> findByFranchiseeEmail(String email);

    Optional<WarehouseItem> findByFranchiseeEmailAndProductId(String email, Long productId);
}
