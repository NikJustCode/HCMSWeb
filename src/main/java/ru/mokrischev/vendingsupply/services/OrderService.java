package ru.mokrischev.vendingsupply.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mokrischev.vendingsupply.model.entity.*;
import ru.mokrischev.vendingsupply.model.enums.OrderStatus;
import ru.mokrischev.vendingsupply.repository.OrderItemRepository;
import ru.mokrischev.vendingsupply.repository.OrderRepository;
import ru.mokrischev.vendingsupply.repository.OrderStatusLogRepository;
import ru.mokrischev.vendingsupply.repository.ProductRepository;
import ru.mokrischev.vendingsupply.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WarehouseService warehouseService;

    private final OrderStatusLogRepository orderStatusLogRepository;

    public List<Order> findByFranchisee(String email) {
        return orderRepository.findByFranchiseeEmailOrderByCreatedAtDesc(email);
    }

    public List<Order> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Map<String, BigDecimal> getMonthlyRevenue() {
        List<Order> orders = orderRepository.findAll();
        Map<String, BigDecimal> revenue = new java.util.TreeMap<>();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");

        for (Order o : orders) {
            if (o.getStatus() == OrderStatus.DELIVERED && o.getCreatedAt() != null) {
                String k = o.getCreatedAt().format(fmt);
                revenue.merge(k, o.getTotalPrice(), BigDecimal::add);
            }
        }
        return revenue;
    }

    public Map<String, Integer> getTopProducts() {
        List<Order> orders = orderRepository.findAll();
        Map<String, Integer> counts = new java.util.HashMap<>();

        for (Order o : orders) {
            if (o.getStatus() == OrderStatus.DELIVERED) {
                List<OrderItem> items = orderItemRepository.findByOrderId(o.getId());
                for (OrderItem i : items) {
                    counts.merge(i.getProduct().getName(), i.getQuantity().intValue(), Integer::sum);
                }
            }
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new));
    }

    public Order findById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    public List<OrderStatusLog> getHistory(Long orderId) {
        return orderStatusLogRepository.findByOrderIdOrderByChangedAtDesc(orderId);
    }

    public List<OrderItem> getItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    @Transactional
    public void createOrder(String franchiseeEmail, Map<Long, Integer> quantities) {
        if (quantities == null || quantities.isEmpty()) {
            throw new RuntimeException("Order is empty");
        }

        User franchisee = userRepository.findByEmail(franchiseeEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + franchiseeEmail));

        Order order = Order.builder()
                .franchisee(franchisee)
                .status(OrderStatus.NEW)
                .totalPrice(BigDecimal.ZERO)
                .build();

        order = orderRepository.save(order);
        logStatusChange(order, OrderStatus.NEW, franchisee.getEmail());

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            Integer qty = entry.getValue();

            if (qty <= 0)
                continue;

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            BigDecimal quantity = BigDecimal.valueOf(qty);
            BigDecimal lineTotal = product.getPrice().multiply(quantity);
            total = total.add(lineTotal);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .priceAtMoment(product.getPrice())
                    .build();

            orderItemRepository.save(item);
        }

        order.setTotalPrice(total);
        orderRepository.save(order);
    }

    @Transactional
    public void updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderStatus oldStatus = order.getStatus();
        if (oldStatus == newStatus)
            return;

        order.setStatus(newStatus);
        orderRepository.save(order);

        logStatusChange(order, newStatus, "Admin");

        if (newStatus == OrderStatus.DELIVERED && oldStatus != OrderStatus.DELIVERED) {
            List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
            for (OrderItem item : items) {
                warehouseService.registerOperation(
                        order.getFranchisee().getEmail(),
                        item.getProduct().getId(),
                        item.getQuantity(),
                        ru.mokrischev.vendingsupply.model.enums.OperationType.INCOMING_ORDER,
                        "Заказ #" + orderId);
            }
        }
    }

    private void logStatusChange(Order order, OrderStatus status, String changedBy) {
        OrderStatusLog log = OrderStatusLog.builder()
                .order(order)
                .status(status)
                .changedBy(changedBy)
                .changedAt(java.time.LocalDateTime.now())
                .build();
        orderStatusLogRepository.save(log);
    }

    public BigDecimal getTotalExpenses(String email) {
        List<Order> orders = orderRepository.findByFranchiseeEmailOrderByCreatedAtDesc(email);
        return orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getExpensesByPeriod(String email, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        List<Order> orders = orderRepository.findByFranchiseeEmailOrderByCreatedAtDesc(email);
        return orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(start)
                        && o.getCreatedAt().isBefore(end))
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
