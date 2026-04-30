package sk.tuke.gamestudio.leaderboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import sk.tuke.gamestudio.authentication.core.model.User;
import sk.tuke.gamestudio.game.domain.model.GameState;
import sk.tuke.gamestudio.game.domain.port.Ports;
import sk.tuke.gamestudio.leaderboard.model.RecordedWin;
import sk.tuke.gamestudio.leaderboard.model.UserStats;
import sk.tuke.gamestudio.leaderboard.model.UserStats.BoardSize;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    // ── Výstupné DTO ──────────────────────────────────────────────────────────
    public record Response(
            String name,
            int smallWins,
            int mediumWins,
            int largeWins,
            int totalPoints
    ) {}

    // ── Aktualizácia výhier podľa veľkosti hracej plochy ─────────────────────
    private static final Map<BoardSize, Consumer<UserStats>> WIN_UPDATERS = Map.of(
            BoardSize.SMALL, s -> s.setSmallWins(s.getSmallWins() + 1),
            BoardSize.MEDIUM, s -> s.setMediumWins(s.getMediumWins() + 1),
            BoardSize.LARGE, s -> s.setLargeWins(s.getLargeWins() + 1)
    );

    private final LeaderboardRepository leaderboardRepository;
    private final RecordedWinRepository recordedWinRepository;
    private final Ports.GameRepository gameRepository;
    private final CacheManager cacheManager;

    // ── Zápis výhry ───────────────────────────────────────────────────────────
    @Transactional
    @Retryable(
            retryFor = {
                    OptimisticLockingFailureException.class,
                    DataIntegrityViolationException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    public void recordWin(User user, String gameId, BoardSize boardSize) {
        log.debug("Recording win: userId={}, gameId={}, boardSize={}", user.getId(), gameId, boardSize);

        requireVerifiedWonGame(user, gameId, boardSize);
        if (!reserveWinRecording(user.getId(), gameId, boardSize)) {
            log.debug("Skipping duplicate win record: userId={}, gameId={}", user.getId(), gameId);
            return;
        }

        var stats = leaderboardRepository.findById(user.getId())
                .orElseGet(() -> createStatsForUser(user));

        WIN_UPDATERS.get(boardSize).accept(stats);
        leaderboardRepository.save(stats);

        // Cache sa maže až po úspešnom commite.
        evictCacheAfterCommit();
    }

    private void requireVerifiedWonGame(User user, String gameId, BoardSize boardSize) {
        var state = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        if (!(state instanceof GameState.Won wonState)) {
            throw new IllegalArgumentException("Game is not in won state: " + gameId);
        }

        int actualSize = wonState.board().size();
        if (actualSize != boardSize.getValue()) {
            throw new IllegalArgumentException(
                    "Board size mismatch for game %s: expected %d but was %d"
                            .formatted(gameId, boardSize.getValue(), actualSize)
            );
        }
        if (wonState.ownerIdentity() == null || !wonState.ownerIdentity().equalsIgnoreCase(userEmail(user))) {
            throw new IllegalArgumentException("Game owner mismatch: " + gameId);
        }
    }

    private static String userEmail(User user) {
        return user.getEmail() == null ? null : user.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean reserveWinRecording(UUID userId, String gameId, BoardSize boardSize) {
        if (recordedWinRepository.existsByUserIdAndGameId(userId, gameId)) {
            return false;
        }

        try {
            recordedWinRepository.saveAndFlush(new RecordedWin(userId, gameId, boardSize));
            return true;
        } catch (DataIntegrityViolationException ex) {
            if (recordedWinRepository.existsByUserIdAndGameId(userId, gameId)) {
                return false;
            }
            throw ex;
        }
    }

    private UserStats createStatsForUser(User user) {
        return new UserStats(user.getId());
    }

    // ── Náhradné spracovanie po vyčerpaní retry ──────────────────────────────
    @Recover
    public void recoverRecordWin(Exception ex, User user, String gameId, BoardSize boardSize) {
        log.error("Failed to record win after 3 retries: userId={}, gameId={}, boardSize={}",
                user.getId(), gameId, boardSize, ex);

        throw new LeaderboardExceptionHandler.WinRecordingException(
                "Could not record win for user " + user.getId()
        );
    }

    // ── Čítanie rebríčka ──────────────────────────────────────────────────────
    @Cacheable(value = "leaderboard", key = "#page + '-' + #size")
    @Transactional(readOnly = true)
    public List<Response> leaderboard(int page, int size) {
        return leaderboardRepository.findLeaderboard(PageRequest.of(page, size)).stream()
                .map(p -> new Response(
                        p.getName(),
                        p.getSmallWins(),
                        p.getMediumWins(),
                        p.getLargeWins(),
                        p.getTotalPoints()
                ))
                .toList();
    }

    // ── Súkromné metódy ───────────────────────────────────────────────────────
    private void evictCacheAfterCommit() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                var cache = cacheManager.getCache("leaderboard");
                if (cache != null) {
                    cache.clear();
                    log.debug("Leaderboard cache evicted after commit");
                }
            }
        });
    }
}
