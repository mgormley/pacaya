package edu.jhu.gm.inf;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;

import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.GlobalFactor;

/**
 * A sequential schedule for (optionally disconnected) factor graphs in which
 * each connected component is a tree.
 * 
 * @author mgormley
 * 
 */
public class BfsBpSchedule implements BpSchedule {

    private static final Logger log = Logger.getLogger(BfsBpSchedule.class);

    private ArrayList<FgEdge> order;
    
    public BfsBpSchedule(FactorGraph fg) {
        // Create the order list.
        order = new ArrayList<FgEdge>();
        
        // Add each connected component to the order.
        for (FgNode root : fg.getConnectedComponents()) {
            if (!fg.isUndirectedTree(root)) {
                System.out.println("Factor graph: " + fg);
                throw new IllegalStateException("Connected component is not a tree: " + root);
            }
            // If we have a single global factor, make it the root.
            int numGlobalFactors = 0;
            FgNode lastGlobalFactor = null;
            for (FgNode node : fg.preOrderTraversal(root)) {
                if (node.isFactor() && node.getFactor() instanceof GlobalFactor) {
                    lastGlobalFactor = node;
                    numGlobalFactors++;
                }
            }
            if (numGlobalFactors > 1) {
                System.out.println("Factor graph: " + fg);
                // TODO: How could we handle this case?
                throw new RuntimeException("More than one global factor is not (yet) supported with BfsBpSchedule.");
            } else if (numGlobalFactors == 1) {
                // Setting the (unique) global factor to the root, given that
                // the connected component is a tree will ensure that the factor
                // only needs to compute all its messages once per iteration,
                // the first time an outgoing edge from it is seen.
                //
                // TODO: This approach is very brittle. It would be nice to have
                // a more general purpose solution.
                root = lastGlobalFactor;
            }
            
            // Choose an arbitrary root node for each connected component.
            addEdgesFromRoot(root, order, fg);            
        }
    }

    public static void addEdgesFromRoot(FgNode root, ArrayList<FgEdge> order, FactorGraph fg) {
        Queue<FgNode> queue = new LinkedList<FgNode>();
        Queue<FgNode> leaves = new LinkedList<FgNode>();
        
        // Run a breadth-first search, to get a topographically ordered list of
        // the edges going from the root down to the leaves.
        queue.add(root);
        ArrayList<FgEdge> rootToLeavesOrder = new ArrayList<FgEdge>();
        bfsSearch(rootToLeavesOrder, queue, leaves, fg);
        assert(queue.size() == 0);
        assert(leaves.size() > 0);
        
        // Add the opposing edges in reverse order of the BFS search from above.
        for (int i=rootToLeavesOrder.size()-1; i >= 0; i--) {
            order.add(rootToLeavesOrder.get(i).getOpposing());
        }
        // Add the BFS search edges in order.
        order.addAll(rootToLeavesOrder);
    }
    
    public static void bfsSearch(ArrayList<FgEdge> order, Queue<FgNode> queue, Queue<FgNode> leaves, FactorGraph fg) {
        // Unmark all the edges.
        fg.setMarkedAllEdges(false);
        
        while (queue.size() > 0) {
            // Process the next node in the queue.
            FgNode node = queue.remove();
            if (log.isTraceEnabled()) { log.trace("node: " + node); }
            int numAdded = 0;
            for (FgEdge edge : node.getOutEdges()) {
                if (edge.isMarked()) {
                    continue;
                }
                // Treat this as an undirected graph by marking this and the
                // opposing edge.
                edge.setMarked(true);
                edge.getOpposing().setMarked(true);
                
                FgNode child = edge.getChild();
                order.add(edge);
                numAdded++;
                // Since this is running top down (from root to leaves), always queue the node.
                queue.add(child);
            }
            if (numAdded == 0) {
                leaves.add(node);
            }
        }
    }

    private static int getNumMarked(List<FgEdge> edges) {
        int numMarked = 0;
        for (FgEdge e : edges) {
            if (e.isMarked()) {
                numMarked++;
            }
        }
        return numMarked;
    }

    @Override
    public List<FgEdge> getOrder() {
        return order;
    }

}
