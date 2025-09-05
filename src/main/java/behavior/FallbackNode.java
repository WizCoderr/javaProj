package behavior;

import game.GameEngine;
import game.Ladybug;

public class FallbackNode extends CompositeNode {

  public FallbackNode(String id,String name) {
    super(id,name);
  }

  @Override
  public Status execute(Ladybug ladybug, GameEngine gameEngine) {
    while (currentChildIndex < children.size()) {
      BehaviorNode child = children.get(currentChildIndex);
      Status childStatus = child.execute(ladybug, gameEngine);

      if (childStatus == Status.RUNNING) {
        return Status.RUNNING; // Child is still running, so is fallback
      } else if (childStatus == Status.SUCCESS) {
        currentChildIndex = 0; // Reset for next time
        return Status.SUCCESS; // Fallback succeeded
      } else { // childStatus == Status.FAILURE
        currentChildIndex++; // Try next child
      }
    }
    currentChildIndex = 0; // Reset for next time
    return Status.FAILURE; // All children failed
  }

  @Override
  public void reset() {
    super.reset();
  }

  @Override
  public String getType() {
    return "fallback";
  }
}