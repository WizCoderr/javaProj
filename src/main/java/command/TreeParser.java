package command;

import behavior.ActionNode;
import behavior.BehaviorNode;
import behavior.CompositeNode;
import behavior.ConditionNode;
import behavior.FallbackNode;
import behavior.ParallelNode;
import behavior.SequenceNode;
import game.GameEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses behavior tree definitions from a file using a Mermaid-like syntax.
 */
public class TreeParser {
    private static final Pattern BRACKET_PATTERN = Pattern.compile("([A-Za-z0-9]+)\\[(.+?)\\]");
    private static final Pattern PAREN_PATTERN = Pattern.compile("([A-Za-z0-9]+)\\((.+?)\\)");
    private static final Pattern EDGE_PATTERN = Pattern.compile("([A-Za-z0-9]+)\\s*-->\\s*([A-Za-z0-9]+)");

    public BehaviorNode parse(Path path,
                              Map<String, BehaviorNode> nodeMap,
                              Map<BehaviorNode, CompositeNode> childToParentMap,
                              GameEngine engine) throws IOException {
        List<String> lines = Files.readAllLines(path);
        Map<String, List<String>> childIdMap = new HashMap<>();
        int actionNodeCount = 0;

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() ||
                trimmedLine.startsWith("graph TD") ||
                trimmedLine.startsWith("flowchart TD")) {
                continue; // skip headers / empty lines
            }

            // Node definitions
            Matcher bracketMatcher = BRACKET_PATTERN.matcher(trimmedLine);
            while (bracketMatcher.find()) {
                String id = bracketMatcher.group(1);
                String label = bracketMatcher.group(2);
                BehaviorNode node = createNode(id, label);
                nodeMap.put(id, node);
                if (node instanceof ActionNode) actionNodeCount++;
            }

            Matcher parenMatcher = PAREN_PATTERN.matcher(trimmedLine);
            while (parenMatcher.find()) {
                String id = parenMatcher.group(1);
                String label = parenMatcher.group(2);
                // Strip brackets from condition labels
                if (label.startsWith("[") && label.endsWith("]")) {
                    label = label.substring(1, label.length() - 1);
                }
                BehaviorNode node = createNode(id, label);
                nodeMap.put(id, node);
                if (node instanceof ActionNode) actionNodeCount++;
            }

            // Edges
            Matcher edgeMatcher = EDGE_PATTERN.matcher(trimmedLine);
            if (edgeMatcher.find()) {
                String parentId = edgeMatcher.group(1);
                String childId = edgeMatcher.group(2);
                childIdMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);
            }
        }

        if (actionNodeCount == 0) {
            throw new IllegalStateException("Error: Behavior tree must contain at least one action node.");
        }

        // Link children
        for (BehaviorNode node : nodeMap.values()) {
            if (node instanceof CompositeNode parent) {
                List<String> childrenIds = childIdMap.getOrDefault(parent.getId(), new ArrayList<>());
                for (String childId : childrenIds) {
                    BehaviorNode childNode = nodeMap.get(childId);
                    if (childNode != null) {
                        parent.addChild(childNode);
                        childToParentMap.put(childNode, parent);
                    }
                }
            }
        }

        // Find root
        Map<String, Boolean> isChild = new HashMap<>();
        childIdMap.values().forEach(list -> list.forEach(id -> isChild.put(id, true)));
        for (String id : nodeMap.keySet()) {
            if (!isChild.containsKey(id)) {
                return nodeMap.get(id);
            }
        }

        throw new IllegalStateException("Error: Could not find a root node in the tree.");
    }

    /**
     * Creates either a composite, action, or condition node.
     */
    private static BehaviorNode createNode(String id, String label) {
        // Composite nodes
        if ("?".equals(label)) {
            return new FallbackNode(id, "Fallback");
        }

        if ("->".equals(label)) {
            return new SequenceNode(id, "Sequence");
        }

        if (label.startsWith("=") && label.endsWith(">")) {
            String mString = label.substring(1, label.length() - 1);
            int m = Integer.parseInt(mString);
            return new ParallelNode(id, m, "Parallel");
        }

        // Action nodes
        if (isAction(label)) {
            return new ActionNode(id, label);
        }

        // Otherwise, treat as condition
        return new ConditionNode(id, label);
    }

    private static boolean isAction(String label) {
        return label.equals("turnLeft") ||
               label.equals("turnRight") ||
               label.equals("move") ||
               label.equals("placeLeaf") ||
               label.equals("takeLeaf") ||
               label.startsWith("fly"); // fly(x,y)
    }

    /**
     * Parses a standalone node definition (e.g. "F[move]" or "I([atEdge])").
     */
    public static BehaviorNode parseNodeDefinition(String definition) {
        Matcher bracketMatcher = BRACKET_PATTERN.matcher(definition);
        Matcher parenMatcher = PAREN_PATTERN.matcher(definition);
        if (bracketMatcher.find()) {
            return createNode(bracketMatcher.group(1), bracketMatcher.group(2));
        } else if (parenMatcher.find()) {
            String label = parenMatcher.group(2);
            if (label.startsWith("[") && label.endsWith("]")) {
                label = label.substring(1, label.length() - 1);
            }
            return createNode(parenMatcher.group(1), label);
        }

        throw new IllegalArgumentException("Error: Invalid node definition format: " + definition);
    }
}