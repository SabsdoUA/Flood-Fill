package sk.tuke.gamestudio.game.domain.model;

import java.util.Arrays;
import java.util.Objects;

public record Board(Color[][] grid, int size) {

    public Board {
        Objects.requireNonNull(grid, "grid must not be null");
        if (size <= 0 || grid.length != size)
            throw new IllegalArgumentException("Invalid board size: " + size);

        var copy = new Color[size][size];
        for (int i = 0; i < size; i++) {
            if (grid[i] == null || grid[i].length != size)
                throw new IllegalArgumentException("Invalid row at index: " + i);
            copy[i] = grid[i].clone();
        }
        grid = copy;
    }

    @Override
    public Color[][] grid() {
        var copy = new Color[size][size];
        for (int i = 0; i < size; i++) copy[i] = grid[i].clone();
        return copy;
    }

    public Color at(int r, int c) {
        return grid[r][c];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Board b)) return false;
        return size == b.size && Arrays.deepEquals(grid, b.grid);
    }

    @Override
    public int hashCode() {
        return 31 * size + Arrays.deepHashCode(grid);
    }

    @Override
    public String toString() {
        return "Board[size=" + size + ", grid=" + Arrays.deepToString(grid) + "]";
    }
}