package behavior;

import game.GameEngine;
import game.Ladybug;

public class ParallelNode extends CompositeNode {
    private final int successThreshold;

    public ParallelNode(String id, int successThreshold, String name) {
        super(id, name);
        this.successThreshold = successThreshold;  // Actually set the parameter
    }

    @Override
    public Status execute(Ladybug ladybug, GameEngine gameEngine) {
        int successCount = 0;
        for (BehaviorNode child : children) {
            if (child.execute(ladybug, gameEngine) == Status.SUCCESS) {
                successCount++;
            }
        }
        return successCount >= successThreshold ? Status.SUCCESS : Status.FAILURE;
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public String getType() {
        return "parallel";
    }

    public int getSuccessThreshold() {
        return successThreshold;
    }
}