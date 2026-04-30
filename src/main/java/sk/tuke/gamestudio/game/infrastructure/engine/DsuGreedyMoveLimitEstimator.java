package sk.tuke.gamestudio.game.infrastructure.engine;

import org.springframework.stereotype.Component;
import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;
import sk.tuke.gamestudio.game.domain.port.Ports;

import java.util.*;
import java.util.stream.IntStream;

@Component
public class DsuGreedyMoveLimitEstimator implements Ports.MoveLimitEstimator {

    private static final int[][] DIRS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
    private static final double SAFETY_MARGIN_RATIO = 0.20d;

    @Override
    public int estimateMoveLimit(Board board) {
        int greedyMoves = estimateGreedyMoves(board);
        return greedyMoves + safetyMargin(greedyMoves);
    }

    int estimateGreedyMoves(Board board) {
        int size = board.size();
        var grid = board.grid();
        var dsu = new Dsu(size * size);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                for (var d : new int[][]{{0, 1}, {1, 0}}) {
                    int nr = r + d[0];
                    int nc = c + d[1];
                    if (nr < size && nc < size && grid[r][c] == grid[nr][nc]) {
                        dsu.union(r * size + c, nr * size + nc);
                    }
                }
            }
        }

        var regionColor = new HashMap<Integer, Color>();
        var adjacency = new HashMap<Integer, Set<Integer>>();

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int root = dsu.find(r * size + c);
                regionColor.put(root, grid[r][c]);
                adjacency.computeIfAbsent(root, ignored -> new HashSet<>());
                for (var d : DIRS) {
                    int nr = r + d[0];
                    int nc = c + d[1];
                    if (nr >= 0 && nr < size && nc >= 0 && nc < size) {
                        int neighborRoot = dsu.find(nr * size + nc);
                        if (neighborRoot != root) {
                            adjacency.get(root).add(neighborRoot);
                        }
                    }
                }
            }
        }

        var blob = new HashSet<Integer>();
        int start = dsu.find(0);
        blob.add(start);
        var frontier = new HashSet<>(adjacency.getOrDefault(start, Set.of()));
        int moves = 0;

        while (!frontier.isEmpty()) {
            var gainByColor = new EnumMap<Color, Integer>(Color.class);
            var regionsByColor = new EnumMap<Color, List<Integer>>(Color.class);

            for (int region : frontier) {
                var color = regionColor.get(region);
                gainByColor.merge(color, dsu.size(region), Integer::sum);
                regionsByColor.computeIfAbsent(color, ignored -> new ArrayList<>()).add(region);
            }

            var bestColor = gainByColor.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElseThrow();

            var absorbed = regionsByColor.get(bestColor);
            blob.addAll(absorbed);
            frontier.removeAll(absorbed);

            for (int region : absorbed) {
                for (int neighbor : adjacency.getOrDefault(region, Set.of())) {
                    if (!blob.contains(neighbor)) {
                        frontier.add(neighbor);
                    }
                }
            }

            moves++;
        }

        return moves;
    }

    private static int safetyMargin(int greedyMoves) {
        if (greedyMoves <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(greedyMoves * SAFETY_MARGIN_RATIO));
    }

    private static final class Dsu {
        private final int[] parent;
        private final int[] sz;

        Dsu(int n) {
            parent = IntStream.range(0, n).toArray();
            sz = new int[n];
            Arrays.fill(sz, 1);
        }

        int find(int x) {
            while (parent[x] != x) {
                parent[x] = parent[parent[x]];
                x = parent[x];
            }
            return x;
        }

        void union(int a, int b) {
            int ra = find(a);
            int rb = find(b);
            if (ra == rb) {
                return;
            }
            if (sz[ra] < sz[rb]) {
                int t = ra;
                ra = rb;
                rb = t;
            }
            parent[rb] = ra;
            sz[ra] += sz[rb];
        }

        int size(int root) {
            return sz[root];
        }
    }
}
