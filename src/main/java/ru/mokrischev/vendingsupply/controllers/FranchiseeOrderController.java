package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.mokrischev.vendingsupply.services.OrderService;
import ru.mokrischev.vendingsupply.services.ProductService;

import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/franchisee/orders")
@RequiredArgsConstructor
public class FranchiseeOrderController {

    private final OrderService orderService;
    private final ProductService productService;

    @GetMapping
    public String listOrders(Model model, Principal principal) {
        model.addAttribute("orders", orderService.findByFranchisee(principal.getName()));
        return "franchisee/orders/list";
    }

    @GetMapping("/{id}")
    public String orderDetails(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.findById(id));
        model.addAttribute("items", orderService.getItems(id));
        model.addAttribute("history", orderService.getHistory(id));
        return "orders/details";
    }

    @GetMapping("/new")
    public String newOrderForm(Model model) {
        model.addAttribute("products", productService.findAllActive());
        return "franchisee/orders/new";
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam Map<String, String> params, Principal principal) {
        Map<Long, Integer> quantities = params.entrySet().stream()
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

        try {
            orderService.createOrder(principal.getName(), quantities);
        } catch (Exception e) {
            return "redirect:/franchisee/orders/new?error=" + e.getMessage();
        }
        return "redirect:/franchisee/orders";
    }
}
