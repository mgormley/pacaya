package edu.jhu.autodiff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public class Toposort {

    public interface Deps<T> {
        List<T> getDeps(T x);
    }

    private Toposort() {
        // Private constructor.
    }

    /**
     * Gets a topological sort for the graph.
     * 
     * @param root The root of the graph.
     * @param deps Functional description of the graph's dependencies.
     * @return The topological sort.
     */
    public static <T> List<T> toposort(T root, Deps<T> deps) {
        List<T> order = new ArrayList<T>();
        HashSet<T> done = new HashSet<T>();
        Stack<T> todo = new Stack<T>();
        HashSet<T> ancestors = new HashSet<T>();
        
        // Run a Tarjan (1976) style topological sort.   
        todo.push(root);
        while (!todo.isEmpty()) {
            T x = todo.peek();
            // Whether all x's descendents are done.
            boolean ready = true;
            for (T y : deps.getDeps(x)) {
                if (!done.contains(y)) {
                    ready = false;
                    todo.push(y);
                }
            }
            if (ready) {
                todo.pop();
                done.add(x);
                ancestors.remove(x);
                order.add(x);
            } else {
                if (ancestors.contains(x)) {
                    throw new IllegalStateException("Graph is not a DAG. Cycle involves node: " + x);
                }
                ancestors.add(x);
            }
        }
        return order;
    }

    /**
     * Gets a topological sort for the graph, where the depth-first search is cutoff by an input set.
     * 
     * @param inputs The input set / leaf set.
     * @param root The root of the graph.
     * @param deps Functional description of the graph's dependencies.
     * @return The topological sort.
     */
    public static <T> List<T> toposort(List<T> inputs,
            T root, final Deps<T> deps) {
        // Get inputs as a set.
        final HashSet<T> inputSet = new HashSet<T>(inputs);
        if (inputSet.size() != inputs.size()) {
            throw new IllegalStateException("Multiple copies of module in inputs list: " + inputs);
        }        
        // Check that inputs set is a valid set of leaves for the given output module.
        checkIsValidLeafSet(inputSet, root, deps);
        
        Deps<T> cutoffDeps = new Deps<T>() {
            @Override
            public List<T> getDeps(T x) {
                if (inputSet.contains(x)) {
                    return Collections.emptyList();
                }
                return deps.getDeps(x);
            }
        };
        return Toposort.toposort(root, cutoffDeps);
    }

    /**
     * Checks that the given inputSet defines a valid leaf set for outMod. A valid leaf set must
     * consist of only descendents of the output, and must define a full cut through the graph with
     * outMod as root.
     */
    public static <T> void checkIsValidLeafSet(HashSet<T> inputSet, T root, Deps<T> deps) {
        {
            // Check that all modules in the input set are descendents of the output module.
            HashSet<T> visited = new HashSet<T>();
            dfs(root, visited, deps);
            if (!visited.containsAll(inputSet)) {
                throw new IllegalStateException("Input set contains modules which are not descendents of the output module: " + inputSet);
            }
        }
        {
            // Check that the input set defines a full cut through the graph with outMod as root.
            HashSet<T> visited = new HashSet<T>();        
            // Mark the inputSet as visited. If it is a valid leaf set, then leaves will be empty upon
            // completion of the DFS.
            visited.addAll(inputSet);
            HashSet<T> leaves = dfs(root, visited, deps);
            if (leaves.size() != 0) {
                throw new IllegalStateException("Input set is not a valid leaf set for the given output module. Extra leaves: " + leaves);
            }
        }
    }

    /**
     * Depth-first search starting at the given output node.
     * 
     * @param root The root node.
     * @param visited The set of visited nodes. Upon completion this set will contain every node
     *            that was visited during this run of depth-first-search.
     * @return The set of leaf nodes.
     */
    // TODO: detect cycles.
    public static <T> HashSet<T> dfs(T root, HashSet<T> visited, Deps<T> deps) {
        // The set of leaves (excluding any which were already marked as visited).
        HashSet<T> leaves = new HashSet<T>();
        // The stack for DFS.
        Stack<T> stack = new Stack<T>();
        stack.push(root);
        while (stack.size() > 0) {
            T p = stack.pop();
            if (visited.add(p)) {
                // Unseen.
                if (deps.getDeps(p).size() == 0) {
                    // Is leaf.
                    leaves.add(p);
                } else {
                    // Not a leaf.
                    stack.addAll(deps.getDeps(p));
                }
            } else {
                // Seen.
                continue;
            }
        }
        return leaves;
    }

}
