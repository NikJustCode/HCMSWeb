package ru.mokrischev.vendingsupply.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mokrischev.vendingsupply.model.entity.Product;
import ru.mokrischev.vendingsupply.model.entity.StockMovement;
import ru.mokrischev.vendingsupply.model.entity.User;
import ru.mokrischev.vendingsupply.model.entity.WarehouseItem;
import ru.mokrischev.vendingsupply.model.enums.OperationType;
import ru.mokrischev.vendingsupply.repository.ProductRepository;
import ru.mokrischev.vendingsupply.repository.StockMovementRepository;
import ru.mokrischev.vendingsupply.repository.UserRepository;
import ru.mokrischev.vendingsupply.repository.WarehouseItemRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseItemRepository warehouseItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StockMovementRepository stockMovementRepository;

    public List<WarehouseItem> findByFranchisee(String email) {
        return warehouseItemRepository.findByFranchiseeEmail(email);
    }

    public List<StockMovement> getHistory(String email) {
        return stockMovementRepository.findByFranchiseeEmailOrderByOperationDateDesc(email);
    }

    public List<StockMovement> getMachineHistory(Long machineId) {
        return stockMovementRepository.findByVendingMachineIdOrderByOperationDateDesc(machineId);
    }

    @Transactional
    public void registerOperation(String email, Long productId, BigDecimal amount, OperationType type,
            String description, ru.mokrischev.vendingsupply.model.entity.VendingMachine machine) {
        User franchisee = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        WarehouseItem item = warehouseItemRepository.findByFranchiseeEmailAndProductId(email, productId)
                .orElse(null);

        if (item == null) {
            item = WarehouseItem.builder()
                    .franchisee(franchisee)
                    .product(product)
                    .quantity(BigDecimal.ZERO)
                    .build();
        }

        // Check stock for outcome
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            if (item.getQuantity().add(amount).compareTo(BigDecimal.ZERO) < 0) {
                throw new ru.mokrischev.vendingsupply.exceptions.InsufficientStockException(
                        "Недостаточно товара " + product.getName());
            }
        }

        BigDecimal newQuantity = item.getQuantity().add(amount);
        item.setQuantity(newQuantity);
        warehouseItemRepository.save(item);

        // Log movement
        StockMovement movement = StockMovement.builder()
                .franchisee(franchisee)
                .product(product)
                .amount(amount)
                .type(type)
                .description(description)
                .vendingMachine(machine)
                .operationDate(LocalDateTime.now())
                .build();
        stockMovementRepository.save(movement);
    }

    // Overload for backward compatibility
    @Transactional
    public void registerOperation(String email, Long productId, BigDecimal amount, OperationType type,
            String description) {
        registerOperation(email, productId, amount, type, description, null);
    }

    @Transactional
    public void registerIncome(String email, Map<Long, Integer> quantities) {
        processBatch(email, quantities, OperationType.MANUAL_INCOME, "Ручной приход", 1, null);
    }

    @Transactional
    public void registerOutcome(String email, Map<Long, Integer> quantities) {
        processBatch(email, quantities, OperationType.MANUAL_OUTCOME, "Ручное списание", -1, null);
    }

    @Transactional
    public void registerService(String email, Map<Long, Integer> quantities,
            ru.mokrischev.vendingsupply.model.entity.VendingMachine machine) {
        processBatch(email, quantities, OperationType.MACHINE_SERVICE, "Обслуживание: " + machine.getName(), -1,
                machine);
    }

    // Deprecated but kept for OrderService compatibility (will update OrderService
    // next)
    @Transactional
    public void manualAdjustment(String email, Long productId, BigDecimal delta) {
        String desc = delta.compareTo(BigDecimal.ZERO) > 0 ? "Корректировка (+)" : "Корректировка (-)";
        OperationType type = delta.compareTo(BigDecimal.ZERO) > 0 ? OperationType.MANUAL_INCOME
                : OperationType.MANUAL_OUTCOME;
        registerOperation(email, productId, delta, type, desc);
    }

    private void processBatch(String email, Map<Long, Integer> quantities, OperationType type, String descriptionPrefix,
            int multiplier, ru.mokrischev.vendingsupply.model.entity.VendingMachine machine) {
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Integer qty = entry.getValue();
            if (qty == null || qty <= 0)
                continue;

            BigDecimal amount = BigDecimal.valueOf(qty).multiply(BigDecimal.valueOf(multiplier));
            registerOperation(email, entry.getKey(), amount, type, descriptionPrefix, machine);
        }
    }

    public Map<String, Integer> getMachineUsage(String email) {
        // Group amounts of MACHINE_SERVICE operations by machine name
        // Ideally use JPQL SUM. In memory for now.
        List<StockMovement> movements = stockMovementRepository.findByFranchiseeEmailOrderByOperationDateDesc(email);
        Map<String, Integer> usage = new java.util.HashMap<>();

        for (StockMovement sm : movements) {
            if (sm.getType() == OperationType.MACHINE_SERVICE && sm.getVendingMachine() != null) {
                // sm.getAmount() is negative for outcome. Abs or negate it.
                // Assuming amount is negative in DB for service?
                // Logic in registerOperation: item.quantity.add(amount). if type is service,
                // amount is -qty.
                // So getAmount is negative.
                int val = sm.getAmount().abs().intValue();
                usage.merge(sm.getVendingMachine().getName(), val, Integer::sum);
            }
        }
        return usage;
    }

    public Map<String, Map<String, java.math.BigDecimal>> getConsumptionByPeriod(String email, LocalDateTime start,
            LocalDateTime end) {
        List<StockMovement> movements = stockMovementRepository.findByFranchiseeEmailOrderByOperationDateDesc(email);

        // Product Name -> Machine Name -> Amount
        Map<String, Map<String, java.math.BigDecimal>> result = new java.util.HashMap<>();

        for (StockMovement sm : movements) {
            if (sm.getOperationDate().isAfter(start) && sm.getOperationDate().isBefore(end)) {
                if (sm.getType() == OperationType.MACHINE_SERVICE && sm.getVendingMachine() != null) {
                    String product = sm.getProduct().getName();
                    String machine = sm.getVendingMachine().getName();
                    BigDecimal amount = sm.getAmount().abs();

                    result.computeIfAbsent(product, k -> new java.util.HashMap<>())
                            .merge(machine, amount, BigDecimal::add);
                }
            }
        }
        return result;
    }
}
