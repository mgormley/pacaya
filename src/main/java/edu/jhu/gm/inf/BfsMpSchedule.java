package edu.jhu.gm.inf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.globalfac.GlobalFactor;

/**
 * A sequential schedule for (optionally disconnected) factor graphs in which
 * each connected component is a tree.
 * 
 * @author mgormley
 * 
 */
public class BfsMpSchedule implements MpSchedule {

    private static final Logger log = Logger.getLogger(BfsMpSchedule.class);

    private ArrayList<Object> order;
    
    public BfsMpSchedule(FactorGraph fg) {
        // Create the order list.
        order = new ArrayList<Object>();
        
        // Add each connected component to the order.
        for (FgNode root : fg.getConnectedComponents()) {
            // Choose a root node for each connected component.
            root = chooseRoot(fg, root);
            
            // Get a topological order over the nodes in the graph.
            List<FgNode> topoOrder = getTopoOrder(fg, root);
            
            // Make a map from node ids to where they come in the topological order.
            Map<FgNode,Integer> nodeToTopoPosition = getNodeToTopoPositionMap(fg, topoOrder); // {id:i for i, id in enumerate(topological_order)}
            
            // Add to the schedule.
            addToSchedule(topoOrder, nodeToTopoPosition);            
        }
    }

    private FgNode chooseRoot(FactorGraph fg, FgNode root) {
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
            log.debug("Factor graph: " + fg);
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
            return lastGlobalFactor;
        } else {
            return root;
        }
    }

    private List<FgNode> getTopoOrder(FactorGraph fg, FgNode root) {
        // Run a breadth-first search, to get a topographically ordered list of
        // the nodes going from the root down to the leaves.
        List<FgNode> topoOrder = fg.bfs(root);
        // Reverse to get a toposort from leaves to root.
        Collections.reverse(topoOrder);
        return topoOrder;
    }

//    private int VAR_IDX = 0;
//    private int FAC_IDX = 1;
//    
//    private int[][] getNodeToTopoPositionMap(FactorGraph fg, List<FgNode> topoOrder) {
//        int[][] n2p = new int[2][];
//        n2p[VAR_IDX] = new int[fg.getNumVars()];
//        n2p[FAC_IDX] = new int[fg.getNumFactors()];
        
    private Map<FgNode, Integer> getNodeToTopoPositionMap(FactorGraph fg, List<FgNode> topoOrder) {
        Map<FgNode, Integer> n2p = new HashMap<>();       
        for (int i=0; i<topoOrder.size(); i++) {
            FgNode node = topoOrder.get(i);
            n2p.put(node, i);
        }
        return n2p;
    }
    
    private void addToSchedule(List<FgNode> topoOrder, Map<FgNode, Integer> nodeToTopoPosition) {
        // Pass from earlier to later nodes.
        for (int i=0; i<topoOrder.size(); i++) {
            FgNode node = topoOrder.get(i);
            if (node.isFactor() && node.getFactor() instanceof GlobalFactor) {
                // Don't add the global factor yet.
                // TODO: Consider something like this:
                //    if (propAfter(i, node, nodeToTopoPosition) >= 0.5) { order.add(node.getFactor()); }
            } else {
                for (FgEdge edge : node.getOutEdges()) {
                    FgNode neighbor = edge.getChild();
                    if (i < nodeToTopoPosition.get(neighbor)) {
                        order.add(edge);
                    }
                }
            }
        }
        
        // Pass from later to earlier nodes.
        for (int i=topoOrder.size()-1; i>=0; i--) {
            FgNode node = topoOrder.get(i);
            if (node.isFactor() && node.getFactor() instanceof GlobalFactor) {
                // Add the global factor.
                order.add(node.getFactor());
            } else {
                for (FgEdge edge : node.getOutEdges()) {
                    FgNode neighbor = edge.getChild();
                    if (i > nodeToTopoPosition.get(neighbor)) {
                        order.add(edge);
                    }
                }
            }
        }
    }
    
    private double getPropAfter(int i, FgNode node, Map<FgNode, Integer> nodeToTopoPosition) {
        double count = 0;
        for (FgEdge edge : node.getOutEdges()) {
            FgNode neighbor = edge.getChild();
            if (i < nodeToTopoPosition.get(neighbor)) {
                count++;
            }
        }
        return count / node.getOutEdges().size();
    }

    @Override
    public List<Object> getOrder() {
        return order;
    }

}
