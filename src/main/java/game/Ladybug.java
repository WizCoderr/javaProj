package game;

import behavior.BehaviorNode;
import behavior.CompositeNode;
import java.util.Map;
import behavior.ActionNode;
import behavior.ConditionNode;
import behavior.ParallelNode;
import behavior.SequenceNode;
import behavior.FallbackNode;
public class Ladybug {
    private final int id;
    private int x, y;
    private char direction;
    private BehaviorNode behaviorTree;
    private BehaviorNode currentNode;
    private Map<String, BehaviorNode> nodeMap;
    private Map<BehaviorNode, CompositeNode> childToParentMap;
    private ExecutionContext context;

    public Ladybug(int id, int x, int y, char direction) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.context = new ExecutionContext();
    }

    public int getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public char getDirection() { return direction; }
    public BehaviorNode getCurrentNode() { return currentNode; }
    public BehaviorNode getBehaviorTree() { return behaviorTree; }
    
    public void setPosition(int x, int y) { 
        this.x = x; 
        this.y = y; 
    }
    
    public void setDirection(char direction) { 
        this.direction = direction; 
    }

    public void setBehaviorTree(BehaviorNode root, Map<String, BehaviorNode> map,
                                Map<BehaviorNode, CompositeNode> parentMap) {
        this.behaviorTree = root;
        this.nodeMap = map;
        this.childToParentMap = parentMap;
        this.currentNode = root;
    }

    public void resetTree() {
        if (behaviorTree != null) {
            behaviorTree.reset();
            currentNode = behaviorTree;
            context.reset();
        }
    }

    public boolean jumpToNode(String nodeId) {
        if (nodeMap != null && nodeMap.containsKey(nodeId)) {
            currentNode = nodeMap.get(nodeId);
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

    public void executeNext(GameEngine gameEngine) {
        if (behaviorTree == null) return;
        
        context.reset();
        executeNodeWithTrace(behaviorTree, gameEngine, true);
    }

    private BehaviorNode.Status executeNodeWithTrace(BehaviorNode node, GameEngine gameEngine, boolean isRoot) {
        currentNode = node;
        
        if (node instanceof FallbackNode) {
            return executeFallbackWithTrace((FallbackNode) node, gameEngine);
        } else if (node instanceof SequenceNode) {
            return executeSequenceWithTrace((SequenceNode) node, gameEngine);
        } else if (node instanceof ParallelNode) {
            return executeParallelWithTrace((ParallelNode) node, gameEngine);
        } else if (node instanceof ActionNode) {
            BehaviorNode.Status status = node.execute(this, gameEngine);
            System.out.println(id + " " + node.getId() + " " + node.getName() + " " + status);
            context.actionExecuted = true;
            return status;
        } else if (node instanceof ConditionNode) {
            BehaviorNode.Status status = node.execute(this, gameEngine);
            System.out.println(id + " " + node.getId() + " " + node.getName() + " " + status);
            return status;
        }
        
        return BehaviorNode.Status.FAILURE;
    }

    private BehaviorNode.Status executeFallbackWithTrace(FallbackNode node, GameEngine gameEngine) {
        System.out.println(id + " " + node.getId() + " fallback ENTRY");
        
        for (BehaviorNode child : node.getChildren()) {
            if (context.actionExecuted) break;
            
            BehaviorNode.Status childStatus = executeNodeWithTrace(child, gameEngine, false);
            
            if (childStatus == BehaviorNode.Status.SUCCESS) {
                System.out.println(id + " " + node.getId() + " fallback SUCCESS");
                return BehaviorNode.Status.SUCCESS;
            }
        }
        
        System.out.println(id + " " + node.getId() + " fallback FAILURE");
        return BehaviorNode.Status.FAILURE;
    }

    private BehaviorNode.Status executeSequenceWithTrace(SequenceNode node, GameEngine gameEngine) {
        System.out.println(id + " " + node.getId() + " sequence ENTRY");
        
        for (BehaviorNode child : node.getChildren()) {
            if (context.actionExecuted) break;
            
            BehaviorNode.Status childStatus = executeNodeWithTrace(child, gameEngine, false);
            
            if (childStatus == BehaviorNode.Status.FAILURE) {
                System.out.println(id + " " + node.getId() + " sequence FAILURE");
                return BehaviorNode.Status.FAILURE;
            }
        }
        
        System.out.println(id + " " + node.getId() + " sequence SUCCESS");
        return BehaviorNode.Status.SUCCESS;
    }

    private BehaviorNode.Status executeParallelWithTrace(ParallelNode node, GameEngine gameEngine) {
        System.out.println(id + " " + node.getId() + " parallel ENTRY");
        
        int successCount = 0;
        for (BehaviorNode child : node.getChildren()) {
            if (context.actionExecuted) break;
            
            if (executeNodeWithTrace(child, gameEngine, false) == BehaviorNode.Status.SUCCESS) {
                successCount++;
            }
        }
        
        BehaviorNode.Status result = successCount >= node.getSuccessThreshold() ? 
            BehaviorNode.Status.SUCCESS : BehaviorNode.Status.FAILURE;
            
        System.out.println(id + " " + node.getId() + " parallel " + result);
        return result;
    }

    private static class ExecutionContext {
        boolean actionExecuted = false;
        
        void reset() {
            actionExecuted = false;
        }
    }
}