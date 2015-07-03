package edu.jhu.pacaya.gm.inf;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.gm.util.BipartiteGraph;
import edu.jhu.pacaya.gm.util.BipartiteGraph.BipVisitor;
import edu.jhu.prim.arrays.BoolArrays;
import edu.jhu.prim.list.IntArrayList;

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
        order = new ArrayList<>();        
        BipartiteGraph<Var, Factor> bg = fg.getBipgraph();
        
        // Unmark all the nodes.
        boolean[] marked1 = new boolean[bg.numT1Nodes()];
        boolean[] marked2 = new boolean[bg.numT2Nodes()];
        // Unmark all the edges. (only used for checking that all edges are visited)
        boolean[] markedE = new boolean[bg.getNumEdges()];
        // Unmark all the global factors
        boolean[] markedGFup = new boolean[bg.numT2Nodes()];
        boolean[] markedGFdown = new boolean[bg.numT2Nodes()];
        
        // Add each connected component to the order.
        IntArrayList rootT2s = bg.getConnectedComponentsT2();
        for (int i=0; i<rootT2s.size(); i++) {
            int rootT2 = rootT2s.get(i);
            // Choose a root node for each connected component.
            rootT2 = chooseRoot(fg, rootT2);

            // 1. Run a breadth-first search over the nodes of the graph, marking nodes as we visit
            // them. Upon visiting a node, add all its edges to unvisted nodes and add those nodes
            // to the queue. This will produce an edge order from the root to the leaves.
            IntArrayList ccorder = bg.bfs(rootT2, false, marked1, marked2);
            
            // 2. In reverse order (i.e. leaves to root), add the opposing edges from the BFS.
            for (int j=ccorder.size()-1; j >=0; j--) {
                int e = bg.opposingE(ccorder.get(j));
                addEdgeToOrder(e, bg, markedGFup);
                markedE[e] = true;
            }
            
            // 3. Then add all the other edges in their BFS order.
            for (int j=0; j<ccorder.size(); j++) {
                int e = ccorder.get(j);
                addEdgeToOrder(e, bg, markedGFdown);
                markedE[e] = true;
            }
        }
        assert !BoolArrays.contains(marked1, false);
        assert !BoolArrays.contains(marked2, false);
        assert !BoolArrays.contains(markedE, false);
    }

    protected void addEdgeToOrder(int e, BipartiteGraph<Var, Factor> bg, boolean[] markedGF) {
        // If this edge is from a global factor...
        if (!bg.isT1T2(e) && bg.t2E(e) instanceof GlobalFactor) {
            // If the global factor hasn't yet been added to the order...
            if (!markedGF[bg.parentE(e)]) {
                // Add the global factor to the schedule.
                order.add(bg.t2E(e));
                markedGF[bg.parentE(e)] = true;
                // TODO: How could we better handle this case? Currently, we will add global factors
                // to the order twice unless they are the root.
                log.warn("More than one global factor is not (yet) supported with BfsBpSchedule.");
            }
        } else {
            order.add(e);
        }
    }

    private static class GlobalFactorVisitor implements BipVisitor<Var,Factor> {
        int numGlobalFactors = 0;
        int lastGlobalFactor = -1;
        @Override
        public void visit(int nodeId, boolean isT1, BipartiteGraph<Var, Factor> bg) {
            if (!isT1 && bg.getT2s().get(nodeId) instanceof GlobalFactor) {
                lastGlobalFactor = nodeId;
                numGlobalFactors++;
            }
        }
    }
    
    private int chooseRoot(FactorGraph fg, int rootFac) {
        // If we have a single global factor, make it the root.
        GlobalFactorVisitor v = new GlobalFactorVisitor();
        
        BipartiteGraph<Var, Factor> bg = fg.getBipgraph();
        boolean[] marked1 = new boolean[bg.numT1Nodes()];
        boolean[] marked2 = new boolean[bg.numT2Nodes()];
        bg.dfs(rootFac, false, marked1, marked2, v);
        
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
            return rootFac;
        }
    }

    @Override
    public List<Object> getOrder() {
        return order;
    }

}
