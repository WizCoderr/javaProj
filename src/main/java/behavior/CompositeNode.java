package behavior;

import java.util.ArrayList;
import java.util.List;

public abstract class CompositeNode extends BehaviorNode {
    protected final List<BehaviorNode> children = new ArrayList<>();
    protected int currentChildIndex; // Added

    public CompositeNode(String id, String name) {
        super(id, name);
        this.currentChildIndex = 0; // Initialize
    }

    public void addChild(BehaviorNode child) {
        children.add(child);
    }

    public List<BehaviorNode> getChildren() {
        return children;
    }

    @Override
    public void reset() {
        this.currentChildIndex = 0; // Reset index
        for (BehaviorNode child : children) {
            child.reset();
        }
    }

    public int getCurrentChildIndex() {
        return currentChildIndex;
    }}