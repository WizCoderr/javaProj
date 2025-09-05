package command;

import behavior.BehaviorNode;
import game.GameEngine;
import game.Ladybug;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Processes user commands from the command-line interface.
 */
public record CommandProcessor(GameEngine gameEngine) {

    /**
     * Starts the command processing loop, reading from standard input.
     */
    public void processCommands() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                String command = parts[0];

                try {
                    if ("quit".equals(command)) {
                        break;
                    }

                    handleCommand(command, parts);
                } catch (IllegalArgumentException e) {
                    System.out.println("Error: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("Error: An unexpected error occurred: " + e.getMessage());
                }
            }
        }
    }

    private void handleCommand(String command, String[] parts) {
        switch (command) {
            case "load":
                handleLoad(parts);
                break;
            case "list":
                if (parts.length == 2 && "ladybugs".equals(parts[1])) {
                    gameEngine.printAllLadybugs();
                } else {
                    throw new IllegalArgumentException("Invalid 'list' command. Usage: list ladybugs");
                }
                break;
            case "print":
                handlePrintPosition(parts);
                break;
            case "reset":
                handleResetTree(parts);
                break;
            case "jump":
                handleJumpTo(parts);
                break;
            case "head":
                handleHead(parts);
                break;
            case "next":
                if (parts.length == 2 && "action".equals(parts[1])) {
                    gameEngine.executeNextAction();
                } else {
                    throw new IllegalArgumentException("Invalid 'next' command. Usage: next action");
                }
                break;
            case "add":
                if (parts.length >= 2 && "sibling".equals(parts[1])) {
                    handleAddSibling(parts);
                } else {
                    throw new IllegalArgumentException("Invalid 'add' command. Usage: add sibling <ladybug> <nodeId> <definition> OR add sibling <ladybug> <definition>");
                }
                break;
            default:
                System.out.println("Error: Unknown command '" + command + "'");
                break;
        }
    }

    private void handleLoad(String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid 'load' command syntax. Usage: load <type> <path>");
        }

        try {
            if ("board".equals(parts[1])) {
                gameEngine.loadBoard(parts[2]);
            } else if ("trees".equals(parts[1])) {
                gameEngine.loadTrees(Arrays.asList(parts).subList(2, parts.length));
            } else {
                throw new IllegalArgumentException("Can only load 'board' or 'trees'.");
            }
        } catch (NoSuchFileException e) {
            System.out.println("Error: File not found at path: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: An I/O error occurred: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handlePrintPosition(String[] parts) {
        if (parts.length != 3 || !("position".equals(parts[1]))) {
            throw new IllegalArgumentException("Invalid 'print' command. Usage: print position <ladybug>");
        }

        try {
            int ladybugId = Integer.parseInt(parts[2]);
            Ladybug l = gameEngine.getLadybugById(ladybugId);
            if (l != null) {
                System.out.println("(" + l.getX() + "," + l.getY() + ")");
            } else {
                System.out.println("Error: Ladybug " + ladybugId + " not found.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid ladybug ID. Please provide a valid integer.");
        }
    }

    private void handleResetTree(String[] parts) {
        if (parts.length != 3 || !("tree".equals(parts[1]))) {
            throw new IllegalArgumentException("Invalid 'reset' command. Usage: reset tree <ladybug>");
        }

        try {
            int ladybugId = Integer.parseInt(parts[2]);
            Ladybug l = gameEngine.getLadybugById(ladybugId);
            if (l != null) {
                l.resetTree();
                System.out.println("OK");
            } else {
                System.out.println("Error: Ladybug " + ladybugId + " not found.");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleJumpTo(String[] parts) {
        if (parts.length != 4 || !("to".equals(parts[1]))) {
            throw new IllegalArgumentException("Invalid 'jump' command. Usage: jump to <ladybug> <nodeId>");
        }

        try {
            int ladybugId = Integer.parseInt(parts[2]);
            String nodeId = parts[3];
            Ladybug l = gameEngine.getLadybugById(ladybugId);
            if (l != null) {
                if (!l.jumpToNode(nodeId)) {
                    System.out.println("Error: Node " + nodeId + " not found in tree for Ladybug " + ladybugId);
                } else {
                    System.out.println("OK");
                }
            } else {
                System.out.println("Error: Ladybug " + ladybugId + " not found.");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleHead(String[] parts) {
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid 'head' command. Usage: head <ladybug>");
        }

        try {
            int ladybugId = Integer.parseInt(parts[1]);
            Ladybug l = gameEngine.getLadybugById(ladybugId);
            if (l != null && l.getCurrentNode() != null) {
                System.out.println(l.getCurrentNode().getId());
            } else {
                System.out.println("Error: Ladybug " + ladybugId + " not found or has no tree assigned.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid ladybug ID. Please provide a valid integer.");
        }
    }

    private void handleAddSibling(String[] parts) {
        if (parts.length != 5 && parts.length != 4) {
            throw new IllegalArgumentException(
                    "Invalid 'add sibling' command format. Usage: add sibling <ladybug_id> <target_node_id> <new_node_def> OR add sibling <ladybug_id> <new_node_def>");
        }
        try {
            int ladybugId = Integer.parseInt(parts[2]);
            Ladybug ladybug = gameEngine.getLadybugById(ladybugId);
            if (ladybug == null) {
                System.out.println("Error: Ladybug " + ladybugId + " not found.");
                return;
            }

            String targetNodeId;
            String newNodeDef;
            if (parts.length == 5) {
                targetNodeId = parts[3];
                newNodeDef = parts[4];
            } else { // parts.length == 4
                if (ladybug.getCurrentNode() == null) {
                    System.out.println("Error: Ladybug " + ladybugId + " has no current node.");
                    return;
                }
                targetNodeId = ladybug.getCurrentNode().getId();
                newNodeDef = parts[3];
            }

            try {
                BehaviorNode newNode = TreeParser.parseNodeDefinition(newNodeDef);
                if (ladybug.addSibling(targetNodeId, newNode)) {
                    System.out.println("OK");
                } else {
                    System.out.println("Error: Failed to add sibling.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid ladybug ID. Please provide a valid integer.");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
