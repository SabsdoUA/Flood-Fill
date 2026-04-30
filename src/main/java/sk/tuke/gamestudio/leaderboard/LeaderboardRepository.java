package sk.tuke.gamestudio.leaderboard;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import sk.tuke.gamestudio.leaderboard.model.UserStats;

import java.util.List;
import java.util.UUID;

public interface LeaderboardRepository extends JpaRepository<UserStats, UUID> {

    // ── Projekcia pre výsledok leaderboardu ───────────────────────────────────
    interface Projection {
        String getName();
        int getSmallWins();
        int getMediumWins();
        int getLargeWins();
        int getTotalPoints();
    }

    // ── Dopyty ────────────────────────────────────────────────────────────────
    @Query(value = """
        SELECT COALESCE(NULLIF(TRIM(u.nickname), ''), NULLIF(TRIM(u.name), ''), u.email) AS name,
               COALESCE(s.small_wins, 0)                                                  AS small_wins,
               COALESCE(s.medium_wins, 0)                                                 AS medium_wins,
               COALESCE(s.large_wins, 0)                                                  AS large_wins,
               COALESCE(s.small_wins, 0)
                   + 2 * COALESCE(s.medium_wins, 0)
                   + 3 * COALESCE(s.large_wins, 0)                                        AS total_points
        FROM users u
        LEFT JOIN user_stats s ON s.user_id = u.id
        ORDER BY total_points DESC
        """, nativeQuery = true)
    List<Projection> findLeaderboard(Pageable pageable);

    // ── Doplnenie chýbajúcich štatistík ──────────────────────────────────────
    @Modifying
    @Query(value = """
        INSERT INTO user_stats (user_id, version, small_wins, medium_wins, large_wins)
        SELECT u.id, 0, 0, 0, 0
        FROM users u
        WHERE NOT EXISTS (
            SELECT 1
            FROM user_stats s
            WHERE s.user_id = u.id
        )
        """, nativeQuery = true)
    int insertMissingStats();
}
