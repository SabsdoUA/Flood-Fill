package sk.tuke.gamestudio.game.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sk.tuke.gamestudio.game.application.GameCommands;
import sk.tuke.gamestudio.game.application.GameResponse;
import sk.tuke.gamestudio.game.application.GameStateMapper;
import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;
import sk.tuke.gamestudio.game.domain.model.GameState;
import sk.tuke.gamestudio.game.domain.port.Ports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartGameUseCaseTest {

    @Mock private Ports.GameRepository repo;
    @Mock private Ports.BoardGenerator generator;
    @Mock private Ports.MoveLimitEstimator moveLimitEstimator;
    @Mock private GameStateMapper mapper;

    @InjectMocks private StartGameUseCase useCase;

    @Test
    void givenStartCommand_whenExecute_thenGenerateOptimizeSaveAndMap() {
        Board board = new Board(new Color[][]{{Color.RED}}, 1);
        GameResponse response = new GameResponse("id", new String[][]{{"RED"}}, 0, 4, "ACTIVE", false, null);

        when(generator.generate(12)).thenReturn(board);
        when(moveLimitEstimator.estimateMoveLimit(board)).thenReturn(4);
        when(mapper.toResponse(any(GameState.Active.class))).thenReturn(response);

        GameResponse result = useCase.execute(new GameCommands.StartGame("id", 12), "qa@example.com");

        assertThat(result).isSameAs(response);
        verify(repo).save(any(GameState.Active.class));
        verify(generator).generate(12);
        verify(moveLimitEstimator).estimateMoveLimit(board);
        verify(mapper).toResponse(any(GameState.Active.class));
    }
}
