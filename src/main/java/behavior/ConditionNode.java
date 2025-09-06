package behavior;

import game.GameEngine;
import game.Ladybug;

public class ConditionNode extends BehaviorNode {
  private final String condition;

  public ConditionNode(String id, String condition) {
    super(id,condition);
    this.condition = condition;
  }

  @Override
  public Status execute(Ladybug ladybug, GameEngine gameEngine) {
    String[] parts = condition.split("\\s+");
    String command = parts[0];
    boolean success = false;

    switch (command) {
      case "leafFront":
        success = (gameEngine.getCellInFront(ladybug) == '*');
        break;
      case "treeFront":
        success = (gameEngine.getCellInFront(ladybug) == '#');
        break;
      case "mushroomFront":
        success = (gameEngine.getCellInFront(ladybug) == 'o');
        break;
      case "atEdge":
        success = gameEngine.isAtEdge(ladybug);
        break;
      case "existsPath":
        try {
          if (parts.length == 3) { // existsPath X,Y
            String[] coords = parts[1].split(",");
            int targetX = Integer.parseInt(coords[0]);
            int targetY = Integer.parseInt(coords[1]);
            success = gameEngine.findPath(ladybug.getX(), ladybug.getY(), targetX, targetY);
          } else if (parts.length == 4) { // existsPath X1,Y1 X2,Y2
            String[] coords1 = parts[1].split(",");
            int x1 = Integer.parseInt(coords1[0]);
            int y1 = Integer.parseInt(coords1[1]);
            String[] coords2 = parts[2].split(",");
            int x2 = Integer.parseInt(coords2[0]);
            int y2 = Integer.parseInt(coords2[1]);
            success = gameEngine.findPath(x1, y1, x2, y2);
          } else {
            success = false; // Invalid format for existsPath
          }
        } catch (NumberFormatException e) {
          success = false; // Invalid number format for coordinates
        }
        break;
      default:
        success = false; // Unknown condition
        break;
    }

    return success ? Status.SUCCESS : Status.FAILURE;
  }

  @Override
  public void reset() {
    // Stateless, so nothing to reset
  }

  @Override
  public String getType() {
    return "condition";
  }
}