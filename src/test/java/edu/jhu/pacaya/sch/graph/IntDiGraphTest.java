package edu.jhu.pacaya.sch.graph;

import static edu.jhu.pacaya.sch.graph.DiEdge.edge;
import static edu.jhu.pacaya.sch.util.TestUtils.testEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.Test;

import edu.jhu.pacaya.sch.util.TestUtils;
import edu.jhu.prim.bimap.IntObjectBimap;
import edu.jhu.prim.tuple.Pair; 

public class IntDiGraphTest {
    
    private static double tol = 1E-9; 
    
    @Test
    public void testSimpleGraphWithStart() {
        IntDiGraph g0 = new IntDiGraph();
        assertEquals(-1, g0.max());
        IntDiGraph g = IntDiGraph.simpleGraphWithStart();
        assertEquals(4, g.max());
        assertTrue(TestUtils.checkThrows(() -> g.addNode(-1), IndexOutOfBoundsException.class));
        
        // adding duplicate edges shouldn't change anything
        g.addEdge(2, 3);
        g.addEdge(4, 0);

        assertEquals(Arrays.asList(0,1,2,3,4), g.getNodes());
        assertEquals(Arrays.asList(
                edge(0, 1), // 0
                edge(0, 2), // 1
                edge(1, 3), // 2
                edge(2, 3), // 3
                edge(3, 0), // 4
//              edge(3, 1), // 5
                edge(4, 0), // 6
                edge(4, 3)  // 7
        ), g.getEdges());
        assertEquals(Arrays.asList(0, 3), g.getSuccessors(4));
        assertEquals(Arrays.asList(1, 2), g.getSuccessors(0));
        assertEquals(Arrays.asList(3),    g.getSuccessors(1));
        assertEquals(Arrays.asList(3),    g.getSuccessors(2));
        assertEquals(Arrays.asList(0),    g.getSuccessors(3));

        assertEquals(Arrays.asList(),        g.getPredecessors(4));
        assertEquals(Arrays.asList(3, 4),    g.getPredecessors(0));
        assertEquals(Arrays.asList(0),       g.getPredecessors(1));
        assertEquals(Arrays.asList(0),       g.getPredecessors(2));
        assertEquals(Arrays.asList(1, 2, 4), g.getPredecessors(3));
        assertEquals(4, g.max());
        // edge graph
        // add immediate cycle:
        IntDiGraph g2 = new IntDiGraph();
        g2.addNodesFrom(Arrays.asList(0,1,2,3,4));
        g2.addEdgesFrom(Arrays.asList(
                edge(0, 1), // 0
                edge(0, 2), // 1
                edge(1, 3), // 2
                edge(2, 3), // 3
                edge(3, 0), // 4
                edge(3, 1), // 5
                edge(4, 0), // 6
                edge(4, 3)  // 7
        ));
        {
            Pair<IntDiGraph, IntObjectBimap<DiEdge>> edgeGraphPair = g2.edgeGraph(false);
            IntDiGraph edgeGraph = edgeGraphPair.get1();
            // no self avoid:
            // 0->2, 1->3, 2->4, 2->5, 3->4, 3->5, 4->0, 4->1, 5->2, 6->0, 6->1, 7->4, 7->5 
            assertEquals(Arrays.asList(0,1,2,3,4,5,6,7),
                    edgeGraph.getNodes());
            assertEquals(Arrays.asList(
                    edge(0, 2),
                    edge(1, 3),
                    edge(2, 4),
                    edge(2, 5),
                    edge(3, 4),
                    edge(3, 5),
                    edge(4, 0),
                    edge(4, 1),
                    edge(5, 2),
                    edge(6, 0),
                    edge(6, 1),
                    edge(7, 4),
                    edge(7, 5)), edgeGraph.getEdges());
            IntObjectBimap<DiEdge> nodeMap = edgeGraphPair.get2();
            assertEquals(8, nodeMap.size());
            assertEquals(0, nodeMap.lookupIndex(edge(0, 1)));
            assertEquals(1, nodeMap.lookupIndex(edge(0, 2)));
            assertEquals(2, nodeMap.lookupIndex(edge(1, 3)));
            assertEquals(3, nodeMap.lookupIndex(edge(2, 3)));
            assertEquals(4, nodeMap.lookupIndex(edge(3, 0)));
            assertEquals(5, nodeMap.lookupIndex(edge(3, 1)));
            assertEquals(6, nodeMap.lookupIndex(edge(4, 0)));
            assertEquals(7, nodeMap.lookupIndex(edge(4, 3)));
            assertEquals(nodeMap.lookupObject(0), edge(0, 1));
            assertEquals(nodeMap.lookupObject(1), edge(0, 2));
            assertEquals(nodeMap.lookupObject(2), edge(1, 3));
            assertEquals(nodeMap.lookupObject(3), edge(2, 3));
            assertEquals(nodeMap.lookupObject(4), edge(3, 0));
            assertEquals(nodeMap.lookupObject(5), edge(3, 1));
            assertEquals(nodeMap.lookupObject(6), edge(4, 0));
            assertEquals(nodeMap.lookupObject(7), edge(4, 3));
        }
        {
            Pair<IntDiGraph, IntObjectBimap<DiEdge>> edgeGraphPair = g2.edgeGraph(true);
            IntDiGraph edgeGraph = edgeGraphPair.get1();
            // no self avoid:
            // 0->2, 1->3, 2->4, 2->5, 3->4, 4->0, 4->1, 5->2, 6->0, 6->1, 7->4 
            assertEquals(Arrays.asList(0,1,2,3,4,5,6,7),
                    edgeGraph.getNodes());
            // 0->2, 1->3, 2->4, 3->4, 4->0, 4->1, 6->0, 6->1, 7->4 
            assertEquals(Arrays.asList(
                    edge(0, 2),
                    edge(1, 3),
                    edge(2, 4),
                    edge(3, 4),
                    edge(3, 5),
                    edge(4, 0),
                    edge(4, 1),
                    edge(6, 0),
                    edge(6, 1),
                    edge(7, 4),
                    edge(7, 5)), edgeGraph.getEdges());
            IntObjectBimap<DiEdge> nodeMap = edgeGraphPair.get2();
            assertEquals(8, nodeMap.size());
            assertEquals(0, nodeMap.lookupIndex(edge(0, 1)));
            assertEquals(1, nodeMap.lookupIndex(edge(0, 2)));
            assertEquals(2, nodeMap.lookupIndex(edge(1, 3)));
            assertEquals(3, nodeMap.lookupIndex(edge(2, 3)));
            assertEquals(4, nodeMap.lookupIndex(edge(3, 0)));
            assertEquals(5, nodeMap.lookupIndex(edge(3, 1)));
            assertEquals(6, nodeMap.lookupIndex(edge(4, 0)));
            assertEquals(7, nodeMap.lookupIndex(edge(4, 3)));
            assertEquals(nodeMap.lookupObject(0), edge(0, 1));
            assertEquals(nodeMap.lookupObject(1), edge(0, 2));
            assertEquals(nodeMap.lookupObject(2), edge(1, 3));
            assertEquals(nodeMap.lookupObject(3), edge(2, 3));
            assertEquals(nodeMap.lookupObject(4), edge(3, 0));
            assertEquals(nodeMap.lookupObject(5), edge(3, 1));
            assertEquals(nodeMap.lookupObject(6), edge(4, 0));
            assertEquals(nodeMap.lookupObject(7), edge(4, 3));
        }
    }

    @Test
    public void testCompare() {
        // edge comparator via sort
        assertEquals(Arrays.asList(
                edge(0,  1),
                edge(0,  1),
                edge(0,  2),
                edge(0,  4),
                edge(4,  1),
                edge(5,  1),
                edge(10,  2),
                edge(10,  4),
                edge(11,  2),
                edge(12,  4)
        ), Arrays.asList(
                edge(0,  1),
                edge(12,  4),
                edge(0,  4),
                edge(4,  1),
                edge(0,  1),
                edge(5,  1),
                edge(0,  2),
                edge(10,  2),
                edge(11,  2),
                edge(10,  4)
        ).stream().sorted().collect(Collectors.toList()));
    }
    
    @Test
    public void testEdgeEquals() {
        testEquals(new DiEdge[][] { 
            { edge(1, 2), edge(1, 2) },
            { edge(1, 3), edge(1, 3) },
            { edge(4, 3), edge(4, 3) },
            { edge(4, 2), edge(4, 2) },
            { null }
        });
    }

    @Test
    public void testEdgeToString() {
        assertEquals(edge(1, 2).toString(), new Pair<Integer, Integer>(1, 2).toString());
    }

    public void testFromMatrix() {
        RealMatrix m = new Array2DRowRealMatrix(
                new double[][] { { 0.3, 0.0, 0.3 }, { 0.1, 0.6, 0.7 }, { 0.0, 0.4, 0.2 }, });
        WeightedIntDiGraph g = WeightedIntDiGraph.fromMatrix(m);
        assertEquals(Arrays.asList(
                edge(0, 0), // 0
//                edge(0, 1), // --- has 0.0
                edge(0, 2), // 1
                edge(1, 0), // 2
                edge(1, 1), // 3
                edge(1, 2), // 4
//              edge(2, 0), // --- has 0.0
                edge(2, 1), // 5
                edge(2, 2)), // 6
                g.getEdges());
        assertEquals(0.3, g.getWeight(0, 0), tol);
        assertEquals(0.3, g.getWeight(0, 2), tol);
        assertEquals(0.1, g.getWeight(1, 0), tol);
        assertEquals(0.6, g.getWeight(1, 1), tol);
        assertEquals(0.7, g.getWeight(1, 2), tol);
        assertEquals(0.4, g.getWeight(2, 1), tol);
        assertEquals(0.2, g.getWeight(2, 2), tol);
    }
}
