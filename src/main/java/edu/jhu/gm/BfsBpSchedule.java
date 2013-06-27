package edu.jhu.gm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;

import edu.jhu.PipelineRunner;
import edu.jhu.gm.FactorGraph.FgEdge;
import edu.jhu.gm.FactorGraph.FgNode;

/**
 * A sequential schedule for (optionally disconnected) factor graphs in which
 * each connected component is a tree.
 * 
 * @author mgormley
 * 
 */
public class BfsBpSchedule implements BpSchedule {

    private static final Logger log = Logger.getLogger(BfsBpSchedule.class);

    private FactorGraph fg;
    private ArrayList<FgEdge> order;
    
    public BfsBpSchedule(FactorGraph fg) {
        this.fg = fg;
        
        // Create the order list.
        order = new ArrayList<FgEdge>();
        
        // Add each connected component to the order.
        for (FgNode root : fg.getConnectedComponents()) {
            if (!fg.isUndirectedTree(root)) {
                throw new IllegalStateException("Connected component is not a tree: " + root);
            }
            // Choose an arbitrary root node for each connected component.
            addEdgesFromRoot(root);
        }
    }

    private void addEdgesFromRoot(FgNode root) {
        Queue<FgNode> queue = new LinkedList<FgNode>();
        Queue<FgNode> leaves = new LinkedList<FgNode>();
        
        // Run a breadth-first search, just to get the leaves.
        queue.add(root);
        bfsSearch(new ArrayList<FgEdge>(), queue, leaves, true);
        assert(queue.size() == 0);
        assert(leaves.size() > 0);
        
        // Now start by sending messages from the leaves up to the root, and
        // then letting the messages propagate back down to the leaves again.
        //
        // Add edges starting at the leaves and going to the root.
        bfsSearch(order, leaves, queue, false);
        // The "queue" should now contain only the root node.
        assert(queue.size() == 1);
        assert(queue.peek() == root);
        assert(leaves.size() == 0);
        //
        // Add edges starting at the root and going to the leaves.
        bfsSearch(order, queue, leaves, true);
        assert(queue.size() == 0);
        assert(leaves.size() > 0);
    }
    
    private void bfsSearch(ArrayList<FgEdge> order, Queue<FgNode> queue, Queue<FgNode> leaves, boolean topDown) {
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
                // If this is running top down (from root to leaves), always queue the node.
                // Otherwise, only queue a node if all but one of its incoming edges is marked.
                if (topDown || getNumMarked(child.getInEdges()) >= child.getInEdges().size() - 1) {
                    queue.add(child);
                }
            }
            if (numAdded == 0) {
                leaves.add(node);
            }
        }
    }

    private int getNumMarked(List<FgEdge> edges) {
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
