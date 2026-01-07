package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.mokrischev.vendingsupply.services.OrderService;
import ru.mokrischev.vendingsupply.services.WarehouseService;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/franchisee/statistics")
@RequiredArgsConstructor
public class FranchiseeStatisticsController {

    private final OrderService orderService;
    private final WarehouseService warehouseService;

    @GetMapping
    public String showStatistics(Model model, Principal principal,
            @RequestParam(required = false, defaultValue = "month") String periodType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String email = principal.getName();
        LocalDateTime chartStart;
        LocalDateTime chartEnd;
        String periodLabel;

        // Chart Logic
        if ("day".equals(periodType) && date != null) {
            chartStart = date.atStartOfDay();
            chartEnd = date.atTime(LocalTime.MAX);
            periodLabel = "на " + date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            model.addAttribute("selectedDate", date);
        } else if ("month".equals(periodType) && month != null && !month.isEmpty()) {
            try {
                String[] parts = month.split("-");
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                chartStart = LocalDateTime.of(y, m, 1, 0, 0);
                chartEnd = chartStart.plusMonths(1).minusSeconds(1);
                periodLabel = "за "
                        + chartStart.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("ru")));
                model.addAttribute("selectedMonth", month);
            } catch (Exception e) {
                // Fallback to current month if bad format
                chartStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                chartEnd = LocalDateTime.now();
                periodLabel = "за текущий месяц";
            }
        } else {
            // Default: Current month
            chartStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            chartEnd = LocalDateTime.now();
            periodLabel = "за текущий месяц";
            model.addAttribute("selectedMonth", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }

        Map<String, Map<String, BigDecimal>> consumption = warehouseService.getConsumptionByPeriod(email, chartStart,
                chartEnd);
        model.addAttribute("consumptionData", consumption);
        model.addAttribute("periodLabel", periodLabel);
        model.addAttribute("periodType", periodType);

        // Expenses Logic
        BigDecimal expenses = BigDecimal.ZERO;
        if (startDate != null && endDate != null) {
            expenses = orderService.getExpensesByPeriod(email, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
            model.addAttribute("calcStartDate", startDate);
            model.addAttribute("calcEndDate", endDate);
        } else {
            // Default expense period: last 30 days
            model.addAttribute("calcStartDate", LocalDate.now().minusDays(30));
            model.addAttribute("calcEndDate", LocalDate.now());
            expenses = orderService.getExpensesByPeriod(email, LocalDate.now().minusDays(30).atStartOfDay(),
                    LocalDateTime.now());
        }
        model.addAttribute("expenses", expenses);

        return "franchisee/statistics";
    }
}
