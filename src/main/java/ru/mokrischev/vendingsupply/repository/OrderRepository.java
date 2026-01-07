package ru.mokrischev.vendingsupply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mokrischev.vendingsupply.model.entity.Order;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByFranchiseeEmailOrderByCreatedAtDesc(String email);

    List<Order> findAllByOrderByCreatedAtDesc();
}
