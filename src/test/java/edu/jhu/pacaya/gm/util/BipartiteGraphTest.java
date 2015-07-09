package edu.jhu.pacaya.gm.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.jhu.pacaya.gm.util.BipartiteGraph.BipVisitor;
import edu.jhu.pacaya.util.JUnitUtils;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.util.Timer;

public class BipartiteGraphTest {

    private BipartiteGraph<String, String> g;

    @Before
    public void setUp() { 
        g = getChainGraph();        
    }
    
    @Test
    public void testGetNumEdges() {          
        // Check number of edges.
        assertEquals(10, g.getNumEdges());
        assertEquals(5, g.getNumUndirEdges());
    }
    
    @Test
    public void testGetNumNbsT1() {                     
        // Check number of neighbors.
        assertEquals(2, g.numNbsT1(0));
        assertEquals(2, g.numNbsT1(1));
        assertEquals(1, g.numNbsT1(2));
    }

    @Test
    public void testGetNumNbsT2() {           
        assertEquals(1, g.numNbsT2(0));
        assertEquals(2, g.numNbsT2(1));
        assertEquals(2, g.numNbsT2(2));
    }

    @Test
    public void testGetChildT1() {      
        // Check neighbors.
        assertEquals(0, g.childT1(0, 0));
        assertEquals(1, g.childT1(0, 1));
        assertEquals(1, g.childT1(1, 0));
        assertEquals(2, g.childT1(1, 1));
        assertEquals(2, g.childT1(2, 0));
    }

    @Test
    public void testGetChildT2() {
        assertEquals(0, g.childT2(0, 0));
        assertEquals(1, g.childT2(1, 0));
        assertEquals(0, g.childT2(1, 1));
        assertEquals(2, g.childT2(2, 0));
        assertEquals(1, g.childT2(2, 1));
    }

    @Test
    public void testGetDualT1() {
        // Check duals.
        assertEquals(0, g.dualT1(0, 0));
        assertEquals(1, g.dualT1(0, 1));
        assertEquals(0, g.childT2(g.childT1(0, 0), g.dualT1(0, 0)));
        assertEquals(0, g.childT2(g.childT1(0, 1), g.dualT1(0, 1)));
        assertEquals(1, g.childT2(g.childT1(1, 0), g.dualT1(1, 0)));
        assertEquals(1, g.childT2(g.childT1(1, 1), g.dualT1(1, 1)));
        assertEquals(2, g.childT2(g.childT1(2, 0), g.dualT1(2, 0)));
    }

    @Test
    public void testGetDualT2() {
        assertEquals(0, g.dualT2(1, 0));
        assertEquals(1, g.dualT2(1, 1));
        assertEquals(0, g.childT1(g.childT2(0, 0), g.dualT2(0, 0)));
        assertEquals(1, g.childT1(g.childT2(1, 0), g.dualT2(1, 0)));
        assertEquals(1, g.childT1(g.childT2(1, 1), g.dualT2(1, 1)));
        assertEquals(2, g.childT1(g.childT2(2, 0), g.dualT2(2, 0)));
        assertEquals(2, g.childT1(g.childT2(2, 1), g.dualT2(2, 1)));
    }
    
    // Also tests isT1T2
    @Test
    public void testGetEdgeT1() {
        // Check edge types.
        assertTrue(g.isT1T2(g.edgeT1(0, 0)));
        assertTrue(g.isT1T2(g.edgeT1(0, 1)));
        assertTrue(g.isT1T2(g.edgeT1(1, 0)));
        assertTrue(g.isT1T2(g.edgeT1(1, 1)));
        assertTrue(g.isT1T2(g.edgeT1(2, 0)));
    }
    
    // Also tests isT1T2
    @Test
    public void testGetEdgeT2() {
        assertFalse(g.isT1T2(g.edgeT2(0, 0)));
        assertFalse(g.isT1T2(g.edgeT2(1, 0)));
        assertFalse(g.isT1T2(g.edgeT2(1, 1)));
        assertFalse(g.isT1T2(g.edgeT2(2, 0)));
        assertFalse(g.isT1T2(g.edgeT2(2, 1)));
    }

    @Test
    public void testParentEChildE() {
        // Check edges.
        assertEquals(0, g.parentE(0));
        assertEquals(0, g.childE(0));
        assertEquals(0, g.parentE(1));
        assertEquals(0, g.childE(1));
        assertEquals(1, g.parentE(2));
        assertEquals(1, g.childE(2));
        assertEquals(1, g.parentE(3));
        assertEquals(1, g.childE(3));
        assertEquals(2, g.parentE(4));
        assertEquals(2, g.childE(4));
        assertEquals(2, g.parentE(5));
        assertEquals(2, g.childE(5));
        assertEquals(0, g.parentE(6));
        assertEquals(1, g.childE(6));
        assertEquals(1, g.parentE(7));
        assertEquals(0, g.childE(7));
    }

    @Test
    public void testT1ET2E() {
        // Check edge values.
        assertEquals("a2", g.t1E(4));
        assertEquals("b2", g.t2E(4));
        assertEquals("a2", g.t1E(5));
        assertEquals("b2", g.t2E(5));
        assertEquals("a0", g.t1E(6));
        assertEquals("b1", g.t2E(6));
        assertEquals("a0", g.t1E(7));
        assertEquals("b1", g.t2E(7));
    }
    
    @Test
    public void testIterE() {
        assertEquals(0, g.iterE(g.edgeT1(0, 0)));
        assertEquals(1, g.iterE(g.edgeT1(0, 1)));
        assertEquals(0, g.iterE(g.edgeT1(1, 0)));
        assertEquals(1, g.iterE(g.edgeT1(1, 1)));
        assertEquals(0, g.iterE(g.edgeT1(2, 0)));

        assertEquals(0, g.iterE(g.edgeT2(0, 0)));
        assertEquals(0, g.iterE(g.edgeT2(1, 0)));
        assertEquals(1, g.iterE(g.edgeT2(1, 1)));
        assertEquals(0, g.iterE(g.edgeT2(2, 0)));
        assertEquals(1, g.iterE(g.edgeT2(2, 1)));
    }

    @Test
    public void testDualE() {
        assertEquals(0, g.dualE(g.edgeT1(0, 0)));
        assertEquals(1, g.dualE(g.edgeT1(0, 1)));
        assertEquals(0, g.dualE(g.edgeT1(1, 0)));
        assertEquals(1, g.dualE(g.edgeT1(1, 1)));
        assertEquals(0, g.dualE(g.edgeT1(2, 0)));
        
        assertEquals(0, g.dualE(g.edgeT2(0, 0)));
        assertEquals(0, g.dualE(g.edgeT2(1, 0)));
        assertEquals(1, g.dualE(g.edgeT2(1, 1)));
        assertEquals(0, g.dualE(g.edgeT2(2, 0)));
        assertEquals(1, g.dualE(g.edgeT2(2, 1)));
    }

    @Test
    public void testOpposingE() {
        assertEquals(2, g.parentE(g.opposingE(g.edgeT1(1, 1))));
        assertEquals(1, g.childE(g.opposingE(g.edgeT1(1, 1))));
    }
    
    @Test
    public void testConnectedComponentsT2() {
        List<String> nodes1 = QLists.getList("a0", "a1", "a2");
        List<String> nodes2 = QLists.getList("b0", "b1", "b2");
        EdgeList el = new EdgeList(7);
        el.addEdge(0, 0);
        el.addEdge(1, 1);
        el.addEdge(2, 2);        
        g = new BipartiteGraph<>(nodes1, nodes2, el);
        
        assertArrayEquals(new int[]{0, 1, 2}, g.getConnectedComponentsT2().elements());
        
        el.addEdge(0, 1);
        el.addEdge(1, 0);
        g = new BipartiteGraph<>(nodes1, nodes2, el);
        assertArrayEquals(new int[]{0, 2}, g.getConnectedComponentsT2().elements());
        
        el.addEdge(2, 1);
        g = new BipartiteGraph<>(nodes1, nodes2, el);
        assertArrayEquals(new int[]{0}, g.getConnectedComponentsT2().elements());

        el.addEdge(2, 0);
        g = new BipartiteGraph<>(nodes1, nodes2, el);
        assertArrayEquals(new int[]{0}, g.getConnectedComponentsT2().elements());
    }
    
    @Test
    public void testBfs() {
        g = getDiamondGraph();
        
        boolean[] marked1 = new boolean[g.numT1Nodes()];
        boolean[] marked2 = new boolean[g.numT2Nodes()];
        IntArrayList edges = g.bfs(0, false, marked1, marked2);
        
        System.out.println("marked1: " + Arrays.toString(marked1));
        System.out.println("marked2: " + Arrays.toString(marked2));
        System.out.println("edges: " + Arrays.toString(edges.elements()));
        JUnitUtils.assertArrayEquals(new boolean[]{true, true}, marked1);
        JUnitUtils.assertArrayEquals(new boolean[]{true, true, true, true}, marked2);
        assertArrayEquals(new int[]{1, 4, 8, 7, 11, 2}, edges.elements());
    }
    
    @Test
    public void testDfsT1DfsT2() {
        g = getDiamondGraph();
        boolean[] marked1 = new boolean[g.numT1Nodes()];
        boolean[] marked2 = new boolean[g.numT2Nodes()];
        final StringBuilder sb = new StringBuilder();
        BipVisitor<String,String> visitor = new BipVisitor<String,String>() {

            @Override
            public void visit(int nodeId, boolean isT1, BipartiteGraph<String,String> g) {
                sb.append(String.format("nodeId=%d isT1=%b\n", nodeId, isT1));
            }
            
        };
        g.dfs(0, true, marked1, marked2, visitor);
        System.out.println(sb.toString());
        String expected = "nodeId=0 isT1=true\n"
                + "nodeId=0 isT1=false\n"
                + "nodeId=2 isT1=false\n"
                + "nodeId=1 isT1=true\n"
                + "nodeId=1 isT1=false\n"
                + "nodeId=3 isT1=false\n";
        assertEquals(expected, sb.toString());
    }
    
    @Test
    public void testBfsTwoConnectedComponents() {
        // Diamond shaped graph with two vars, two unary factors.
        List<String> nodes1 = QLists.getList("v0", "v1");
        List<String> nodes2 = QLists.getList("f0", "f1");
        EdgeList el = new EdgeList(7);
        el.addEdge(0, 0); // 0,1
        el.addEdge(1, 1); // 2,3
        g = new BipartiteGraph<>(nodes1, nodes2, el);
        
        boolean[] marked1 = new boolean[nodes1.size()];
        boolean[] marked2 = new boolean[nodes2.size()];
        {
            // Connected component 1.
            IntArrayList edges = g.bfs(0, false, marked1, marked2);        
            System.out.println("marked1: " + Arrays.toString(marked1));
            System.out.println("marked2: " + Arrays.toString(marked2));
            System.out.println("edges: " + Arrays.toString(edges.elements()));
            JUnitUtils.assertArrayEquals(new boolean[]{true, false}, marked1);
            JUnitUtils.assertArrayEquals(new boolean[]{true, false}, marked2);
            assertArrayEquals(new int[]{1}, edges.elements());
        }
        {
            // Connected component 2.
            IntArrayList edges = g.bfs(1, true, marked1, marked2);        
            System.out.println("marked1: " + Arrays.toString(marked1));
            System.out.println("marked2: " + Arrays.toString(marked2));
            System.out.println("edges: " + Arrays.toString(edges.elements()));
            JUnitUtils.assertArrayEquals(new boolean[]{true, true}, marked1);
            JUnitUtils.assertArrayEquals(new boolean[]{true, true}, marked2);
            assertArrayEquals(new int[]{2}, edges.elements());
        }
    }
    
    @Test
    public void testIsAcyclic() {
        List<String> nodes1 = QLists.getList("a0", "a1", "a2");
        List<String> nodes2 = QLists.getList("b0", "b1", "b2");
        EdgeList el = new EdgeList(7);
        el.addEdge(0, 0);
        el.addEdge(1, 1);
        el.addEdge(2, 2);
        g = new BipartiteGraph<>(nodes1, nodes2, el);
        
        assertTrue(g.isAcyclic());
        
        el.addEdge(0, 1);
        g = new BipartiteGraph<>(nodes1, nodes2, el);
        assertTrue(g.isAcyclic());
        
        el.addEdge(2, 1);
        g = new BipartiteGraph<>(nodes1, nodes2, el);
        assertTrue(g.isAcyclic());

        el.addEdge(2, 0);
        g = new BipartiteGraph<>(nodes1, nodes2, el);
        assertFalse(g.isAcyclic());

        el.addEdge(1, 0);
        g = new BipartiteGraph<>(nodes1, nodes2, el);
        assertFalse(g.isAcyclic());
        
        // Test other graphs.        
        g = getChainGraph();
        assertTrue(g.isAcyclic());
        
        g = getDiamondGraph();
        assertFalse(g.isAcyclic());
    }

    protected BipartiteGraph<String,String> getChainGraph() {
        List<String> nodes1 = QLists.getList("a0", "a1", "a2");
        List<String> nodes2 = QLists.getList("b0", "b1", "b2");
        EdgeList el = new EdgeList(7);
        el.addEdge(0, 0);
        el.addEdge(1, 1);
        el.addEdge(2, 2);
        el.addEdge(0, 1);
        el.addEdge(1, 2);
        //el.addEdge(0, 2);
        return new BipartiteGraph<>(nodes1, nodes2, el);
    }

    public static BipartiteGraph<String,String> getDiamondGraph() {
        // Diamond shaped graph with two vars, two unary factors, and two (identical) binary factors.
        List<String> nodes1 = QLists.getList("v0", "v1");
        List<String> nodes2 = QLists.getList("f0", "f1", "f01a", "f01b");
        EdgeList el = new EdgeList(7);
        el.addEdge(0, 0); // 0,1
        el.addEdge(1, 1); // 2,3
        el.addEdge(0, 2); // 4,5
        el.addEdge(1, 2); // 6,7        
        el.addEdge(0, 3); // 8,9
        el.addEdge(1, 3); // 10,11
        return new BipartiteGraph<>(nodes1, nodes2, el);
    }

    
    @Ignore("Speed test")
    @Test
    public void testSpeedOfArrayCreation() {
        int N = 1600;
        int NN = N * N;
        int M = 2;
        int numTrials = 10;
        for (int trial = 0; trial < numTrials; trial++) {
            {
                // Create several 2d arrays.
                Timer t1 = new Timer();
                t1.start();

                // Create an N x N
                int[][] nbs1 = new int[N][];
                for (int v = 0; v < N; v++) {
                    nbs1[v] = new int[N];
                }
                // Create an NN x 2.
                int[][] nbs2 = new int[NN][];
                for (int f = 0; f < NN; f++) {
                    nbs2[f] = new int[M];
                }

                // Create an N x N
                Object[] msg1 = new Object[NN];
                // Create an NN x 2.
                Object[] msg2 = new Object[NN * M];

                // Create an N x N
                Object[] msgAdj1 = new Object[NN];
                // Create an NN x 2.
                Object[] msgAdj2 = new Object[NN * M];

                t1.stop();
                System.out.println("V1 tot ms: " + t1.totMs());
            }
            {
                // Create several 2d arrays.
                Timer t2 = new Timer();
                t2.start();

                // Create an N x N
                int[][] nbs1 = new int[N][];
                for (int v = 0; v < N; v++) {
                    nbs1[v] = new int[N];
                }
                // Create an NN x M.
                int[][] nbs2 = new int[NN][];
                for (int f = 0; f < NN; f++) {
                    nbs2[f] = new int[M];
                }

                // Create an N x N
                Object[][] msg1 = new Object[N][];
                for (int v = 0; v < N; v++) {
                    msg1[v] = new Object[N];
                }
                // Create an NN x 2.
                Object[][] msg2 = new Object[NN][];
                for (int f = 0; f < NN; f++) {
                    msg2[f] = new Object[M];
                }

                // Create an N x N
                Object[][] msgAdj1 = new Object[N][];
                for (int v = 0; v < N; v++) {
                    msgAdj1[v] = new Object[N];
                }
                // Create an NN x 2.
                Object[][] msgAdj2 = new Object[NN][];
                for (int f = 0; f < NN; f++) {
                    msgAdj2[f] = new Object[M];
                }

                t2.stop();
                System.out.println("V2 tot ms: " + t2.totMs());
            }
        }
    }

}
