package sk.tuke.gamestudio.game.infrastructure.engine;

import org.springframework.stereotype.Component;
import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;
import sk.tuke.gamestudio.game.domain.model.GameDomainException;
import sk.tuke.gamestudio.game.domain.port.Ports;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomBoardGenerator implements Ports.BoardGenerator {

    private static final Set<Integer> VALID_SIZES = Set.of(12, 15, 18);
    private static final int[][] DIRS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
    private static final Color[] COLORS = Color.values();

    @Override
    public Board generate(int size) {
        if (!VALID_SIZES.contains(size)) throw new GameDomainException.InvalidSize(size);

        var rnd = ThreadLocalRandom.current();
        var grid = new Color[size][size];
        var vis = new boolean[size][size];
        var counts = new int[COLORS.length];

        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (!vis[r][c]) placeBlock(grid, vis, counts, r, c, size, rnd);

        return new Board(grid, size);
    }

    private static void placeBlock(Color[][] grid, boolean[][] vis, int[] counts,
                                   int r0, int c0, int size, Random rnd) {
        int target = blockSize(rnd);
        var front = new ArrayList<int[]>();
        var block = new ArrayList<int[]>(target);
        front.add(new int[]{r0, c0});

        while (block.size() < target && !front.isEmpty()) {
            var cell = front.remove(rnd.nextInt(front.size()));
            if (vis[cell[0]][cell[1]]) continue;
            vis[cell[0]][cell[1]] = true;
            block.add(cell);
            for (var d : DIRS) {
                int nr = cell[0] + d[0], nc = cell[1] + d[1];
                if (nr >= 0 && nr < size && nc >= 0 && nc < size && !vis[nr][nc])
                    front.add(new int[]{nr, nc});
            }
        }

        int forbidden = adjacentMask(grid, block, size);
        int chosen = leastUsedAllowed(counts, forbidden, rnd);

        for (var cell : block) {
            grid[cell[0]][cell[1]] = COLORS[chosen];
            counts[chosen]++;
        }
    }

    private static int blockSize(Random rnd) {
        int r = rnd.nextInt(100);
        return r < 25 ? 1 : r < 50 ? 2 : r < 70 ? 3 : r < 85 ? 4 : 5;
    }

    private static int adjacentMask(Color[][] grid, ArrayList<int[]> block, int size) {
        int mask = 0;
        for (var cell : block)
            for (var d : DIRS) {
                int nr = cell[0] + d[0], nc = cell[1] + d[1];
                if (nr >= 0 && nr < size && nc >= 0 && nc < size && grid[nr][nc] != null)
                    mask |= (1 << grid[nr][nc].ordinal());
            }
        return mask;
    }

    private static int leastUsedAllowed(int[] counts, int forbidden, Random rnd) {
        var idxs = new int[]{0, 1, 2, 3, 4, 5};
        for (int i = 5; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = idxs[i];
            idxs[i] = idxs[j];
            idxs[j] = t;
        }

        int best = idxs[0], min = Integer.MAX_VALUE;
        for (int i : idxs) {
            if ((forbidden & (1 << i)) == 0 && counts[i] < min) {
                min = counts[i];
                best = i;
            }
        }
        return best;
    }
}