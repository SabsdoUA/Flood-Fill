package sk.tuke.gamestudio.game.domain.service;

import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;

public final class WinChecker {

    private WinChecker() {}

    public static boolean isWon(Board board) {
        Color origin = board.at(0, 0);
        int size = board.size();
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board.at(r, c) != origin) return false;
        return true;
    }
}