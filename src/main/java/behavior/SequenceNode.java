package behavior;

import game.GameEngine;
import game.Ladybug;
public class SequenceNode extends CompositeNode {
  public SequenceNode(String id, String name) {
      super(id, name);
  }

  @Override
  public Status execute(Ladybug ladybug, GameEngine gameEngine) {
      // Continue from where we left off
      while (currentChildIndex < children.size()) {
          BehaviorNode child = children.get(currentChildIndex);
          Status childStatus = child.execute(ladybug, gameEngine);

          if (childStatus == Status.FAILURE) {
              reset(); // Reset for next execution
              return Status.FAILURE;
          } else if (childStatus == Status.RUNNING) {
              return Status.RUNNING; // Continue from this child next time
          }
          // On SUCCESS, move to next child
          currentChildIndex++;
      }
      
      reset(); // All children succeeded, reset for next execution
      return Status.SUCCESS;
  }

  @Override
  public String getType() {
      return "sequence";
  }
}