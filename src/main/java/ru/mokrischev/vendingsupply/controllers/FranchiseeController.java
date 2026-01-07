package ru.mokrischev.vendingsupply.controllers;

import org.springframework.stereotype.Controller;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/franchisee")
@RequiredArgsConstructor
public class FranchiseeController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "franchisee/dashboard";
    }
}
