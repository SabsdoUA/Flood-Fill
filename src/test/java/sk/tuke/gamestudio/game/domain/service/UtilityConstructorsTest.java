package sk.tuke.gamestudio.game.domain.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class UtilityConstructorsTest {

    @Test
    void givenUtilityClasses_whenReflectivelyInstantiate_thenCoverPrivateConstructors() throws Exception {
        Constructor<FloodFill> ff = FloodFill.class.getDeclaredConstructor();
        ff.setAccessible(true);
        Constructor<WinChecker> wc = WinChecker.class.getDeclaredConstructor();
        wc.setAccessible(true);

        assertThat(ff.newInstance()).isInstanceOf(FloodFill.class);
        assertThat(wc.newInstance()).isInstanceOf(WinChecker.class);
    }
}
