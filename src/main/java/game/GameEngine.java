package game;

import behavior.BehaviorNode;
import behavior.CompositeNode;
import command.TreeParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.Queue;

public class GameEngine {
    private GameBoard gameBoard;
    private final List<Ladybug> ladybugs = new ArrayList<>();
    private int nextLadybugId = 1;

    public GameBoard getGameBoard() {
        return gameBoard;
    }

    public void loadBoard(String path) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(path));
        this.gameBoard = new GameBoard(lines);
        this.ladybugs.clear();
        this.nextLadybugId = 1;

        for (int y = 1; y <= gameBoard.getHeight(); y++) {
            for (int x = 1; x <= gameBoard.getWidth(); x++) {
                char cell = gameBoard.getCell(x, y);
                if (isLadybugChar(cell)) {
                    ladybugs.add(new Ladybug(nextLadybugId++, x, y, cell));
                }
            }
        }

        for (String row : lines) {
            System.out.println(row);
        }
    }

    public void loadTrees(List<String> paths) throws IOException {
        if (paths.size() > ladybugs.size()) {
            throw new IllegalStateException("Error: Number of trees to load (" + paths.size() + ") exceeds number of ladybugs on board (" + ladybugs.size() + ").");
        }

        final TreeParser parser = new TreeParser();
        // Clear existing behavior trees from ladybugs before assigning new ones
        for (Ladybug ladybug : ladybugs) {
            ladybug.setBehaviorTree(null, new HashMap<>(), new HashMap<>()); // Clear existing tree
        }

        // Only load trees for the first N ladybugs where N = paths.size()
        for (int i = 0; i < paths.size(); i++) {
            List<String> treeLines = Files.readAllLines(Path.of(paths.get(i)));
            for (String row : treeLines) {
                System.out.println(row);
            }

            // Assign tree to the i-th ladybug
            Map<String, BehaviorNode> nodeMap = new HashMap<>();
            Map<BehaviorNode, CompositeNode> childToParentMap = new HashMap<>();
            BehaviorNode root = parser.parse(Path.of(paths.get(i)), nodeMap, childToParentMap, this);
            ladybugs.get(i).setBehaviorTree(root, nodeMap, childToParentMap);
        }

        // Remove ladybugs that don't have trees assigned
        for (int i = ladybugs.size() - 1; i >= paths.size(); i--) {
            Ladybug ladybug = ladybugs.get(i);
            gameBoard.setCell(ladybug.getX(), ladybug.getY(), '.');
            ladybugs.remove(i);
        }
    }

    public Ladybug getLadybugById(int id) {
        return ladybugs.stream().filter(l -> l.getId() == id).findFirst().orElse(null);
    }

    public void executeNextAction() {
        if (ladybugs.isEmpty()) {
            System.out.println("Error: No ladybugs on the board.");
            return;
        }

        // Execute each ladybug in order
        for (Ladybug ladybug : ladybugs) {
            if (ladybug.getBehaviorTree() != null) {
                try {
                    ladybug.executeNext(this);
                } catch (Exception e) {
                    System.out.println("Error: An unexpected error occurred for Ladybug "
                        + ladybug.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    public void printBoard() {
        if (gameBoard != null) {
            System.out.println(gameBoard.getBoardString());
        }
    }

    public void printAllLadybugs() {
        if (ladybugs.isEmpty()) {
            return;
        }

        for (int i = 0; i < ladybugs.size(); i++) {
            if (ladybugs.get(i).getBehaviorTree() != null) {
                if (i > 0) System.out.print(" ");
                System.out.print(ladybugs.get(i).getId());
            }
        }
        System.out.println();
    }

    private boolean isLadybugChar(char c) {
        return c == '^' || c == '>' || c == 'v' || c == '<';
    }

    public boolean turn(Ladybug ladybug, boolean isRight) {
        char currentDir = ladybug.getDirection();
        String dirs = "^>v<";
        int index = dirs.indexOf(currentDir);
        int newIndex = isRight ? (index + 1) % 4 : (index + 3) % 4;
        char newDir = dirs.charAt(newIndex);
        ladybug.setDirection(newDir);
        gameBoard.setCell(ladybug.getX(), ladybug.getY(), newDir);
        return true;
    }

    public boolean move(Ladybug ladybug) {
        int currentX = ladybug.getX();
        int currentY = ladybug.getY();
        int nextX = currentX;
        int nextY = currentY;

        switch (ladybug.getDirection()) {
            case '^': nextY--; break;
            case '>': nextX++; break;
            case 'v': nextY++; break;
            case '<': nextX--; break;
            default: return false;
        }

        char destinationCell = gameBoard.getCell(nextX, nextY);
        if (destinationCell == '#' || isLadybugAt(nextX, nextY, ladybug.getId())) {
            return false;
        }

        if (destinationCell == 'o') {
            int pushToX = nextX + (nextX - currentX);
            int pushToY = nextY + (nextY - currentY);
            char pushToCell = gameBoard.getCell(pushToX, pushToY);
            if (pushToCell != '.') {
                return false;
            }
            gameBoard.setCell(pushToX, pushToY, 'o');
        }

        gameBoard.setCell(currentX, currentY, '.');
        ladybug.setPosition(nextX, nextY);
        gameBoard.setCell(nextX, nextY, ladybug.getDirection());
        return true;
    }

    public boolean fly(Ladybug ladybug, int x, int y) {
        if (gameBoard.getCell(x, y) != '.') {
            return false;
        }

        gameBoard.setCell(ladybug.getX(), ladybug.getY(), '.');
        
        int deltaX = x - ladybug.getX();
        int deltaY = y - ladybug.getY();
        char newDirection;
        
        if (Math.abs(deltaX) >= Math.abs(deltaY)) {
            newDirection = deltaX > 0 ? '>' : '<';
        } else {
            newDirection = deltaY > 0 ? 'v' : '^';
        }
        
        ladybug.setPosition(x, y);
        ladybug.setDirection(newDirection);
        gameBoard.setCell(x, y, newDirection);
        return true;
    }

    public boolean placeLeaf(Ladybug ladybug) {
        int nextX = ladybug.getX();
        int nextY = ladybug.getY();
        
        switch (ladybug.getDirection()) {
            case '^': nextY--; break;
            case '>': nextX++; break;
            case 'v': nextY++; break;
            case '<': nextX--; break;
            default: return false;
        }
        
        char frontCell = gameBoard.getCell(nextX, nextY);
        if (frontCell != '.') {
            return false;
        }
        
        gameBoard.setCell(nextX, nextY, '*');
        return true;
    }

    public boolean takeLeaf(Ladybug ladybug) {
        int nextX = ladybug.getX();
        int nextY = ladybug.getY();
        
        switch (ladybug.getDirection()) {
            case '^': nextY--; break;
            case '>': nextX++; break;
            case 'v': nextY++; break;
            case '<': nextX--; break;
            default: return false;
        }
        
        char frontCell = gameBoard.getCell(nextX, nextY);
        if (frontCell != '*') {
            return false;
        }
        
        gameBoard.setCell(nextX, nextY, '.');
        return true;
    }

    public char getCellInFront(Ladybug ladybug) {
        int nextX = ladybug.getX();
        int nextY = ladybug.getY();
        
        switch (ladybug.getDirection()) {
            case '^': nextY--; break;
            case '>': nextX++; break;
            case 'v': nextY++; break;
            case '<': nextX--; break;
            default: break;
        }
        
        return gameBoard.getCell(nextX, nextY);
    }

    public boolean isAtEdge(Ladybug ladybug) {
        int x = ladybug.getX();
        int y = ladybug.getY();
        return x == 1 || x == gameBoard.getWidth() || y == 1 || y == gameBoard.getHeight();
    }

    private boolean isLadybugAt(int x, int y, int excludeId) {
        for (Ladybug l : ladybugs) {
            if (l.getId() != excludeId && l.getX() == x && l.getY() == y) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidAndWalkable(int x, int y) {
        if (x < 1 || x > gameBoard.getWidth() || y < 1 || y > gameBoard.getHeight()) {
            return false;
        }
        char cell = gameBoard.getCell(x, y);
        return cell == '.' || cell == 'o';
    }

    public boolean findPath(int startX, int startY, int endX, int endY) {
        if (gameBoard == null) {
            return false;
        }

        if (!isValidAndWalkable(startX, startY) || !isValidAndWalkable(endX, endY)) {
            return false;
        }

        Queue<int[]> queue = new LinkedList<>();
        boolean[][] visited = new boolean[gameBoard.getHeight() + 1][gameBoard.getWidth() + 1];
        queue.offer(new int[]{startX, startY});
        visited[startY][startX] = true;

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int currentX = current[0];
            int currentY = current[1];

            if (currentX == endX && currentY == endY) {
                return true;
            }

            for (int i = 0; i < 4; i++) {
                int newX = currentX + dx[i];
                int newY = currentY + dy[i];
                
                if (isValidAndWalkable(newX, newY) && !visited[newY][newX]) {
                    visited[newY][newX] = true;
                    queue.offer(new int[]{newX, newY});
                }
            }
        }
        return false;
    }
}