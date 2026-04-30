package sk.tuke.gamestudio.authentication.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import sk.tuke.gamestudio.authentication.core.model.User;
import sk.tuke.gamestudio.authentication.core.service.UserService;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StartController.class)
@AutoConfigureMockMvc(addFilters = false)
class StartControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StartController controller;

    @MockBean
    private UserService userService;

    @MockBean
    private PersistentTokenBasedRememberMeServices rememberMeServices;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void givenOAuthRedirectRequest_whenCallSecured_thenReturnFrontendRedirect() throws Exception {
        ReflectionTestUtils.setField(controller, "frontendUrl", "https://example.com");
        when(userService.currentUser()).thenReturn(null);
        when(userService.resolveUser((UserService.UserContext) null)).thenReturn(Optional.empty());

        mockMvc.perform(get("/secured").secure(true))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com?oauth=1"));
    }

    @Test
    void givenResolvedUser_whenCallCurrentUser_thenReturnDisplayName() throws Exception {
        User user = new User();
        user.setNickname("nick");
        when(userService.resolveOAuthUser()).thenReturn("ok");
        when(userService.resolveUser((java.security.Principal) null)).thenReturn(Optional.empty());
        when(userService.currentUser()).thenReturn(new UserService.UserContext("user@gmail.com"));
        when(userService.resolveUser(new UserService.UserContext("user@gmail.com"))).thenReturn(Optional.of(user));

        mockMvc.perform(get("/secured/user"))
                .andExpect(status().isOk())
                .andExpect(content().string("Prihlásený používateľ: nick"));
    }
}
