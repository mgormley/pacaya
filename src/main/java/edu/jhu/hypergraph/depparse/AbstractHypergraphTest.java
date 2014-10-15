package edu.jhu.hypergraph.depparse;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.hypergraph.Hyperedge;
import edu.jhu.hypergraph.Hypergraph;
import edu.jhu.hypergraph.Hypergraph.HyperedgeFn;
import edu.jhu.hypergraph.Hypernode;

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

}
