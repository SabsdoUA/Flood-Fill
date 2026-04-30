package sk.tuke.gamestudio.game.domain.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum Color {
    RED, BLUE, GREEN, YELLOW, PURPLE, ORANGE;

    private static final Map<String, Color> BY_NAME =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(Color::name, c -> c));

    public static Optional<Color> fromString(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(BY_NAME.get(name.toUpperCase()));
    }
}