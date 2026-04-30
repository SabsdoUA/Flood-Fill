package sk.tuke.gamestudio.game.domain.model;

public sealed interface GameState
        permits GameState.Active, GameState.Won, GameState.Lost {

    String gameId();
    String ownerIdentity();
    Board board();
    int movesTaken();
    int moveLimit();

    record Active(String gameId, String ownerIdentity, Board board, int movesTaken, int moveLimit)
            implements GameState {

        public GameState transition(Board newBoard, boolean won) {
            int next = movesTaken + 1;
            if (won) return new Won(gameId, ownerIdentity, newBoard, next, moveLimit);
            if (next >= moveLimit) return new Lost(gameId, ownerIdentity, newBoard, next, moveLimit);
            return new Active(gameId, ownerIdentity, newBoard, next, moveLimit);
        }
    }

    record Won(String gameId, String ownerIdentity, Board board, int movesTaken, int moveLimit)
            implements GameState {}

    record Lost(String gameId, String ownerIdentity, Board board, int movesTaken, int moveLimit)
            implements GameState {}
}
