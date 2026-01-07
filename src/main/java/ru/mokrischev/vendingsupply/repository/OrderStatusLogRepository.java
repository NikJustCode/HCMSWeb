package ru.mokrischev.vendingsupply.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mokrischev.vendingsupply.model.entity.OrderStatusLog;

import java.util.List;

public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, Long> {
    List<OrderStatusLog> findByOrderIdOrderByChangedAtDesc(Long orderId);
}
