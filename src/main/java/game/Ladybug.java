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
        if (behaviorTree == null) {
            return;
        }
        
        // Always start from root and execute once per cycle
        executeNode(behaviorTree, gameEngine);
    }

    private BehaviorNode.Status executeNode(BehaviorNode node, GameEngine gameEngine) {
        String nodeType = getNodeTypeName(node);
        
        BehaviorNode.Status status;
        
        if (node instanceof FallbackNode) {
            // Print compact format: "1 A fallback ENTRY 1 B"
            FallbackNode fallback = (FallbackNode) node;
            if (!fallback.getChildren().isEmpty()) {
                System.out.println(id + " " + node.getId() + " " + nodeType + " ENTRY " + id + " " + 
                    fallback.getChildren().get(0).getId());
            }
            status = executeFallbackNode(fallback, gameEngine);
        } else if (node instanceof SequenceNode) {
            // Print compact format: "sequence ENTRY 1 D"
            SequenceNode sequence = (SequenceNode) node;
            if (!sequence.getChildren().isEmpty()) {
                System.out.println(nodeType + " ENTRY " + id + " " + 
                    sequence.getChildren().get(0).getId());
            }
            status = executeSequenceNode(sequence, gameEngine);
        } else if (node instanceof ParallelNode) {
            // Print compact format: "parallel ENTRY 1 F"
            ParallelNode parallel = (ParallelNode) node;
            if (!parallel.getChildren().isEmpty()) {
                System.out.println(nodeType + " ENTRY " + id + " " + 
                    parallel.getChildren().get(0).getId());
            }
            status = executeParallelNode(parallel, gameEngine);
        } else if (node instanceof ConditionNode) {
            status = ((ConditionNode) node).execute(this, gameEngine);
            // Print condition name and status: "treeFront FAILURE 1 B"
            String conditionName = node.getName(); // Get actual condition name
            System.out.println(conditionName + " " + status + " " + id + " " + node.getId());
        } else if (node instanceof ActionNode) {
            status = ((ActionNode) node).execute(this, gameEngine);
            // Print action name and status: "takeLeaf FAILURE"
            String actionName = node.getName(); // Get actual action name
            System.out.println(actionName + " " + status);
        } else {
            status = BehaviorNode.Status.FAILURE;
        }
        
        return status;
    }

    private BehaviorNode.Status executeFallbackNode(FallbackNode node, GameEngine gameEngine) {
        for (BehaviorNode child : node.getChildren()) {
            BehaviorNode.Status childStatus = executeNode(child, gameEngine);
            if (childStatus == BehaviorNode.Status.SUCCESS) {
                return BehaviorNode.Status.SUCCESS;
            }
        }
        return BehaviorNode.Status.FAILURE;
    }

    private BehaviorNode.Status executeSequenceNode(SequenceNode node, GameEngine gameEngine) {
        for (BehaviorNode child : node.getChildren()) {
            BehaviorNode.Status childStatus = executeNode(child, gameEngine);
            if (childStatus != BehaviorNode.Status.SUCCESS) {
                return childStatus; // Return FAILURE or RUNNING
            }
        }
        return BehaviorNode.Status.SUCCESS;
    }

    private BehaviorNode.Status executeParallelNode(ParallelNode node, GameEngine gameEngine) {
        int successCount = 0;
        int failureCount = 0;
        
        for (BehaviorNode child : node.getChildren()) {
            BehaviorNode.Status childStatus = executeNode(child, gameEngine);
            if (childStatus == BehaviorNode.Status.SUCCESS) {
                successCount++;
            } else if (childStatus == BehaviorNode.Status.FAILURE) {
                failureCount++;
            }
        }
        
        if (successCount >= node.getSuccessThreshold()) {
            return BehaviorNode.Status.SUCCESS;
        }
        if (failureCount > (node.getChildren().size() - node.getSuccessThreshold())) {
            return BehaviorNode.Status.FAILURE;
        }
        return BehaviorNode.Status.RUNNING;
    }

    private String getNodeTypeName(BehaviorNode node) {
        if (node instanceof FallbackNode) return "fallback";
        if (node instanceof SequenceNode) return "sequence";
        if (node instanceof ParallelNode) return "parallel";
        if (node instanceof ConditionNode) return "condition";
        if (node instanceof ActionNode) return "action";
        return "unknown";
    }
}