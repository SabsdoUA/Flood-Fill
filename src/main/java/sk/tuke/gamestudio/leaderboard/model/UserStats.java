package sk.tuke.gamestudio.leaderboard.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_stats")
public class UserStats implements Persistable<UUID> {

    // ── Veľkosť hracej plochy ────────────────────────────────────────────────
    public enum BoardSize {
        SMALL(12),
        MEDIUM(15),
        LARGE(18);

        private final int value;

        BoardSize(int value) { this.value = value; }

        public int getValue() { return value; }

        public static Optional<BoardSize> fromValue(int value) {
            return Arrays.stream(values()).filter(b -> b.value == value).findFirst();
        }

        public static int[] allowedValues() {
            return Arrays.stream(values()).mapToInt(BoardSize::getValue).toArray();
        }
    }

    // ── Entity ────────────────────────────────────────────────────────────────
    @Id
    @Column(name = "user_id")
    private UUID id;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false)
    private int smallWins;

    @Column(nullable = false)
    private int mediumWins;

    @Column(nullable = false)
    private int largeWins;


    @Transient
    private boolean isNew = true;

    public UserStats(UUID userId) {
        this.id = userId;
    }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }

    // ── Výpočet bodov ────────────────────────────────────────────────────────
    public int totalPoints() {
        return smallWins + (2 * mediumWins) + (3 * largeWins);
    }
}
