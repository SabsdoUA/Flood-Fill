package sk.tuke.gamestudio.game.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import sk.tuke.gamestudio.authentication.core.service.UserService;
import sk.tuke.gamestudio.game.application.GameResponse;
import sk.tuke.gamestudio.game.application.usecase.MakeMoveUseCase;
import sk.tuke.gamestudio.game.application.usecase.ResumeGameUseCase;
import sk.tuke.gamestudio.game.application.usecase.StartGameUseCase;
import sk.tuke.gamestudio.leaderboard.LeaderboardService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({GameController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class GameControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StartGameUseCase startGameUseCase;

    @MockBean
    private ResumeGameUseCase resumeGameUseCase;

    @MockBean
    private MakeMoveUseCase makeMoveUseCase;

    @MockBean
    private UserService userService;

    @MockBean
    private LeaderboardService leaderboardService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void givenValidStartPayload_whenPostStart_thenReturnMappedJson() throws Exception {
        when(userService.resolveIdentity(null)).thenReturn(Optional.empty());
        when(userService.currentUser()).thenReturn(null);
        when(startGameUseCase.execute(any(), any())).thenReturn(
                new GameResponse("g1", new String[][]{{"RED"}}, 0, 5, "ACTIVE", false, null)
        );

        mockMvc.perform(post("/api/game/g1/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"size\":12}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.gameId").value("g1"))
                .andExpect(jsonPath("$.moveLimit").value(5))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void givenInvalidStartPayload_whenPostStart_thenReturnBadRequest() throws Exception {
        when(userService.resolveIdentity(null)).thenReturn(Optional.empty());
        when(userService.currentUser()).thenReturn(null);
        mockMvc.perform(post("/api/game/g1/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"size\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.size").value("must be greater than or equal to 1"));

        verifyNoInteractions(startGameUseCase);
    }

    @Test
    void givenBlankColor_whenPostMove_thenReturnBadRequest() throws Exception {
        when(userService.resolveIdentity(null)).thenReturn(Optional.empty());
        when(userService.currentUser()).thenReturn(null);
        mockMvc.perform(post("/api/game/g1/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.color").value("must not be blank"));

        verifyNoInteractions(makeMoveUseCase);
    }
}
