package sk.tuke.gamestudio.game.domain.service;

import sk.tuke.gamestudio.game.domain.model.Color;

import java.util.ArrayDeque;

public final class FloodFill {

    private static final int[][] DIRS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    private FloodFill() {}

    public static Color[][] apply(Color[][] grid, int r, int c, Color newColor) {
        int size = grid.length;
        Color oldColor = grid[r][c];

        if (oldColor == newColor) return grid;

        var result = deepCopy(grid, size);
        var queue = new ArrayDeque<int[]>(size * size / 2);
        queue.add(new int[]{r, c});

        while (!queue.isEmpty()) {
            var cell = queue.poll();
            int cr = cell[0], cc = cell[1];

            if (result[cr][cc] != oldColor) continue;
            result[cr][cc] = newColor;

            for (var d : DIRS) {
                int nr = cr + d[0], nc = cc + d[1];
                if (nr >= 0 && nr < size && nc >= 0 && nc < size
                        && result[nr][nc] == oldColor)
                    queue.add(new int[]{nr, nc});
            }
        }
        return result;
    }

    private static Color[][] deepCopy(Color[][] src, int size) {
        var copy = new Color[size][size];
        for (int i = 0; i < size; i++) copy[i] = src[i].clone();
        return copy;
    }
}