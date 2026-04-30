package sk.tuke.gamestudio.leaderboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import sk.tuke.gamestudio.authentication.core.model.User;
import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;
import sk.tuke.gamestudio.game.domain.model.GameState;
import sk.tuke.gamestudio.game.domain.port.Ports;
import sk.tuke.gamestudio.leaderboard.LeaderboardExceptionHandler.WinRecordingException;
import sk.tuke.gamestudio.leaderboard.model.RecordedWin;
import sk.tuke.gamestudio.leaderboard.model.UserStats;
import sk.tuke.gamestudio.leaderboard.model.UserStats.BoardSize;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private LeaderboardRepository leaderboardRepository;
    @Mock
    private RecordedWinRepository recordedWinRepository;
    @Mock
    private Ports.GameRepository gameRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;
    @InjectMocks
    private LeaderboardService service;

    @Test
    void givenMissingStats_whenRecordWin_thenCreateAndIncrementAndEvictCacheAfterCommit() {
        // Given
        User user = user();
        String gameId = "game-1";
        AtomicReference<TransactionSynchronization> syncRef = new AtomicReference<>();

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(wonGame(gameId, 12)));
        when(recordedWinRepository.existsByUserIdAndGameId(user.getId(), gameId)).thenReturn(false);
        when(recordedWinRepository.saveAndFlush(any(RecordedWin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaderboardRepository.findById(user.getId())).thenReturn(Optional.empty());
        when(cacheManager.getCache("leaderboard")).thenReturn(cache);
        when(leaderboardRepository.save(any(UserStats.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<TransactionSynchronizationManager> syncManager = mockStatic(TransactionSynchronizationManager.class)) {
            syncManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(invocation -> {
                        syncRef.set(invocation.getArgument(0));
                        return null;
                    });

            // When
            service.recordWin(user, gameId, BoardSize.SMALL);

            // Then
            ArgumentCaptor<UserStats> captor = ArgumentCaptor.forClass(UserStats.class);
            verify(recordedWinRepository).saveAndFlush(any(RecordedWin.class));
            verify(leaderboardRepository).save(captor.capture());
            UserStats saved = captor.getValue();

            assertThat(saved.getId()).isEqualTo(user.getId());
            assertThat(saved.getSmallWins()).isEqualTo(1);
            assertThat(saved.getMediumWins()).isZero();
            assertThat(saved.getLargeWins()).isZero();

            assertThat(syncRef.get()).isNotNull();
            syncRef.get().afterCommit();
            verify(cache).clear();
        }
    }

    @ParameterizedTest
    @EnumSource(BoardSize.class)
    void givenExistingStats_whenRecordWin_thenIncrementOnlyExpectedCounter(BoardSize boardSize) {
        // Given
        User user = user();
        String gameId = "game-" + boardSize.name().toLowerCase();
        UserStats existing = new UserStats(user.getId());
        existing.setSmallWins(2);
        existing.setMediumWins(3);
        existing.setLargeWins(4);

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(wonGame(gameId, boardSize.getValue())));
        when(recordedWinRepository.existsByUserIdAndGameId(user.getId(), gameId)).thenReturn(false);
        when(recordedWinRepository.saveAndFlush(any(RecordedWin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaderboardRepository.findById(user.getId())).thenReturn(Optional.of(existing));

        try (MockedStatic<TransactionSynchronizationManager> syncManager = mockStatic(TransactionSynchronizationManager.class)) {
            syncManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(invocation -> null);

            // When
            service.recordWin(user, gameId, boardSize);

            // Then
            verify(leaderboardRepository).save(existing);
            assertThat(existing.getSmallWins()).isEqualTo(boardSize == BoardSize.SMALL ? 3 : 2);
            assertThat(existing.getMediumWins()).isEqualTo(boardSize == BoardSize.MEDIUM ? 4 : 3);
            assertThat(existing.getLargeWins()).isEqualTo(boardSize == BoardSize.LARGE ? 5 : 4);
        }
    }

    @Test
    void givenNullBoardSize_whenRecordWin_thenThrowNullPointerAndDoNotSave() {
        // Given
        User user = user();
        String gameId = "game-null";
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(wonGame(gameId, 12)));

        // When / Then
        assertThrows(NullPointerException.class, () -> service.recordWin(user, gameId, null));

        verify(leaderboardRepository, never()).save(any());
    }

    @Test
    void givenCacheIsMissing_whenAfterCommitCallbackRuns_thenDoNotFailAndDoNotClearAnything() {
        // Given
        User user = user();
        String gameId = "game-cache";
        AtomicReference<TransactionSynchronization> syncRef = new AtomicReference<>();

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(wonGame(gameId, 18)));
        when(recordedWinRepository.existsByUserIdAndGameId(user.getId(), gameId)).thenReturn(false);
        when(recordedWinRepository.saveAndFlush(any(RecordedWin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaderboardRepository.findById(user.getId())).thenReturn(Optional.of(new UserStats(user.getId())));
        when(cacheManager.getCache("leaderboard")).thenReturn(null);

        try (MockedStatic<TransactionSynchronizationManager> syncManager = mockStatic(TransactionSynchronizationManager.class)) {
            syncManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(invocation -> {
                        syncRef.set(invocation.getArgument(0));
                        return null;
                    });

            // When
            service.recordWin(user, gameId, BoardSize.LARGE);

            // Then
            assertThat(syncRef.get()).isNotNull();
            syncRef.get().afterCommit();
            verify(cacheManager).getCache("leaderboard");
            verifyNoInteractions(cache);
        }
    }

    @Test
    void givenGameIsNotWon_whenRecordWin_thenRejectAndDoNotPersistStats() {
        // Given
        User user = user();
        String gameId = "game-active";
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(new GameState.Active(gameId, user.getEmail(), board(12), 2, 10)));

        // When / Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.recordWin(user, gameId, BoardSize.SMALL)
        );

        assertThat(ex.getMessage()).contains("not in won state");
        verifyNoInteractions(recordedWinRepository, leaderboardRepository, cacheManager);
    }

    @Test
    void givenBoardSizeMismatch_whenRecordWin_thenRejectAndDoNotPersistStats() {
        // Given
        User user = user();
        String gameId = "game-mismatch";
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(wonGame(gameId, 15)));

        // When / Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.recordWin(user, gameId, BoardSize.SMALL)
        );

        assertThat(ex.getMessage()).contains("Board size mismatch");
        verifyNoInteractions(recordedWinRepository, leaderboardRepository, cacheManager);
    }

    @Test
    void givenWonGameOwnedByAnotherUser_whenRecordWin_thenRejectAndDoNotPersistStats() {
        User user = user();
        String gameId = "game-owner-mismatch";
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(new GameState.Won(gameId, "other@example.com", board(12), 4, 10)));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.recordWin(user, gameId, BoardSize.SMALL)
        );

        assertThat(ex.getMessage()).contains("Game owner mismatch");
        verifyNoInteractions(recordedWinRepository, leaderboardRepository, cacheManager);
    }

    @Test
    void givenWinAlreadyRecordedForSameGame_whenRecordWin_thenDoNothing() {
        // Given
        User user = user();
        String gameId = "game-duplicate";
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(wonGame(gameId, 12)));
        when(recordedWinRepository.existsByUserIdAndGameId(user.getId(), gameId)).thenReturn(true);

        // When
        service.recordWin(user, gameId, BoardSize.SMALL);

        // Then
        verify(recordedWinRepository, never()).saveAndFlush(any(RecordedWin.class));
        verifyNoInteractions(leaderboardRepository, cacheManager);
    }

    @Test
    void givenDuplicateInsertRace_whenRecordWin_thenTreatAsAlreadyRecorded() {
        // Given
        User user = user();
        String gameId = "game-race";
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(wonGame(gameId, 12)));
        when(recordedWinRepository.existsByUserIdAndGameId(user.getId(), gameId))
                .thenReturn(false)
                .thenReturn(true);
        when(recordedWinRepository.saveAndFlush(any(RecordedWin.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate recorded win"));

        // When
        service.recordWin(user, gameId, BoardSize.SMALL);

        // Then
        verifyNoInteractions(leaderboardRepository, cacheManager);
    }

    @Test
    void givenProjectionRows_whenLeaderboard_thenMapEveryFieldToResponse() {
        // Given
        LeaderboardRepository.Projection alice = projection("Alice", 1, 2, 3, 14);
        LeaderboardRepository.Projection bob = projection("Bob", 0, 1, 0, 2);
        when(leaderboardRepository.findLeaderboard(PageRequest.of(1, 5))).thenReturn(List.of(alice, bob));

        // When
        List<LeaderboardService.Response> response = service.leaderboard(1, 5);

        // Then
        assertThat(response)
                .extracting(LeaderboardService.Response::name,
                        LeaderboardService.Response::smallWins,
                        LeaderboardService.Response::mediumWins,
                        LeaderboardService.Response::largeWins,
                        LeaderboardService.Response::totalPoints)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Alice", 1, 2, 3, 14),
                        org.assertj.core.groups.Tuple.tuple("Bob", 0, 1, 0, 2)
                );
    }

    @Test
    void givenNoRows_whenLeaderboard_thenReturnEmptyList() {
        // Given
        when(leaderboardRepository.findLeaderboard(PageRequest.of(0, 50))).thenReturn(List.of());

        // When
        List<LeaderboardService.Response> response = service.leaderboard(0, 50);

        // Then
        assertThat(response).isEmpty();
    }

    @Test
    void givenResponseRecord_whenComparedAndPrinted_thenHonorValueSemantics() {
        // Given
        LeaderboardService.Response baseline = new LeaderboardService.Response("Alice", 1, 2, 3, 14);
        LeaderboardService.Response same = new LeaderboardService.Response("Alice", 1, 2, 3, 14);
        LeaderboardService.Response different = new LeaderboardService.Response("Bob", 1, 2, 3, 14);

        // When / Then
        assertThat(baseline).isEqualTo(same);
        assertThat(baseline.hashCode()).isEqualTo(same.hashCode());
        assertThat(baseline).isNotEqualTo(different);

        assertThat(baseline.name()).isEqualTo("Alice");
        assertThat(baseline.smallWins()).isEqualTo(1);
        assertThat(baseline.mediumWins()).isEqualTo(2);
        assertThat(baseline.largeWins()).isEqualTo(3);
        assertThat(baseline.totalPoints()).isEqualTo(14);
        assertThat(baseline.toString()).contains("Alice", "smallWins=1", "mediumWins=2", "largeWins=3", "totalPoints=14");
    }

    @ParameterizedTest
    @EnumSource(BoardSize.class)
    void givenRetryExhausted_whenRecoverRecordWin_thenThrowDomainExceptionWithUserId(BoardSize boardSize) {
        // Given
        User user = user();

        // When
        WinRecordingException ex = assertThrows(
                WinRecordingException.class,
                () -> service.recoverRecordWin(new OptimisticLockingFailureException("lock"), user, "game-retry", boardSize)
        );

        // Then
        assertThat(ex.getMessage()).contains(user.getId().toString());
    }

    @Test
    void givenDataIntegrityViolation_whenRecoverRecordWin_thenAlsoThrowDomainException() {
        // Given
        User user = user();

        // When
        WinRecordingException ex = assertThrows(
                WinRecordingException.class,
                () -> service.recoverRecordWin(new DataIntegrityViolationException("duplicate"), user, "game-retry", BoardSize.SMALL)
        );

        // Then
        assertThat(ex.getMessage()).contains("Could not record win for user");
    }

    private static LeaderboardRepository.Projection projection(String name, int small, int medium, int large, int total) {
        return new LeaderboardRepository.Projection() {
            @Override public String getName() { return name; }
            @Override public int getSmallWins() { return small; }
            @Override public int getMediumWins() { return medium; }
            @Override public int getLargeWins() { return large; }
            @Override public int getTotalPoints() { return total; }
        };
    }

    private static User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("qa@example.com");
        return user;
    }

    private static GameState.Won wonGame(String gameId, int size) {
        return new GameState.Won(gameId, "qa@example.com", board(size), 4, 10);
    }

    private static Board board(int size) {
        Color[][] grid = new Color[size][size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                grid[row][col] = Color.RED;
            }
        }
        return new Board(grid, size);
    }
}
