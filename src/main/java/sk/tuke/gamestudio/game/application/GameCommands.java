package sk.tuke.gamestudio.game.application;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class GameCommands {
    private GameCommands() {}
    public record StartGame(String gameId, @Min(1) int size) {}
    public record ResumeGame(String gameId, @Min(1) int size) {}
    public record MakeMove(String gameId, @NotBlank String color) {}
}