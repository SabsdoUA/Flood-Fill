package sk.tuke.gamestudio.game.domain.model;

public abstract sealed class GameDomainException extends RuntimeException
        permits GameDomainException.NotFound,
        GameDomainException.Forbidden,
        GameDomainException.AlreadyWon,
        GameDomainException.MoveLimitReached,
        GameDomainException.InvalidColor,
        GameDomainException.InvalidSize {

    private GameDomainException(String message) {
        super(message);
    }

    public static final class NotFound extends GameDomainException {
        public NotFound(String gameId) {
            super("Game not found: " + gameId);
        }
    }

    public static final class Forbidden extends GameDomainException {
        public Forbidden(String gameId) {
            super("Game access denied: " + gameId);
        }
    }

    public static final class AlreadyWon extends GameDomainException {
        public AlreadyWon() {
            super("Game already won");
        }
    }

    public static final class MoveLimitReached extends GameDomainException {
        public MoveLimitReached() {
            super("Move limit reached");
        }
    }

    public static final class InvalidColor extends GameDomainException {
        public InvalidColor(String color) {
            super("Invalid color: " + color);
        }
    }

    public static final class InvalidSize extends GameDomainException {
        public InvalidSize(int size) {
            super("Invalid board size: " + size);
        }
    }
}
