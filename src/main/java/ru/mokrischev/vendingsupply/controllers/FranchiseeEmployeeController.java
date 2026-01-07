package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.mokrischev.vendingsupply.model.entity.Employee;
import ru.mokrischev.vendingsupply.model.enums.ScheduleType;
import ru.mokrischev.vendingsupply.services.EmployeeService;
import ru.mokrischev.vendingsupply.services.VendingMachineService;

import jakarta.validation.Valid;
import java.security.Principal;
import java.time.DayOfWeek;
import java.util.List;

@Controller
@RequestMapping("/franchisee/employees")
@RequiredArgsConstructor
public class FranchiseeEmployeeController {

    private final EmployeeService employeeService;
    private final VendingMachineService vendingMachineService;

    @GetMapping
    public String list(Model model, Principal principal) {
        model.addAttribute("employees", employeeService.findByFranchisee(principal.getName()));
        return "franchisee/employees/list";
    }

    @GetMapping("/new")
    public String newEmployee(Model model, Principal principal) {
        model.addAttribute("employee", new Employee());
        addCommonAttributes(model, principal);
        return "franchisee/employees/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Employee employee,
            BindingResult bindingResult,
            @RequestParam(required = false) List<Long> machineIds,
            Principal principal, Model model) {

        if (bindingResult.hasErrors()) {
            addCommonAttributes(model, principal);
            return "franchisee/employees/form";
        }

        // Logic based on schedule type
        if (employee.getScheduleType() == ScheduleType.WEEKLY_DAYS) {
            employee.setShiftPattern(null);
            if (employee.getWorkingDays() == null || employee.getWorkingDays().isEmpty()) {
                bindingResult.rejectValue("workingDays", "error.employee", "Выберите хотя бы один рабочий день");
                addCommonAttributes(model, principal);
                return "franchisee/employees/form";
            }
        } else if (employee.getScheduleType() == ScheduleType.SHIFT_PATTERN) {
            employee.setWorkingDays(null);
            if (employee.getShiftPattern() == null || employee.getShiftPattern().trim().isEmpty()) {
                bindingResult.rejectValue("shiftPattern", "error.employee", "Укажите схему смен (например, 2/2)");
                addCommonAttributes(model, principal);
                return "franchisee/employees/form";
            }
        }

        employeeService.save(employee, machineIds, principal.getName());
        return "redirect:/franchisee/employees";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model, Principal principal) {
        Employee employee = employeeService.findByIdAndFranchisee(id, principal.getName());
        if (employee == null) {
            return "redirect:/franchisee/employees";
        }
        model.addAttribute("employee", employee);
        addCommonAttributes(model, principal);
        return "franchisee/employees/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Principal principal) {
        employeeService.delete(id, principal.getName());
        return "redirect:/franchisee/employees";
    }

    private void addCommonAttributes(Model model, Principal principal) {
        model.addAttribute("scheduleTypes", ScheduleType.values());
        model.addAttribute("machines", vendingMachineService.findAllByFranchisee(principal.getName()));

        // Russian days mapping for the form
        model.addAttribute("daysList", List.of(
                new DayWrapper(DayOfWeek.MONDAY, "Пн"),
                new DayWrapper(DayOfWeek.TUESDAY, "Вт"),
                new DayWrapper(DayOfWeek.WEDNESDAY, "Ср"),
                new DayWrapper(DayOfWeek.THURSDAY, "Чт"),
                new DayWrapper(DayOfWeek.FRIDAY, "Пт"),
                new DayWrapper(DayOfWeek.SATURDAY, "Сб"),
                new DayWrapper(DayOfWeek.SUNDAY, "Вс")));
    }

    @Value
    public static class DayWrapper {
        DayOfWeek day;
        String label;
    }
}
