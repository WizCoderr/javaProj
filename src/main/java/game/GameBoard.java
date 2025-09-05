package game;

import java.util.List;

/**
 * Represents the 2D game board.
 * The board is a grid of characters representing different objects.
 */
public class GameBoard {
    private final char[][] grid;
    private final int width;
    private final int height;

    /**
     * Constructs a GameBoard from a list of strings representing the rows.
     *
     * @param lines The rows of the board.
     */
    public GameBoard(List<String> lines) {
        if (lines == null || lines.isEmpty() || lines.get(0).isEmpty()) {
            throw new IllegalArgumentException("Error: Board data cannot be empty.");
        }

        this.height = lines.size();
        this.width = lines.get(0).length();
        this.grid = new char[height][width];

        for (int i = 0; i < height; i++) {
            if (lines.get(i).length() != this.width) {
                throw new IllegalArgumentException("Error: All board rows must have the same length.");
            }
            this.grid[i] = lines.get(i).toCharArray();
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Gets the character at a specific coordinate.
     * Coordinates are 1-based.
     *
     * @param x The x-coordinate (1 to width).
     * @param y The y-coordinate (1 to height).
     * @return The character at the given position.
     */
    public char getCell(int x, int y) {
        if (x < 1 || x > width || y < 1 || y > height) {
            return '#'; // Treat out-of-bounds as a wall.
        }
        return grid[y - 1][x - 1];
    }

    /**
     * Sets the character at a specific coordinate.
     * Coordinates are 1-based.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param value The character to place on the board.
     */
    public void setCell(int x, int y, char value) {
        if (x >= 1 && x <= width && y >= 1 && y <= height) {
            grid[y - 1][x - 1] = value;
        }
    }

    public String getBoardString() {
        StringBuilder sb = new StringBuilder();
        // Border format matching expected output
        sb.append("+");
        for (int j = 0; j < width; j++) {
            sb.append("-");
        }
        sb.append("+\n");
        
        for (int i = 0; i < height; i++) {
            sb.append("|");
            for (int j = 0; j < width; j++) {
                char cellChar = grid[i][j];
                sb.append(cellChar);
            }
            sb.append("|\n");
        }
        
        sb.append("+");
        for (int j = 0; j < width; j++) {
            sb.append("-");
        }
        sb.append("+");
        return sb.toString();
    }
}