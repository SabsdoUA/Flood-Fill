package sk.tuke.gamestudio.game.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import sk.tuke.gamestudio.authentication.core.service.UserService;
import sk.tuke.gamestudio.game.application.GameResponse;
import sk.tuke.gamestudio.game.application.usecase.MakeMoveUseCase;
import sk.tuke.gamestudio.game.application.usecase.ResumeGameUseCase;
import sk.tuke.gamestudio.game.application.usecase.StartGameUseCase;
import sk.tuke.gamestudio.leaderboard.LeaderboardService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock private StartGameUseCase startGame;
    @Mock private ResumeGameUseCase resumeGame;
    @Mock private MakeMoveUseCase makeMove;
    @Mock private UserService userService;
    @Mock private LeaderboardService leaderboardService;
    @Mock private Principal principal;

    @InjectMocks private GameController controller;

    @Test
    void givenStartRequest_whenStart_thenReturnOkResponse() throws Exception {
        GameResponse response = new GameResponse("id", null, 0, 3, "ACTIVE", false, null);
        when(userService.resolveIdentity(principal)).thenReturn(Optional.of("qa@example.com"));
        when(startGame.execute(any(), any())).thenReturn(response);

        ResponseEntity<?> entity = invoke("start", "id", startBody(12));

        assertThat(entity.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(entity.getBody()).isSameAs(response);
    }

    @Test
    void givenResumeRequest_whenResume_thenReturnOkResponse() throws Exception {
        GameResponse response = new GameResponse("id", null, 1, 3, "ACTIVE", false, null);
        when(userService.resolveIdentity(principal)).thenReturn(Optional.of("qa@example.com"));
        when(resumeGame.execute(any(), any())).thenReturn(response);

        ResponseEntity<?> entity = invoke("resume", "id", startBody(12));

        assertThat(entity.getBody()).isSameAs(response);
    }

    @Test
    void givenMoveRequest_whenMove_thenReturnOkResponse() throws Exception {
        var user = new sk.tuke.gamestudio.authentication.core.model.User();
        GameResponse response = new GameResponse("id", new String[12][12], 2, 3, "WON", true, null);
        when(userService.resolveIdentity(principal)).thenReturn(Optional.of("qa@example.com"));
        when(makeMove.execute(any(), any())).thenReturn(response);
        when(userService.resolveUser(principal)).thenReturn(Optional.of(user));

        ResponseEntity<?> entity = invoke("move", "id", moveBody("RED"));

        assertThat(entity.getBody()).isSameAs(response);
        verify(leaderboardService).recordWin(user, "id", sk.tuke.gamestudio.leaderboard.model.UserStats.BoardSize.SMALL);
    }

    private ResponseEntity<?> invoke(String methodName, String gameId, Object body) throws Exception {
        Method method = GameController.class.getDeclaredMethod(methodName, String.class, Principal.class, body.getClass());
        return (ResponseEntity<?>) method.invoke(controller, gameId, principal, body);
    }

    private static Object startBody(int size) throws Exception {
        Class<?> cls = Class.forName("sk.tuke.gamestudio.game.infrastructure.web.GameController$StartBody");
        Constructor<?> ctor = cls.getDeclaredConstructor(int.class);
        ctor.setAccessible(true);
        return ctor.newInstance(size);
    }

    private static Object moveBody(String color) throws Exception {
        Class<?> cls = Class.forName("sk.tuke.gamestudio.game.infrastructure.web.GameController$MoveBody");
        Constructor<?> ctor = cls.getDeclaredConstructor(String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(color);
    }
}
