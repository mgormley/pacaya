package edu.jhu.pacaya.sch.graph;

import static edu.jhu.pacaya.sch.graph.DiEdge.edge;
import static edu.jhu.pacaya.sch.util.TestUtils.testEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;

import edu.jhu.prim.tuple.Pair; 

public class IntDiGraphTest {

    @Test
    public void testSimpleGraphWithStart() {
        IntDiGraph g = IntDiGraph.simpleGraphWithStart();

        // adding duplicate edges shouldn't change anything
        g.addEdge(2, 3);
        g.addEdge(4, 0);

        assertEquals(Arrays.asList(0,1,2,3,4), g.getNodes());
        assertEquals(Arrays.asList(
                edge(0,  1),
                edge(0,  2),
                edge(1,  3),
                edge(2,  3),
                edge(3,  0),
                edge(4, 0),
                edge(4, 3)
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
}
