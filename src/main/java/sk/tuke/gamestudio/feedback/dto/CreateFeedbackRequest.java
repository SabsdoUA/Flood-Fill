package sk.tuke.gamestudio.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFeedbackRequest(
        @NotNull(message = "Hodnotenie je povinné")
        @Min(value = 1, message = "Hodnotenie musí byť v rozsahu od 1 do 5")
        @Max(value = 5, message = "Hodnotenie musí byť v rozsahu od 1 do 5")
        Integer rating,

        @Size(max = 150, message = "Komentár môže mať najviac 150 znakov")
        String comment
) {}
