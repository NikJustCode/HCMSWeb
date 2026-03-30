package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.mokrischev.vendingsupply.services.ProductService;
import ru.mokrischev.vendingsupply.services.WarehouseService;

import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/franchisee/warehouse")
@RequiredArgsConstructor
public class FranchiseeWarehouseController {

    private final WarehouseService warehouseService;
    private final ProductService productService;
    private final ru.mokrischev.vendingsupply.repository.VendingMachineRepository vendingMachineRepository;

    @GetMapping
    public String viewWarehouse(Principal principal, Model model) {
        model.addAttribute("items", warehouseService.findByFranchisee(principal.getName()));
        return "franchisee/warehouse";
    }

    @GetMapping("/history")
    public String viewHistory(
            Principal principal,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @RequestParam(required = false) String type,
            Model model) {
        
        java.util.List<ru.mokrischev.vendingsupply.model.entity.StockMovement> movements = warehouseService.getHistory(principal.getName());
        
        if (machineId != null) {
            movements = movements.stream()
                .filter(m -> m.getVendingMachine() != null && m.getVendingMachine().getId().equals(machineId))
                .collect(Collectors.toList());
        }
        if (date != null) {
            movements = movements.stream()
                .filter(m -> m.getOperationDate().toLocalDate().equals(date))
                .collect(Collectors.toList());
        }
        if (type != null && !type.isEmpty()) {
            movements = movements.stream()
                .filter(m -> m.getType().name().equals(type))
                .collect(Collectors.toList());
        }

        model.addAttribute("movements", movements);
        model.addAttribute("machines", vendingMachineRepository.findByFranchiseeEmail(principal.getName()));
        model.addAttribute("operations", ru.mokrischev.vendingsupply.model.enums.OperationType.values());
        model.addAttribute("selectedMachineId", machineId);
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedType", type);
        
        return "franchisee/warehouse/history";
    }

    @GetMapping("/income")
    public String viewIncome(Model model) {
        model.addAttribute("products", productService.findAll());
        return "franchisee/warehouse/income";
    }

    @PostMapping("/income")
    public String processIncome(Principal principal,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer quantity,
            @RequestParam Map<String, String> allParams) {

        // Handle single item form
        if (productId != null && quantity != null) {
            warehouseService.manualAdjustment(principal.getName(), productId, java.math.BigDecimal.valueOf(quantity));
            return "redirect:/franchisee/warehouse";
        }

        // Handle batch items (if any legacy forms use this)
        Map<Long, Integer> quantities = parseQuantities(allParams);
        if (!quantities.isEmpty()) {
            warehouseService.registerIncome(principal.getName(), quantities);
        }

        return "redirect:/franchisee/warehouse";
    }

    @GetMapping("/outcome")
    public String viewOutcome(Principal principal, Model model) {
        model.addAttribute("warehouseItems", warehouseService.findByFranchisee(principal.getName()).stream()
                .filter(item -> item.getQuantity().compareTo(java.math.BigDecimal.ZERO) > 0)
                .collect(Collectors.toList()));
        return "franchisee/warehouse/outcome";
    }

    @PostMapping("/outcome")
    public String processOutcome(Principal principal,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer quantity,
            @RequestParam Map<String, String> allParams) {

        try {
            // Handle single item form
            if (productId != null && quantity != null) {
                warehouseService.manualAdjustment(principal.getName(), productId,
                        java.math.BigDecimal.valueOf(quantity).negate());
                return "redirect:/franchisee/warehouse";
            }

            // Handle batch
            Map<Long, Integer> quantities = parseQuantities(allParams);
            if (!quantities.isEmpty()) {
                warehouseService.registerOutcome(principal.getName(), quantities);
            }
        } catch (Exception e) {
            return "redirect:/franchisee/warehouse/outcome?error=" + e.getMessage();
        }
        return "redirect:/franchisee/warehouse";
    }

    private Map<Long, Integer> parseQuantities(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("quantity_"))
                .collect(Collectors.toMap(
                        entry -> Long.parseLong(entry.getKey().replace("quantity_", "")),
                        entry -> {
                            try {
                                return Integer.parseInt(entry.getValue());
                            } catch (NumberFormatException e) {
                                return 0;
                            }
                        }));
    }
}
