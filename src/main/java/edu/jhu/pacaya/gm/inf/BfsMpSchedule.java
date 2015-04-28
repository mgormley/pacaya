package edu.jhu.pacaya.gm.inf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraph.FgEdge;
import edu.jhu.pacaya.gm.model.FactorGraph.FgNode;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.gm.util.Visitor;

/**
 * A sequential schedule for (optionally disconnected) factor graphs in which
 * each connected component is a tree.
 * 
 * @author mgormley
 * 
 */
public class BfsMpSchedule implements MpSchedule {

    private static final Logger log = LoggerFactory.getLogger(BfsMpSchedule.class);

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
            int[][] nodeToTopoPosition = getNodeToTopoPositionMap(fg, topoOrder); // {id:i for i, id in enumerate(topological_order)}
            
            // Add to the schedule.
            addToSchedule(topoOrder, nodeToTopoPosition);
        }
    }

    private static class GlobalFactorVisitor implements Visitor<FgNode> {
        int numGlobalFactors = 0;
        FgNode lastGlobalFactor = null;
        @Override
        public void visit(FgNode node) {
            if (node.isFactor() && node.getFactor() instanceof GlobalFactor) {
                lastGlobalFactor = node;
                numGlobalFactors++;
            }
        }
    }
    
    private FgNode chooseRoot(FactorGraph fg, FgNode root) {
        // If we have a single global factor, make it the root.
        GlobalFactorVisitor v = new GlobalFactorVisitor();
        fg.preOrderTraversal(root, v);

        if (v.numGlobalFactors == 1) {
            if (v.numGlobalFactors > 1) {
                // TODO: How could we better handle this case?
                log.warn("More than one global factor is not (yet) supported with BfsBpSchedule.");
            }
            // Setting the (unique) global factor to the root, given that
            // the connected component is a tree will ensure that the factor
            // only needs to compute all its messages once per iteration,
            // the first time an outgoing edge from it is seen.
            //
            // TODO: This approach is very brittle. It would be nice to have
            // a more general purpose solution.
            return v.lastGlobalFactor;
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

    private static int VAR_IDX = 0;
    private static int FAC_IDX = 1;
    
    private int[][] getNodeToTopoPositionMap(FactorGraph fg, List<FgNode> topoOrder) {
        int[][] n2p = new int[2][];
        n2p[VAR_IDX] = new int[fg.getNumVars()];
        n2p[FAC_IDX] = new int[fg.getNumFactors()];
        for (int i=0; i<topoOrder.size(); i++) {
            FgNode node = topoOrder.get(i);
            setPosition(node, n2p, i);
        }
        return n2p;
    }

    private static void setPosition(FgNode node, int[][] n2p, int pos) {
        if (node.isVar()) {
            n2p[VAR_IDX][node.getVar().getId()] = pos;
        } else {
            n2p[FAC_IDX][node.getFactor().getId()] = pos;
        }
    }

    private static int getPosition(FgNode node, int[][] n2p) {
        int pos;
        if (node.isVar()) {
            pos = n2p[VAR_IDX][node.getVar().getId()];
        } else {
            pos = n2p[FAC_IDX][node.getFactor().getId()];
        }
        return pos;
    }
        
    private void addToSchedule(List<FgNode> topoOrder, int[][] nodeToTopoPosition) {
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
                    if (i < getPosition(neighbor, nodeToTopoPosition)) {
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
                    if (i > getPosition(neighbor, nodeToTopoPosition)) {
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
