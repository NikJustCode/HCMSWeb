package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.mokrischev.vendingsupply.model.entity.Feedback;
import ru.mokrischev.vendingsupply.repository.FeedbackRepository;

import java.util.List;

@Controller
@RequestMapping("/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackRepository feedbackRepository;

    @GetMapping
    public String listFeedback(Model model) {
        List<Feedback> feedbacks = feedbackRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("feedbacks", feedbacks);
        return "admin/feedback/list";
    }

    @PostMapping("/delete/{id}")
    public String deleteFeedback(@PathVariable Long id) {
        feedbackRepository.deleteById(id);
        return "redirect:/admin/feedback";
    }

    @PostMapping("/mark-read/{id}")
    public String markAsRead(@PathVariable Long id) {
        Feedback feedback = feedbackRepository.findById(id).orElseThrow();
        feedback.setRead(true);
        feedbackRepository.save(feedback);
        return "redirect:/admin/feedback";
    }
}
