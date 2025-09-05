package behavior;

import game.GameEngine;
import game.Ladybug;

public class SequenceNode extends CompositeNode {

  public SequenceNode(String id , String name) {
    super(id,name);
  }

  @Override
  public Status execute(Ladybug ladybug, GameEngine gameEngine) {
    while (currentChildIndex < children.size()) {
      BehaviorNode child = children.get(currentChildIndex);
      Status childStatus = child.execute(ladybug, gameEngine);

      if (childStatus == Status.RUNNING) {
        return Status.RUNNING; // Child is still running, so is sequence
      } else if (childStatus == Status.FAILURE) {
        currentChildIndex = 0; // Reset for next time
        return Status.FAILURE; // Sequence failed
      } else { // childStatus == Status.SUCCESS
        currentChildIndex++; // Move to next child
      }
    }
    currentChildIndex = 0; // Reset for next time
    return Status.SUCCESS; // All children succeeded
  }

  @Override
  public void reset() {
    super.reset();
  }

  @Override
  public String getType() {
    return "sequence";
  }
}