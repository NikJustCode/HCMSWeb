package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.mokrischev.vendingsupply.model.entity.User;
import ru.mokrischev.vendingsupply.services.UserService;
import ru.mokrischev.vendingsupply.services.VendingMachineService;
import ru.mokrischev.vendingsupply.services.WarehouseService;

@Controller
@RequestMapping("/admin/franchisee")
@RequiredArgsConstructor
public class AdminFranchiseeController {

    private final UserService userService;
    private final VendingMachineService vendingMachineService;
    private final WarehouseService warehouseService;

    @GetMapping
    public String listFranchisees(Model model) {
        model.addAttribute("franchisees", userService.findAllFranchisees());
        return "admin/franchisee/list";
    }

    @GetMapping("/{id}")
    public String franchiseeDetails(@PathVariable Long id, Model model) {
        User franchisee = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Franchisee not found"));

        model.addAttribute("franchisee", franchisee);
        model.addAttribute("machines", vendingMachineService.findAllByFranchisee(franchisee.getEmail()));
        model.addAttribute("warehouseItems", warehouseService.findByFranchisee(franchisee.getEmail()));

        return "admin/franchisee/details";
    }
}
