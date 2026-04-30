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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeGameUseCaseTest {

    @Mock private Ports.GameRepository repo;
    @Mock private Ports.BoardGenerator generator;
    @Mock private Ports.MoveLimitEstimator moveLimitEstimator;
    @Mock private GameStateMapper mapper;

    @InjectMocks private ResumeGameUseCase useCase;

    @Test
    void givenExistingGame_whenExecute_thenReturnMappedExistingState() {
        GameState.Active state = new GameState.Active("id", "qa@example.com", new Board(new Color[][]{{Color.RED}}, 1), 1, 4);
        GameResponse response = new GameResponse("id", new String[][]{{"RED"}}, 1, 4, "ACTIVE", false, null);
        when(repo.findById("id")).thenReturn(Optional.of(state));
        when(mapper.toResponse(state)).thenReturn(response);

        GameResponse result = useCase.execute(new GameCommands.ResumeGame("id", 12), "qa@example.com");

        assertThat(result).isSameAs(response);
        verify(repo, never()).save(any());
        verify(generator, never()).generate(anyInt());
    }

    @Test
    void givenMissingGame_whenExecute_thenStartNewAndSave() {
        Board board = new Board(new Color[][]{{Color.BLUE}}, 1);
        GameResponse response = new GameResponse("id", new String[][]{{"BLUE"}}, 0, 6, "ACTIVE", false, null);

        when(repo.findById("id")).thenReturn(Optional.empty());
        when(generator.generate(15)).thenReturn(board);
        when(moveLimitEstimator.estimateMoveLimit(board)).thenReturn(6);
        when(mapper.toResponse(any(GameState.Active.class))).thenReturn(response);

        GameResponse result = useCase.execute(new GameCommands.ResumeGame("id", 15), "qa@example.com");

        assertThat(result).isSameAs(response);
        verify(repo).save(any(GameState.Active.class));
    }

    @Test
    void givenExistingGameOwnedByAnotherUser_whenExecute_thenRejectAccess() {
        GameState.Active state = new GameState.Active("id", "other@example.com", new Board(new Color[][]{{Color.RED}}, 1), 1, 4);
        when(repo.findById("id")).thenReturn(Optional.of(state));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                useCase.execute(new GameCommands.ResumeGame("id", 12), "qa@example.com"))
                .isInstanceOf(sk.tuke.gamestudio.game.domain.model.GameDomainException.Forbidden.class)
                .hasMessage("Game access denied: id");
    }
}
