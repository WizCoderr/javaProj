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
    private boolean actionExecuted;

    public Ladybug(int id, int x, int y, char direction) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.actionExecuted = false;
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

    /**
     * Executes behavior tree step by step until one action is performed.
     */
    public void executeNext(GameEngine gameEngine) {
        if (behaviorTree == null) return;
        
        // Reset flag and start from root
        actionExecuted = false;
        currentNode = behaviorTree;
        
        // Execute the tree, stopping after the first action
        executeNodeRecursively(behaviorTree, gameEngine);
    }

    private BehaviorNode.Status executeNodeRecursively(BehaviorNode node, GameEngine gameEngine) {
        currentNode = node;
        
        if (node instanceof ActionNode) {
            return executeAction((ActionNode) node, gameEngine);
        } else if (node instanceof ConditionNode) {
            return executeCondition((ConditionNode) node, gameEngine);
        } else if (node instanceof FallbackNode) {
            return executeFallback((FallbackNode) node, gameEngine);
        } else if (node instanceof SequenceNode) {
            return executeSequence((SequenceNode) node, gameEngine);
        } else if (node instanceof ParallelNode) {
            return executeParallel((ParallelNode) node, gameEngine);
        }
        
        return BehaviorNode.Status.FAILURE;
    }

    private BehaviorNode.Status executeFallback(FallbackNode node, GameEngine gameEngine) {
        System.out.println(id + " " + node.getId() + " fallback ENTRY");
        
        List<BehaviorNode> children = node.getChildren();
        
        for (BehaviorNode child : children) {
            if (actionExecuted) {
                break;
            }
            
            BehaviorNode.Status childStatus = executeNodeRecursively(child, gameEngine);
            
            if (childStatus == BehaviorNode.Status.SUCCESS) {
                System.out.println(id + " " + node.getId() + " fallback SUCCESS");
                return BehaviorNode.Status.SUCCESS;
            }
            // Continue to next child on FAILURE
        }
        
        System.out.println(id + " " + node.getId() + " fallback FAILURE");
        return BehaviorNode.Status.FAILURE;
    }

    private BehaviorNode.Status executeSequence(SequenceNode node, GameEngine gameEngine) {
        System.out.println(id + " " + node.getId() + " sequence ENTRY");
        
        List<BehaviorNode> children = node.getChildren();
        
        for (BehaviorNode child : children) {
            if (actionExecuted) {
                break;
            }
            
            BehaviorNode.Status childStatus = executeNodeRecursively(child, gameEngine);
            
            if (childStatus == BehaviorNode.Status.FAILURE) {
                System.out.println(id + " " + node.getId() + " sequence FAILURE");
                return BehaviorNode.Status.FAILURE;
            }
            // Continue to next child on SUCCESS
        }
        
        System.out.println(id + " " + node.getId() + " sequence SUCCESS");
        return BehaviorNode.Status.SUCCESS;
    }

    private BehaviorNode.Status executeParallel(ParallelNode node, GameEngine gameEngine) {
        System.out.println(id + " " + node.getId() + " parallel ENTRY");
        
        List<BehaviorNode> children = node.getChildren();
        int successCount = 0;
        
        for (BehaviorNode child : children) {
            if (actionExecuted) {
                break;
            }
            
            BehaviorNode.Status childStatus = executeNodeRecursively(child, gameEngine);
            if (childStatus == BehaviorNode.Status.SUCCESS) {
                successCount++;
            }
        }
        
        BehaviorNode.Status result = successCount >= node.getSuccessThreshold() ? 
            BehaviorNode.Status.SUCCESS : BehaviorNode.Status.FAILURE;
            
        System.out.println(id + " " + node.getId() + " parallel " + result);
        return result;
    }

    private BehaviorNode.Status executeCondition(ConditionNode node, GameEngine gameEngine) {
        BehaviorNode.Status status = node.execute(this, gameEngine);
        System.out.println(id + " " + node.getId() + " " + node.getName() + " " + status);
        return status;
    }

    private BehaviorNode.Status executeAction(ActionNode node, GameEngine gameEngine) {
        BehaviorNode.Status status = node.execute(this, gameEngine);
        System.out.println(id + " " + node.getId() + " " + node.getName() + " " + status);
        
        // Mark that we've executed an action - this will stop further execution
        actionExecuted = true;
        return status;
    }
}