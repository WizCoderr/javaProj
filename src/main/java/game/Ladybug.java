package game;

import behavior.BehaviorNode;
import behavior.CompositeNode;
import java.util.Map;
import java.util.List;
import behavior.ActionNode;
import behavior.ConditionNode;
import behavior.ParallelNode;
import behavior.SequenceNode;
import behavior.FallbackNode;

/**
 * Represents a ladybug on the game board.
 * Each ladybug has a position, direction, and an associated behavior tree.
 */
public class Ladybug {
    private final int id;
    private int x;
    private int y;
    private char direction;
    private BehaviorNode behaviorTree;
    private BehaviorNode currentNode;
    private Map<String, BehaviorNode> nodeMap;
    private Map<BehaviorNode, CompositeNode> childToParentMap;

    public Ladybug(int id, int x, int y, char direction) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

    public int getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public char getDirection() { return direction; }
    public BehaviorNode getCurrentNode() { return currentNode; }
    public BehaviorNode getBehaviorTree() { return behaviorTree; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void setDirection(char direction) { this.direction = direction; }

    public void setBehaviorTree(BehaviorNode root, Map<String, BehaviorNode> map,
                                Map<BehaviorNode, CompositeNode> parentMap) {
        this.behaviorTree = root;
        this.nodeMap = map;
        this.childToParentMap = parentMap;
        this.currentNode = root;
    }

    public void resetTree() {
        if (behaviorTree != null) {
            this.currentNode = behaviorTree;
            this.behaviorTree.reset();
        }
    }

    public boolean jumpToNode(String nodeId) {
        if (nodeMap != null && nodeMap.containsKey(nodeId)) {
            this.currentNode = nodeMap.get(nodeId);
            return true;
        }
        return false;
    }

    public boolean addSibling(String targetNodeId, BehaviorNode newNode) {
        if (nodeMap == null || childToParentMap == null) {
            throw new IllegalStateException("Behavior tree not initialized for ladybug " + id);
        }
        if (!nodeMap.containsKey(targetNodeId)) {
            throw new IllegalArgumentException("Target node " + targetNodeId + " not found in the tree.");
        }
        if (nodeMap.containsKey(newNode.getId())) {
            throw new IllegalArgumentException("A node with ID " + newNode.getId() + " already exists.");
        }

        BehaviorNode targetNode = nodeMap.get(targetNodeId);
        CompositeNode parent = childToParentMap.get(targetNode);

        if (parent == null) {
            if (targetNode == behaviorTree) {
                throw new IllegalArgumentException("Cannot add sibling to the root node.");
            } else {
                throw new IllegalStateException("Target node " + targetNodeId + " has no parent.");
            }
        }

        parent.addChild(newNode);
        nodeMap.put(newNode.getId(), newNode);
        childToParentMap.put(newNode, parent);
        return true;
    }

    private String getDisplayType(BehaviorNode node) {
        if (node instanceof FallbackNode) return "Fallback";
        if (node instanceof SequenceNode) return "Sequence";  
        if (node instanceof ParallelNode) return "Parallel";
        return node.getName();
    }

    /**
     * Executes behavior tree step by step until an action is performed.
     */
    public void executeNext(GameEngine gameEngine) {
        if (behaviorTree == null) return;
        if (currentNode == null) currentNode = behaviorTree;
        
        // Execute until we hit the first action, then stop
        traverseUntilAction(gameEngine);
    }

    private void traverseUntilAction(GameEngine gameEngine) {
        executeNodeUntilAction(behaviorTree, gameEngine);
        // Reset to beginning for next execution
        currentNode = behaviorTree;
    }

    private BehaviorNode.Status executeNodeUntilAction(BehaviorNode node, GameEngine gameEngine) {
        if (node instanceof FallbackNode) {
            System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " ENTRY");
            FallbackNode fallback = (FallbackNode) node;
            List<BehaviorNode> children = fallback.getChildren();
            
            for (BehaviorNode child : children) {
                BehaviorNode.Status childStatus = executeNodeUntilAction(child, gameEngine);
                if (childStatus == BehaviorNode.Status.SUCCESS) {
                    System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " SUCCESS");
                    return BehaviorNode.Status.SUCCESS;
                }
            }
            System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " FAILURE");
            return BehaviorNode.Status.FAILURE;
            
        } else if (node instanceof SequenceNode) {
            System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " ENTRY");
            SequenceNode sequence = (SequenceNode) node;
            List<BehaviorNode> children = sequence.getChildren();
            
            for (BehaviorNode child : children) {
                BehaviorNode.Status childStatus = executeNodeUntilAction(child, gameEngine);
                if (childStatus == BehaviorNode.Status.FAILURE) {
                    System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " FAILURE");
                    return BehaviorNode.Status.FAILURE;
                }
            }
            System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " SUCCESS");
            return BehaviorNode.Status.SUCCESS;
            
        } else if (node instanceof ParallelNode) {
            System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " ENTRY");
            ParallelNode parallel = (ParallelNode) node;
            List<BehaviorNode> children = parallel.getChildren();
            int successCount = 0;
            
            for (BehaviorNode child : children) {
                BehaviorNode.Status childStatus = executeNodeUntilAction(child, gameEngine);
                if (childStatus == BehaviorNode.Status.SUCCESS) {
                    successCount++;
                }
            }
            
            BehaviorNode.Status result = successCount >= parallel.getSuccessThreshold() ? 
                BehaviorNode.Status.SUCCESS : BehaviorNode.Status.FAILURE;
            System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " " + result);
            return result;
            
        } else if (node instanceof ConditionNode) {
            BehaviorNode.Status status = node.execute(this, gameEngine);
            System.out.println(id + " " + node.getId() + " " + node.getName() + " " + status);
            return status;
            
        } else if (node instanceof ActionNode) {
            BehaviorNode.Status status = node.execute(this, gameEngine);
            System.out.println(id + " " + node.getId() + " " + node.getName() + " " + status);
            gameEngine.printBoard();  // Print board after action
            return status;
        }
        
        return BehaviorNode.Status.FAILURE;
    }
}
