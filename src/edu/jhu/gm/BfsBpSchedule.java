package edu.jhu.gm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;

import edu.jhu.PipelineRunner;
import edu.jhu.gm.FactorGraph.FgEdge;
import edu.jhu.gm.FactorGraph.FgNode;

public class BfsBpSchedule implements BpSchedule {

    private static final Logger log = Logger.getLogger(BfsBpSchedule.class);

    private FactorGraph fg;
    private ArrayList<FgEdge> order;
    
    public BfsBpSchedule(FactorGraph fg) {
        this.fg = fg;

        // TODO: check that this factor graph is a tree.
        //fg.isTree();
        
        // Choose an arbitrary root node.
        FgNode root = fg.getNode(0);

        // Run a breadth-first search.
        order = new ArrayList<FgEdge>();
        Queue<FgNode> queue = new LinkedList<FgNode>();
        Queue<FgNode> leaves = new LinkedList<FgNode>();
        queue.add(root);
        // Add edges starting at the root and going to the leaves.
        bfsSearch(order, queue, leaves, true);
        assert(queue.size() == 0);
        assert(leaves.size() > 0);
        // Unmark all the edges.
        for (FgEdge edge : fg.getEdges()) {
            edge.setMarked(false);
        }
        // Add edges starting at the leaves and going to the root.
        bfsSearch(order, leaves, queue, false);    
        
        // The "queue" should now contain only the root node.
        assert(queue.size() == 1);
        assert(queue.poll() == root);
    }
    
    private void bfsSearch(ArrayList<FgEdge> order, Queue<FgNode> queue, Queue<FgNode> leaves, boolean topDown) {
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
