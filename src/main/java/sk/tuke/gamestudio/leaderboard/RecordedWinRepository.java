package sk.tuke.gamestudio.leaderboard;

import org.springframework.data.jpa.repository.JpaRepository;
import sk.tuke.gamestudio.leaderboard.model.RecordedWin;

import java.util.UUID;

public interface RecordedWinRepository extends JpaRepository<RecordedWin, Long> {

    boolean existsByUserIdAndGameId(UUID userId, String gameId);
}
