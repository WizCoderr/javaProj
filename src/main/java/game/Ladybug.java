package game;

import behavior.BehaviorNode;
import behavior.CompositeNode;
import java.util.Map;
import java.util.List;
import java.util.Stack;
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
    private Stack<ExecutionContext> executionStack;

    private static class ExecutionContext {
        BehaviorNode node;
        int childIndex;
        boolean hasEntered;

        ExecutionContext(BehaviorNode node) {
            this.node = node;
            this.childIndex = 0;
            this.hasEntered = false;
        }
    }

    public Ladybug(int id, int x, int y, char direction) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.executionStack = new Stack<>();
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
        this.executionStack.clear();
        if (root != null) {
            this.executionStack.push(new ExecutionContext(root));
        }
    }

    public void resetTree() {
        if (behaviorTree != null) {
            this.currentNode = behaviorTree;
            this.behaviorTree.reset();
            this.executionStack.clear();
            this.executionStack.push(new ExecutionContext(behaviorTree));
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
        if (node instanceof FallbackNode) return "fallback";
        if (node instanceof SequenceNode) return "sequence";  
        if (node instanceof ParallelNode) return "parallel";
        return node.getName();
    }

    /**
     * Executes behavior tree step by step until an action is performed.
     */
    public void executeNext(GameEngine gameEngine) {
        if (behaviorTree == null || executionStack.isEmpty()) {
            return;
        }

        // Continue execution from where we left off
        BehaviorNode.Status result = executeStep(gameEngine);
        
        // Update current node based on the top of the stack
        if (!executionStack.isEmpty()) {
            currentNode = executionStack.peek().node;
        } else {
            // Tree execution finished, restart from root
            currentNode = behaviorTree;
            executionStack.push(new ExecutionContext(behaviorTree));
        }
    }

    private BehaviorNode.Status executeStep(GameEngine gameEngine) {
        if (executionStack.isEmpty()) {
            return BehaviorNode.Status.FAILURE;
        }

        ExecutionContext context = executionStack.peek();
        BehaviorNode node = context.node;

        if (node instanceof FallbackNode) {
            return executeFallback((FallbackNode) node, context, gameEngine);
        } else if (node instanceof SequenceNode) {
            return executeSequence((SequenceNode) node, context, gameEngine);
        } else if (node instanceof ParallelNode) {
            return executeParallel((ParallelNode) node, context, gameEngine);
        } else if (node instanceof ConditionNode) {
            return executeCondition((ConditionNode) node, gameEngine);
        } else if (node instanceof ActionNode) {
            return executeAction((ActionNode) node, gameEngine);
        }

        return BehaviorNode.Status.FAILURE;
    }

    private BehaviorNode.Status executeFallback(FallbackNode node, ExecutionContext context, GameEngine gameEngine) {
        if (!context.hasEntered) {
            System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " ENTRY");
            context.hasEntered = true;
        }

        List<BehaviorNode> children = node.getChildren();
        
        while (context.childIndex < children.size()) {
            BehaviorNode child = children.get(context.childIndex);
            
            // Push child onto stack if not already there
            if (executionStack.peek() == context) {
                executionStack.push(new ExecutionContext(child));
            }
            
            BehaviorNode.Status childStatus = executeStep(gameEngine);
            
            if (childStatus == BehaviorNode.Status.SUCCESS) {
                System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " SUCCESS");
                executionStack.pop(); // Remove this context
                return BehaviorNode.Status.SUCCESS;
            } else if (childStatus == BehaviorNode.Status.FAILURE) {
                context.childIndex++;
                // Child context already popped in recursive call
            } else { // RUNNING
                return BehaviorNode.Status.RUNNING;
            }
        }

        System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " FAILURE");
        executionStack.pop(); // Remove this context
        return BehaviorNode.Status.FAILURE;
    }

    private BehaviorNode.Status executeSequence(SequenceNode node, ExecutionContext context, GameEngine gameEngine) {
        if (!context.hasEntered) {
            System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " ENTRY");
            context.hasEntered = true;
        }

        List<BehaviorNode> children = node.getChildren();
        
        while (context.childIndex < children.size()) {
            BehaviorNode child = children.get(context.childIndex);
            
            // Push child onto stack if not already there
            if (executionStack.peek() == context) {
                executionStack.push(new ExecutionContext(child));
            }
            
            BehaviorNode.Status childStatus = executeStep(gameEngine);
            
            if (childStatus == BehaviorNode.Status.FAILURE) {
                System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " FAILURE");
                executionStack.pop(); // Remove this context
                return BehaviorNode.Status.FAILURE;
            } else if (childStatus == BehaviorNode.Status.SUCCESS) {
                context.childIndex++;
                // Child context already popped in recursive call
            } else { // RUNNING
                return BehaviorNode.Status.RUNNING;
            }
        }

        System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " SUCCESS");
        executionStack.pop(); // Remove this context
        return BehaviorNode.Status.SUCCESS;
    }

    private BehaviorNode.Status executeParallel(ParallelNode node, ExecutionContext context, GameEngine gameEngine) {
        if (!context.hasEntered) {
            System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " ENTRY");
            context.hasEntered = true;
        }

        List<BehaviorNode> children = node.getChildren();
        int successCount = 0;

        for (BehaviorNode child : children) {
            executionStack.push(new ExecutionContext(child));
            BehaviorNode.Status childStatus = executeStep(gameEngine);
            if (childStatus == BehaviorNode.Status.SUCCESS) {
                successCount++;
            }
        }

        BehaviorNode.Status result = successCount >= node.getSuccessThreshold() ? 
            BehaviorNode.Status.SUCCESS : BehaviorNode.Status.FAILURE;
        System.out.println(id + " " + node.getId() + " " + getDisplayType(node) + " " + result);
        executionStack.pop(); // Remove this context
        return result;
    }

    private BehaviorNode.Status executeCondition(ConditionNode node, GameEngine gameEngine) {
        BehaviorNode.Status status = node.execute(this, gameEngine);
        System.out.println(id + " " + node.getId() + " " + node.getName() + " " + status);
        executionStack.pop(); // Remove this context
        return status;
    }

    private BehaviorNode.Status executeAction(ActionNode node, GameEngine gameEngine) {
        BehaviorNode.Status status = node.execute(this, gameEngine);
        System.out.println(id + " " + node.getId() + " " + node.getName() + " " + status);
        gameEngine.printBoard();
        executionStack.pop(); // Remove this context
        return status;
    }
}