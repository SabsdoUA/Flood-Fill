package sk.tuke.gamestudio.game.application;

public record GameResponse(
        String gameId,
        String[][] grid,
        int movesTaken,
        int moveLimit,
        String status,
        boolean won,
        String error
) { }