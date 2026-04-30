package sk.tuke.gamestudio.game.domain.port;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class PortsTest {

    @Test
    void givenUtilityClass_whenReflectivelyInstantiate_thenConstructorCovered() throws Exception {
        Constructor<Ports> ctor = Ports.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        Object instance = ctor.newInstance();

        assertThat(instance).isInstanceOf(Ports.class);
    }
}
