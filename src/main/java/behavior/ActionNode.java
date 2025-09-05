package behavior;

import game.GameEngine;
import game.Ladybug;

public class ActionNode extends BehaviorNode {
  private final String action;

  public ActionNode(String id, String action) {
    super(id,action);
    this.action = action;
  }

  @Override
  public Status execute(Ladybug ladybug, GameEngine gameEngine) {
    String[] parts = action.split("\\s+");
    String command = parts[0];
    boolean success = false;

    switch (command) {
      case "turnLeft":
        success = gameEngine.turn(ladybug, false);
        break;
      case "turnRight":
        success = gameEngine.turn(ladybug, true);
        break;
      case "move":
        success = gameEngine.move(ladybug);
        break;
      case "placeLeaf":
        success = gameEngine.placeLeaf(ladybug);
        break;
      case "takeLeaf":
        success = gameEngine.takeLeaf(ladybug);
        break;
      case "fly":
        try {
          int x = Integer.parseInt(parts[1]);
          int y = Integer.parseInt(parts[2]);
          success = gameEngine.fly(ladybug, x, y);
        } catch (Exception e) {
          success = false; // Invalid format for fly command
        }
        break;
      default:
        success = false; // Unknown action
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
    return "action";
  }
}