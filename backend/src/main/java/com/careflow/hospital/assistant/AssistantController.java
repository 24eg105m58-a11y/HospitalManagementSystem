package com.careflow.hospital.assistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    public record Question(@NotBlank String question) {}
    public record Answer(String answer, List<String> sources, boolean grounded) {}

    @PostMapping("/ask")
    public Answer ask(@Valid @RequestBody Question request) {
        // Deliberately do not invent clinical or policy answers. Replace this adapter with a
        // retrieval service connected to an approved, curated hospital knowledge base.
        return new Answer("The hospital knowledge base is not connected yet. Please contact the care team for current hospital information.", List.of(), false);
    }
}
