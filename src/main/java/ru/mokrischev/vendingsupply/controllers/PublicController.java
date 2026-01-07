package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.mokrischev.vendingsupply.model.entity.Feedback;
import ru.mokrischev.vendingsupply.repository.FeedbackRepository;

@Controller
@RequiredArgsConstructor
public class PublicController {

    private final FeedbackRepository feedbackRepository;

    @GetMapping("/contacts")
    public String contactsPage() {
        return "contacts";
    }

    @PostMapping("/feedback")
    public String submitFeedback(@RequestParam String name,
            @RequestParam String contactInfo,
            @RequestParam String message,
            Model model) {
        Feedback feedback = Feedback.builder()
                .name(name)
                .contactInfo(contactInfo)
                .message(message)
                .build();
        feedbackRepository.save(feedback);
        return "redirect:/contacts?success";
    }
}
