package sk.tuke.gamestudio.leaderboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardConfigTest {

    @Mock
    private LeaderboardRepository leaderboardRepository;
    @Mock
    private TransactionTemplate transactionTemplate;

    @Test
    void givenConfig_whenCreateCacheManager_thenReturnConfiguredLeaderboardCache() {
        // Given
        LeaderboardConfig config = new LeaderboardConfig(leaderboardRepository, transactionTemplate);

        // When
        CacheManager cacheManager = config.cacheManager();

        // Then
        assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
        assertThat(cacheManager.getCache("leaderboard")).isNotNull();
    }

    @Test
    void givenInitializer_whenRun_thenExecuteInsertMissingStatsInTransaction() throws Exception {
        // Given
        LeaderboardConfig config = new LeaderboardConfig(leaderboardRepository, transactionTemplate);
        ApplicationArguments args = new DefaultApplicationArguments(new String[]{});

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            var callback = invocation.<org.springframework.transaction.support.TransactionCallback<Integer>>getArgument(0);
            return callback.doInTransaction(null);
        });

        // When
        config.leaderboardInitializer().run(args);

        // Then
        verify(transactionTemplate).execute(any());
        verify(leaderboardRepository).insertMissingStats();
    }
}
