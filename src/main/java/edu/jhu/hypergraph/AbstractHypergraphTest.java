package edu.jhu.hypergraph;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.hypergraph.Hypergraph.HyperedgeFn;
import edu.jhu.util.Prm;

public abstract class AbstractHypergraphTest {

    protected abstract Hypergraph getHypergraph();
    
    protected Hypergraph graph;
    
    @Before
    public void setUp() {
        graph = getHypergraph();
    }
    
    @Test
    public void testNoNullNodesApplyTopoSort() {        
        graph.applyTopoSort(new HyperedgeFn() {            
            @Override
            public void apply(Hyperedge e) {
                assertNotNull(e);
                assertNotNull(e.getHeadNode());
                assertNotNull(e.getTailNodes());
                for (Hypernode t : e.getTailNodes()) {
                    assertNotNull(t);
                }
            }
        });
    }
    
    @Test
    public void testNoNullNodesApplyRevTopoSort() {        
        graph.applyRevTopoSort(new HyperedgeFn() {            
            @Override
            public void apply(Hyperedge e) {
                assertNotNull(e);
                assertNotNull(e.getHeadNode());
                assertNotNull(e.getTailNodes());
                for (Hypernode t : e.getTailNodes()) {
                    assertNotNull(t);
                }
            }
        });
    }    
    
    @Test
    public void testEqualityOfTopoSortEdges() {
        final ArrayList<Hyperedge> topo = getTopoOrder();
        final ArrayList<Hyperedge> rev = getRevTopoOrder();
        
        HashSet<Hyperedge> sTopo = new HashSet<Hyperedge>(topo);
        HashSet<Hyperedge> sRev = new HashSet<Hyperedge>(rev);
        HashSet<Hyperedge> sDiff1 = new HashSet<Hyperedge>(topo);
        sDiff1.removeAll(sRev);
        HashSet<Hyperedge> sDiff2 = new HashSet<Hyperedge>(rev);
        sDiff2.removeAll(sTopo);
        System.out.println(sDiff1);
        System.out.println(sDiff2);
        assertEquals(sTopo, sRev);
    }

    /** Safely get the topo order making explicit copies of each edge. */
    protected ArrayList<Hyperedge> getRevTopoOrder() {
        final ArrayList<Hyperedge> rev = new ArrayList<>();
        graph.applyRevTopoSort(new HyperedgeFn() {                
            @Override
            public void apply(Hyperedge e) {
                rev.add(Prm.clone(e));
            }
        });
        return rev;
    }
    
    /** Safely get the reverse topo order making explicit copies of each edge. */
    protected ArrayList<Hyperedge> getTopoOrder() {
        final ArrayList<Hyperedge> topo = new ArrayList<>();
        graph.applyTopoSort(new HyperedgeFn() {                
            @Override
            public void apply(Hyperedge e) {
                topo.add(Prm.clone(e));
            }
        });
        return topo;
    }
    
    @Test
    public void testIsTopoOrderCorrect() {
        // For each edge, check that all of its antecedent nodes have already been completed.
        // How? Visit each edge. Mark the tail nodes as "used". Check that the head node has NOT been used as a tail, if it is we do not have a topo sort.
        ArrayList<Hyperedge> topoOrder = getTopoOrder();
        checkIsValidTopoOrder(graph, topoOrder);
    }

    public static void checkIsValidTopoOrder(Hypergraph graph, ArrayList<Hyperedge> topoOrder) {
        boolean[] usedAsTail = new boolean[graph.getNodes().size()];
        Arrays.fill(usedAsTail, false);
        for (Hyperedge edge : topoOrder) {
            for (Hypernode tail : edge.getTailNodes()) {
                usedAsTail[tail.getId()] = true;
            }
            assertFalse("head: "+edge.getHeadNode(), usedAsTail[edge.getHeadNode().getId()]);
        }
    }
    
    @Test
    public void testIsRevTopoOrderCorrect() {
        // For each edge, check that all of its head nodes have already been completed.
        ArrayList<Hyperedge> edges = getRevTopoOrder();
        Collections.reverse(edges);
        checkIsValidTopoOrder(graph, edges);
    }

}
