package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.mokrischev.vendingsupply.services.OrderService;

@Controller
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final OrderService orderService;

    @GetMapping
    public String showStatistics(Model model) {
        model.addAttribute("monthlyRevenue", orderService.getMonthlyRevenue());
        model.addAttribute("topProducts", orderService.getTopProducts());
        return "admin/statistics";
    }
}
