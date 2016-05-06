package edu.jhu.pacaya.sch.graph;

import static edu.jhu.pacaya.sch.graph.DiEdge.edge;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.InputMismatchException;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.Test;

import edu.jhu.pacaya.sch.util.TestUtils; 

public class WeightedIntDiGraphTest {
    // tolerance for double comparison
    private static double tol = 1E-9; 
    
    @Test
    public void testSimpleGraphWithStart() {
        WeightedIntDiGraph g0 = WeightedIntDiGraph.fromUnweighted(new IntDiGraph());
        assertEquals(-1, g0.max());
        WeightedIntDiGraph g = WeightedIntDiGraph.fromUnweighted(IntDiGraph.simpleGraphWithStart());
        assertEquals(4, g.max());
        assertTrue(TestUtils.checkThrows(() -> g.addNode(-1), IndexOutOfBoundsException.class));
        assertTrue(TestUtils.checkThrows(() -> g.setWeight(5, 0, .5), IndexOutOfBoundsException.class));
        assertTrue(TestUtils.checkThrows(() -> g.getWeight(5, 0), IndexOutOfBoundsException.class));
        assertTrue(g.getEdges().contains(edge(2, 3)));
        assertEquals(0.0, g.getWeight(2,  3), tol);
        assertEquals(0.0, g.getWeight(4,  0), tol);
        g.setWeight(4,  0, .3);
        assertEquals(.3, g.getWeight(4,  0), tol);
        
        // adding duplicate edges shouldn't change anything
        g.addEdge(2, 3);
        g.addEdge(4, 0);
        g.addEdge(4, 0, .5);
        assertEquals(.3, g.getWeight(4,  0), tol);

        assertEquals(Arrays.asList(0,1,2,3,4), g.getNodes());
        assertEquals(Arrays.asList(
                edge(0, 1),
                edge(0, 2),
                edge(1, 3),
                edge(2, 3),
                edge(3, 0),
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
        assertEquals(4, g.max());
    }

    
    @Test
    public void testOnes() {
        assertArrayEquals(
                new double[] { 1, 1, 1, 1, 1},
                WeightedIntDiGraph.ones(5),
                tol
        );
    }

    @Test
    public void testMatrix() {
        /*
        edge(0, 1),
        edge(0, 2),
        edge(1, 3),
        edge(2, 3),
        edge(3, 0),
        edge(4, 0),
        edge(4, 3),
        edge(6, 8),
        edge(9, 3),
         */
        // first the 0 case
        WeightedIntDiGraph g = WeightedIntDiGraph.fromUnweighted(IntDiGraph.simpleGraphWithStart());
        g.addEdge(9, 3);
        assertEquals(9, g.max());
        g.addEdge(6, 8);
        RealMatrix mExpected = new Array2DRowRealMatrix(10, 10);
        RealMatrix m1 = g.toMatrix();
        assertEquals(mExpected, m1);

        // now have entries default to 1
        WeightedIntDiGraph g2 = WeightedIntDiGraph.fromUnweighted(IntDiGraph.simpleGraphWithStart(), Void -> 1.0);
        g2.addEdge(9, 3);
        assertEquals(9, g.max());
        g2.addEdge(6, 8);
        // now add again with different weight (should do nothing)
        g2.addEdge(9, 3, 9.0);
        // just set the weight for the other
        g2.setWeight(6, 8, 7.0);
        // and add another edge
        g2.addEdge(5, 4, 6.0);

        mExpected = new Array2DRowRealMatrix(new double[][] {
            { 0, 1, 1, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 1, 0, 0, 1, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 6, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 7, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 }
        });
        RealMatrix m2 = g2.toMatrix();
        assertEquals(mExpected, m2);

    }    
    
    @Test
    public void testSpectralNorm() {
        RealMatrix ones = new Array2DRowRealMatrix(3, 3).scalarAdd(1.0);
        assertEquals(3.0, WeightedIntDiGraph.computeSpectralNorm(ones), tol);

        RealMatrix m0 = new Array2DRowRealMatrix(new double[][] {
            { 0.3, 0.0, 0.3 },
            { 0.1, 0.6, 0.7 },
            { 0.0, 0.4, 0.2 },
        });
        assertEquals(0.981051, WeightedIntDiGraph.computeSpectralNorm(m0), 1E-5);

        RealMatrix m1 = new Array2DRowRealMatrix(new double[][] {
            { 0.3, 0.0, 0.9 },
            { 1.2, 3.0, 2.0 },
            { 0.0, 1.0, 3.0 },
        });
        assertEquals(4.502332, WeightedIntDiGraph.computeSpectralNorm(m1), 1E-5);

        RealMatrix m2 = new Array2DRowRealMatrix(new double[][] {
            { -3, 2, 1 },
            { 7, -2, -2 },
            { 3, -2, -9 },
        });
        assertEquals(10.600341, WeightedIntDiGraph.computeSpectralNorm(m2), 1E-5);

    }

    @Test
    public void testSumWalks() {
        RealMatrix m = new Array2DRowRealMatrix(new double[][] {
            { 0.3, 0.0, 0.3 },
            { 0.1, 0.6, 0.7 },
            { 0.0, 0.4, 0.2 },
        });
        RealMatrix sum = MatrixUtils.createRealIdentityMatrix(3);
        for (int i = 0; i < 2000; i++) {
            sum = sum.add(m.power(i + 1));
        }
        RealVector ones = new ArrayRealVector(3, 1.0);
        // this is how I'm computing the reference (different from the methods implementation)
        double expectedSumWalks = sum.preMultiply(ones).dotProduct(ones);
        RealVector s = new ArrayRealVector(new double[] {1, 2, 3});
        RealVector t = new ArrayRealVector(new double[] {5, 7, 4});
        assertEquals(127.49999999999903, expectedSumWalks, tol);
        assertEquals(127.49999999999903, WeightedIntDiGraph.sumWalks(m, null, null), tol);
        assertEquals(127.49999999999903, WeightedIntDiGraph.sumWalks(m, null, ones), tol);
        assertEquals(127.49999999999903, WeightedIntDiGraph.sumWalks(m, ones, null), tol);
        assertTrue(TestUtils.checkThrows(() -> {
            WeightedIntDiGraph.sumWalks(
                new Array2DRowRealMatrix(
                        new double[][] {
                            { 0.3, 0.0, 0.3, 0 },
                            { 0.1, 0.6, 0.7, 0 },
                            { 0.0, 0.4, 0.2, 0 }}),
                null,
                null);
            }, InputMismatchException.class));                
        assertEquals(699.9999999999948, WeightedIntDiGraph.sumWalks(m, null, t), tol);
        assertEquals(274.99999999999795, WeightedIntDiGraph.sumWalks(m, s, null), tol);
        assertEquals(1509.9999999999886, WeightedIntDiGraph.sumWalks(m, s, t), tol);

    }    
    
}
