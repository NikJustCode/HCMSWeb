package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.mokrischev.vendingsupply.model.entity.ServiceReport;
import ru.mokrischev.vendingsupply.model.entity.VendingMachine;
import ru.mokrischev.vendingsupply.repository.ServiceReportRepository;
import ru.mokrischev.vendingsupply.services.VendingMachineService;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/franchisee/reports")
@RequiredArgsConstructor
public class FranchiseeServiceReportController {

    private final ServiceReportRepository serviceReportRepository;
    private final VendingMachineService vendingMachineService;

    @GetMapping
    public String list(@org.springframework.web.bind.annotation.RequestParam(value = "machineId", required = false) Long machineId, Model model, Principal principal) {
        String franchiseeEmail = principal.getName();
        List<ServiceReport> reports;
        
        if (machineId != null) {
            reports = serviceReportRepository.findByEmployeeFranchiseeEmailAndMachineIdOrderByServiceDateDesc(franchiseeEmail, machineId);
        } else {
            reports = serviceReportRepository.findByEmployeeFranchiseeEmailOrderByServiceDateDesc(franchiseeEmail);
        }

        List<VendingMachine> machines = vendingMachineService.findAllByFranchisee(franchiseeEmail);
        
        model.addAttribute("reports", reports);
        model.addAttribute("machines", machines);
        model.addAttribute("selectedMachineId", machineId);
        return "franchisee/reports/list";
    }
    
    @GetMapping("/{id}")
    public String viewDetails(@PathVariable Long id, Model model, Principal principal) {
        ServiceReport report = serviceReportRepository.findById(id).orElse(null);
        if (report == null || !report.getEmployee().getFranchisee().getEmail().equals(principal.getName())) {
            return "redirect:/franchisee/reports";
        }
        model.addAttribute("report", report);
        return "franchisee/reports/details";
    }
}
