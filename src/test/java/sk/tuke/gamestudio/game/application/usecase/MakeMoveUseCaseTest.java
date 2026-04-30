package sk.tuke.gamestudio.game.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sk.tuke.gamestudio.game.application.GameCommands;
import sk.tuke.gamestudio.game.application.GameResponse;
import sk.tuke.gamestudio.game.application.GameStateMapper;
import sk.tuke.gamestudio.game.domain.model.*;
import sk.tuke.gamestudio.game.domain.port.Ports;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MakeMoveUseCaseTest {

    @Mock private Ports.GameRepository repo;
    @Mock private GameStateMapper mapper;

    @InjectMocks private MakeMoveUseCase useCase;

    private static Board board(Color[][] grid) {
        return new Board(grid, grid.length);
    }

    @Test
    void givenMissingGame_whenExecute_thenThrowNotFound() {
        when(repo.findById("id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new GameCommands.MakeMove("id", "RED"), "qa@example.com"))
                .isInstanceOf(GameDomainException.NotFound.class)
                .hasMessage("Game not found: id");
    }

    @Test
    void givenWonGame_whenExecute_thenThrowAlreadyWon() {
        GameState.Won won = new GameState.Won("id", "qa@example.com", board(new Color[][]{{Color.RED}}), 1, 3);
        when(repo.findById("id")).thenReturn(Optional.of(won));

        assertThatThrownBy(() -> useCase.execute(new GameCommands.MakeMove("id", "BLUE"), "qa@example.com"))
                .isInstanceOf(GameDomainException.AlreadyWon.class);
    }

    @Test
    void givenLostGame_whenExecute_thenThrowMoveLimitReached() {
        GameState.Lost lost = new GameState.Lost("id", "qa@example.com", board(new Color[][]{{Color.RED}}), 3, 3);
        when(repo.findById("id")).thenReturn(Optional.of(lost));

        assertThatThrownBy(() -> useCase.execute(new GameCommands.MakeMove("id", "BLUE"), "qa@example.com"))
                .isInstanceOf(GameDomainException.MoveLimitReached.class);
    }

    @Test
    void givenInvalidColor_whenExecute_thenThrowInvalidColor() {
        GameState.Active active = new GameState.Active("id", "qa@example.com", board(new Color[][]{{Color.RED}}), 0, 3);
        when(repo.findById("id")).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> useCase.execute(new GameCommands.MakeMove("id", "pink"), "qa@example.com"))
                .isInstanceOf(GameDomainException.InvalidColor.class)
                .hasMessage("Invalid color: pink");
    }

    @Test
    void givenSameColorAsBlob_whenExecute_thenReturnCurrentStateWithoutSave() {
        GameState.Active active = new GameState.Active("id", "qa@example.com", board(new Color[][]{{Color.RED, Color.BLUE}, {Color.GREEN, Color.YELLOW}}), 0, 3);
        GameResponse response = new GameResponse("id", null, 0, 3, "ACTIVE", false, null);
        when(repo.findById("id")).thenReturn(Optional.of(active));
        when(mapper.toResponse(active)).thenReturn(response);

        GameResponse result = useCase.execute(new GameCommands.MakeMove("id", "RED"), "qa@example.com");

        assertThat(result).isSameAs(response);
        verify(repo, never()).save(any());
    }

    @Test
    void givenValidMove_whenExecute_thenFloodFillTransitionSaveAndMap() {
        GameState.Active active = new GameState.Active(
                "id",
                "qa@example.com",
                board(new Color[][]{{Color.RED, Color.BLUE}, {Color.BLUE, Color.BLUE}}),
                0,
                3
        );
        GameResponse response = new GameResponse("id", null, 1, 3, "ACTIVE", false, null);

        when(repo.findById("id")).thenReturn(Optional.of(active));
        when(mapper.toResponse(any(GameState.class))).thenReturn(response);

        GameResponse result = useCase.execute(new GameCommands.MakeMove("id", "BLUE"), "qa@example.com");

        assertThat(result).isSameAs(response);
        verify(repo).save(any(GameState.class));
        verify(mapper).toResponse(any(GameState.class));
    }

    @Test
    void givenGameOwnedByAnotherUser_whenExecute_thenRejectAccess() {
        GameState.Active active = new GameState.Active("id", "other@example.com", board(new Color[][]{{Color.RED}}), 0, 3);
        when(repo.findById("id")).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> useCase.execute(new GameCommands.MakeMove("id", "BLUE"), "qa@example.com"))
                .isInstanceOf(GameDomainException.Forbidden.class)
                .hasMessage("Game access denied: id");
    }
}
