package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.mokrischev.vendingsupply.model.entity.MachineReview;
import ru.mokrischev.vendingsupply.model.entity.VendingMachine;
import ru.mokrischev.vendingsupply.repository.MachineReviewRepository;
import ru.mokrischev.vendingsupply.repository.VendingMachineRepository;

@Controller
@RequestMapping("/public/reviews")
@RequiredArgsConstructor
public class PublicReviewController {

    private final VendingMachineRepository vendingMachineRepository;
    private final MachineReviewRepository machineReviewRepository;

    @GetMapping("/machine/{machineId}")
    public String showReviewForm(@PathVariable Long machineId, Model model) {
        VendingMachine machine = vendingMachineRepository.findById(machineId).orElse(null);
        if (machine == null || !machine.isActive()) {
            return "redirect:/";
        }
        model.addAttribute("machine", machine);
        model.addAttribute("review", new MachineReview());
        return "public/review_form";
    }

    @PostMapping("/machine/{machineId}")
    public String submitReview(@PathVariable Long machineId, @ModelAttribute MachineReview review) {
        VendingMachine machine = vendingMachineRepository.findById(machineId).orElse(null);
        if (machine != null && machine.isActive()) {
            review.setId(null);
            review.setMachine(machine);
            machineReviewRepository.save(review);
            return "public/review_success";
        }
        return "redirect:/";
    }
}
