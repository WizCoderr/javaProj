package behavior;

import game.GameEngine;
import game.Ladybug;

public abstract class BehaviorNode {
    private final String id;    // e.g. "F"
    private final String name;  // e.g. "takeLeaf" or "move" or "atEdge"

    public BehaviorNode(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /** Unique node ID like A, B, C … */
    public String getId() {
        return id;
    }

    /** Node label from the tree file (e.g., takeLeaf, move, atEdge). */
    public String getName() {
        return name;
    }

    public enum Status {
        SUCCESS, FAILURE, RUNNING
    }

    /** Execute the node’s behavior. */
    public abstract Status execute(Ladybug ladybug, GameEngine gameEngine);

    /** Reset any state (for composites). */
    public abstract void reset();

    /** Node type: "action", "condition", "sequence", "fallback", "parallel". */
    public abstract String getType();
}
