package sk.tuke.gamestudio.leaderboard.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sk.tuke.gamestudio.leaderboard.model.UserStats.BoardSize;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "recorded_wins",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recorded_wins_user_game",
                columnNames = {"user_id", "game_id"}
        )
)
public class RecordedWin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "game_id", nullable = false, updatable = false)
    private String gameId;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_size", nullable = false, updatable = false, length = 16)
    private BoardSize boardSize;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    public RecordedWin(UUID userId, String gameId, BoardSize boardSize) {
        this.userId = userId;
        this.gameId = gameId;
        this.boardSize = boardSize;
        this.recordedAt = Instant.now();
    }
}
