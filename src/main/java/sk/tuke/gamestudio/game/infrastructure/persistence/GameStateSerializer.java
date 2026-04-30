package sk.tuke.gamestudio.game.infrastructure.persistence;

import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;
import sk.tuke.gamestudio.game.domain.model.GameState;

import java.nio.charset.StandardCharsets;
import java.util.Map;

final class GameStateSerializer {

    static final byte FORMAT_VERSION = 2;

    private static final Map<Color, Byte> TO_BYTE = Map.of(
            Color.RED, (byte) 0,
            Color.BLUE, (byte) 1,
            Color.GREEN, (byte) 2,
            Color.YELLOW, (byte) 3,
            Color.PURPLE, (byte) 4,
            Color.ORANGE, (byte) 5
    );

    private static final Color[] FROM_BYTE = buildReverseTable();

    private GameStateSerializer() {
    }

    private static Color[] buildReverseTable() {
        var t = new Color[TO_BYTE.size()];
        TO_BYTE.forEach((c, b) -> t[b & 0xFF] = c);
        return t;
    }

    static byte[] serialize(GameState state) {
        int size = state.board().size();
        var grid = state.board().grid();
        byte[] owner = ownerBytes(state.ownerIdentity());
        var buf = new byte[13 + owner.length + size * size];

        buf[0] = FORMAT_VERSION;
        buf[1] = statusByte(state);
        buf[2] = (byte) size;
        writeInt(buf, 3, state.movesTaken());
        writeInt(buf, 7, state.moveLimit());
        writeShort(buf, 11, owner.length);
        System.arraycopy(owner, 0, buf, 13, owner.length);

        int idx = 13 + owner.length;
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                buf[idx++] = TO_BYTE.get(grid[r][c]);

        return buf;
    }

    static GameState deserialize(String gameId, byte[] data) {
        if (data[0] != 1 && data[0] != FORMAT_VERSION)
            throw new IllegalStateException("Unknown board format version: " + data[0]);

        int size = Byte.toUnsignedInt(data[2]);
        int taken = readInt(data, 3);
        int limit = readInt(data, 7);
        String ownerIdentity = null;
        int idx;
        if (data[0] == 1) {
            idx = 11;
        } else {
            int ownerLength = readShort(data, 11);
            ownerIdentity = ownerLength == 0 ? null : new String(data, 13, ownerLength, StandardCharsets.UTF_8);
            idx = 13 + ownerLength;
        }
        var grid = new Color[size][size];

        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                grid[r][c] = FROM_BYTE[data[idx++] & 0xFF];

        var board = new Board(grid, size);
        return switch (data[1]) {
            case 0 -> new GameState.Active(gameId, ownerIdentity, board, taken, limit);
            case 1 -> new GameState.Won(gameId, ownerIdentity, board, taken, limit);
            case 2 -> new GameState.Lost(gameId, ownerIdentity, board, taken, limit);
            default -> throw new IllegalStateException("Unknown status byte: " + data[1]);
        };
    }

    private static byte[] ownerBytes(String ownerIdentity) {
        return ownerIdentity == null ? new byte[0] : ownerIdentity.getBytes(StandardCharsets.UTF_8);
    }

    private static byte statusByte(GameState s) {
        return switch (s) {
            case GameState.Active e -> 0;
            case GameState.Won e -> 1;
            case GameState.Lost e -> 2;
        };
    }

    private static void writeInt(byte[] buf, int off, int v) {
        buf[off] = (byte) (v >>> 24);
        buf[off + 1] = (byte) (v >>> 16);
        buf[off + 2] = (byte) (v >>> 8);
        buf[off + 3] = (byte) v;
    }

    private static void writeShort(byte[] buf, int off, int v) {
        buf[off] = (byte) (v >>> 8);
        buf[off + 1] = (byte) v;
    }

    private static int readInt(byte[] buf, int off) {
        return ((buf[off] & 0xFF) << 24)
                | ((buf[off + 1] & 0xFF) << 16)
                | ((buf[off + 2] & 0xFF) << 8)
                | (buf[off + 3] & 0xFF);
    }

    private static int readShort(byte[] buf, int off) {
        return ((buf[off] & 0xFF) << 8) | (buf[off + 1] & 0xFF);
    }
}
