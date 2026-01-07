package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.mokrischev.vendingsupply.model.entity.VendingMachine;
import ru.mokrischev.vendingsupply.services.VendingMachineService;

import java.security.Principal;

@Controller
@RequestMapping("/franchisee/machines")
@RequiredArgsConstructor
public class FranchiseeMachineController {

    private final VendingMachineService vendingMachineService;
    private final ru.mokrischev.vendingsupply.services.WarehouseService warehouseService;

    @GetMapping
    public String list(Model model, Principal principal) {
        model.addAttribute("machines", vendingMachineService.findAllByFranchisee(principal.getName()));
        return "franchisee/machines/list";
    }

    @GetMapping("/new")
    public String newMachine(Model model) {
        model.addAttribute("machine", new VendingMachine());
        return "franchisee/machines/form";
    }

    @PostMapping("/save")
    public String save(@jakarta.validation.Valid @ModelAttribute("machine") VendingMachine machine,
            org.springframework.validation.BindingResult bindingResult,
            Principal principal, Model model) {
        if (bindingResult.hasErrors()) {
            return "franchisee/machines/form";
        }
        vendingMachineService.save(machine, principal.getName());
        return "redirect:/franchisee/machines";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model, Principal principal) {
        VendingMachine machine = vendingMachineService.findByIdAndFranchisee(id, principal.getName());
        if (machine == null) {
            return "redirect:/franchisee/machines";
        }
        model.addAttribute("machine", machine);
        return "franchisee/machines/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Principal principal) {
        vendingMachineService.delete(id, principal.getName());
        return "redirect:/franchisee/machines";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, Principal principal) {
        VendingMachine machine = vendingMachineService.findByIdAndFranchisee(id, principal.getName());
        if (machine == null) {
            return "redirect:/franchisee/machines";
        }
        model.addAttribute("machine", machine);
        model.addAttribute("history", warehouseService.getMachineHistory(id));
        return "franchisee/machines/details";
    }

    @GetMapping("/{id}/service")
    public String serviceForm(@PathVariable Long id, Model model, Principal principal) {
        VendingMachine machine = vendingMachineService.findByIdAndFranchisee(id, principal.getName());
        if (machine == null) {
            return "redirect:/franchisee/machines";
        }
        model.addAttribute("machine", machine);
        model.addAttribute("warehouseItems", warehouseService.findByFranchisee(principal.getName()));
        model.addAttribute("serviceForm", new ru.mokrischev.vendingsupply.dto.BatchServiceForm());
        return "franchisee/machines/service";
    }

    @PostMapping("/{id}/service")
    public String processService(@PathVariable Long id, Principal principal,
            @ModelAttribute ru.mokrischev.vendingsupply.dto.BatchServiceForm form,
            Model model) {

        VendingMachine machine = vendingMachineService.findByIdAndFranchisee(id, principal.getName());
        if (machine == null) {
            return "redirect:/franchisee/machines";
        }

        try {
            java.util.Map<Long, Integer> quantities = new java.util.HashMap<>();
            if (form.getItems() != null) {
                for (ru.mokrischev.vendingsupply.dto.BatchServiceForm.ServiceItem item : form.getItems()) {
                    if (item.getAmount() != null && item.getAmount() > 0) {
                        quantities.put(item.getProductId(), item.getAmount());
                    }
                }
            }

            if (quantities.isEmpty()) {
                model.addAttribute("error_message", "Выберите хотя бы один товар");
                model.addAttribute("machine", machine);
                model.addAttribute("warehouseItems", warehouseService.findByFranchisee(principal.getName()));
                return "franchisee/machines/service";
            }

            warehouseService.registerService(principal.getName(), quantities, machine);
        } catch (ru.mokrischev.vendingsupply.exceptions.InsufficientStockException e) {
            model.addAttribute("error_message", e.getMessage());
            model.addAttribute("machine", machine);
            model.addAttribute("warehouseItems", warehouseService.findByFranchisee(principal.getName()));
            return "franchisee/machines/service";
        } catch (Exception e) {
            model.addAttribute("error_message", "Ошибка: " + e.getMessage());
            model.addAttribute("machine", machine);
            model.addAttribute("warehouseItems", warehouseService.findByFranchisee(principal.getName()));
            return "franchisee/machines/service";
        }

        return "redirect:/franchisee/machines/" + id;
    }
}
