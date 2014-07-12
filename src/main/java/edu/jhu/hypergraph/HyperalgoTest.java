package edu.jhu.hypergraph;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import edu.jhu.hypergraph.Hyperalgo.Scores;
import edu.jhu.hypergraph.MemHypergraph.MemHypernode;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.dist.Dirichlet;
import edu.jhu.util.semiring.LogSignAlgebra;
import edu.jhu.util.semiring.RealAlgebra;
import edu.jhu.util.semiring.Semiring;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;

public class HyperalgoTest {

    private Hyperpotential w = new Hyperpotential() {        
        @Override
        public double getScore(Hyperedge e, Semiring s) {
            if (s instanceof Algebra) { 
                return ((Algebra) s).fromReal(1);
            } else {
                throw new RuntimeException();
            }
        }
    };
    

    @Test
    public void testTopoSortNodes() {
        MemHypergraph graph = getSimpleGraph();        
        List<MemHypernode> memNodes = graph.getMemNodes();
        for (int i=0; i<3; i++) {
            Collections.shuffle(memNodes, new Random(i));
            List<MemHypernode> sort = MemHypergraph.topoSortNodes(memNodes);
            // valid sorts:
            List<String> valid = Arrays.asList(new String[] { 
                    "[7, 5, 3, 11, 8, 2, 9, 10, ROOT]",
                    "[3, 5, 7, 8, 11, 2, 9, 10, ROOT]",
                    "[3, 7, 8, 5, 11, 10, 2, 9, ROOT]",
                    "[5, 7, 3, 8, 11, 10, 9, 2, ROOT]",
                    "[7, 5, 11, 3, 10, 8, 9, 2, ROOT]",
                    "[7, 5, 11, 2, 3, 8, 9, 10, ROOT]",
                    "[3, 7, 8, 5, 11, 10, 9, 2, ROOT]",
                    "[5, 3, 7, 8, 11, 10, 2, 9, ROOT]",
                    "[5, 7, 11, 2, 3, 10, 8, 9, ROOT]",
                    "[3, 5, 7, 8, 11, 9, 10, 2, ROOT]", // seed = 0
                    "[5, 3, 7, 8, 11, 10, 9, 2, ROOT]", // seed = 1
                    "[7, 3, 8, 5, 11, 10, 9, 2, ROOT]", // seed = 2
            });
            System.out.println(sort);
            assertTrue(valid.contains(sort.toString()));
        }
    }
    
    @Test
    public void testSimpleGraph() {
        // NOTE: This graph yields marginals greater than one because of its cyclicity.
        MemHypergraph graph = getSimpleGraph();        

        Scores expected = new Scores();
        expected.beta = new double[]{24.0, 1.0, 1.0, 1.0, 2.0, 2.0, 2.0, 4.0, 3.0};
        expected.alpha = new double[]{1.0, 32.0, 26.0, 14.0, 26.0, 6.0, 12.0, 6.0, 8.0};
        expected.marginal = new double[]{1.0, 1.3333333333333333, 1.0833333333333333, 0.5833333333333334, 2.1666666666666665, 0.5, 1.0, 1.0, 1.0};
        expected.alphaAdj = null;
        expected.betaAdj = null;
        expected.weightAdj = null;
        
        expected = forwardBackwardCheck(graph, expected, new RealAlgebra(), null);
        forwardBackwardCheck(graph, expected, new LogSignAlgebra(), null);
    }
       
    private static MemHypergraph getSimpleGraph() {
        MemHypergraph graph = new MemHypergraph();
        MemHypernode root = graph.newRoot("ROOT");
        MemHypernode n7 = graph.newNode("7");
        MemHypernode n5 = graph.newNode("5");
        MemHypernode n3 = graph.newNode("3");
        MemHypernode n11 = graph.newNode("11");
        MemHypernode n8 = graph.newNode("8");
        MemHypernode n2 = graph.newNode("2");
        MemHypernode n9 = graph.newNode("9");
        MemHypernode n10 = graph.newNode("10");
        
        // Add an incoming edge to each node with no tail nodes.
        graph.newEdge(n7);
        graph.newEdge(n5);
        graph.newEdge(n3);
        
        // Directed edges between two nodes.
        graph.newEdge(n11, n7);
        graph.newEdge(n8, n7);
        graph.newEdge(n11, n5);
        graph.newEdge(n8, n3);
        graph.newEdge(n10, n3);
        graph.newEdge(n2, n11);
        graph.newEdge(n9, n11);
        graph.newEdge(n10, n11);
        graph.newEdge(n9, n8);
        // The only hyperedge.
        graph.newEdge(root, n2, n9, n10);
        
        return graph;
    }
    
    @Test
    public void testTinyGraph() {
        MemHypergraph graph = getTinyGraph();        

        Scores expected = new Scores();
        expected.beta = new double[]{1.0, 1.0, 1.0, 2.0, 1.0, 3.0};
        expected.alpha = new double[]{1.0, 3.0, 2.0, 1.0, 1.0, 1.0};
        expected.marginal = new double[]{0.3333333333333333, 1.0, 0.6666666666666666, 0.6666666666666666, 0.3333333333333333, 1.0};
        expected.alphaAdj = new double[]{0.3333333333333333, 0.3333333333333333, 0.3333333333333333, 1.9999999999999998, 1.0, 4.0};
        expected.betaAdj = new double[]{0, 0, 0, -0.6666666666666667, -0.6666666666666667, -1.0};
        expected.weightAdj = new double[]{0, 0, 0, 0, 0, 0, 0, 0};
        
        forwardBackwardCheck(graph, expected, new RealAlgebra(), null);
        forwardBackwardCheck(graph, expected, new LogSignAlgebra(), null);
    }
    
    private Scores forwardBackwardCheck(MemHypergraph graph, Scores expected, Algebra s, double[] marginalAdj) {
        Scores scores = new Scores();
        Hyperalgo.forward(graph, w, s, scores);
        scores.marginalAdj = new double[graph.getNodes().size()];
        if (marginalAdj == null) {
            DoubleArrays.fill(scores.marginalAdj, s.fromReal(1));
        } else {
            scores.marginalAdj = Algebras.getFromReal(marginalAdj, s);
        }
        Hyperalgo.backward(graph, w, s, scores);
        
        System.out.println("Nodes: " + graph.getNodes());
        printAndCheck("beta", expected.beta, scores.beta, s);
        printAndCheck("alpha", expected.alpha, scores.alpha, s);
        printAndCheck("marginal", expected.marginal, scores.marginal, s);
        printAndCheck("alphaAdj", expected.alphaAdj, scores.alphaAdj, s);
        printAndCheck("betaAdj", expected.betaAdj, scores.betaAdj, s);
        System.out.println("Edges: " + graph.getEdges());
        printAndCheck("weightAdj", expected.weightAdj, scores.weightAdj, s);        
        return scores;
    }

    private void printAndCheck(String name, double[] expectedReals, double[] actual, Algebra s) {
        double[] actualReals = Algebras.getToReal(actual, s);
        System.out.println(name + ": " + Arrays.toString(actualReals));
        if (expectedReals == null) {
            System.out.println("WARN: skipping check of " + name);
            return;
        }
        JUnitUtils.assertArrayEquals(expectedReals, actualReals, 1e-13);
    }

    private static MemHypergraph getTinyGraph() {
        MemHypergraph graph = new MemHypergraph();
        // 1   2  3
        //  \/  \/
        //  |  /|
        //  | / |
        //  4   5
        //  |   /
        //  |  /
        //  ROOT
        MemHypernode n1 = graph.newNode("1");
        MemHypernode n2 = graph.newNode("2");
        MemHypernode n3 = graph.newNode("3");
        MemHypernode n4 = graph.newNode("4");
        MemHypernode n5 = graph.newNode("5");
        MemHypernode root = graph.newRoot("ROOT");
        
        // Add an incoming edge to each node with no tail nodes.
        graph.newEdge(n1);
        graph.newEdge(n2);
        graph.newEdge(n3);
        
        // Directed edges between two nodes.
        graph.newEdge(n4, n1, n2);
        graph.newEdge(n4, n2, n3);
        graph.newEdge(n5, n2, n3);
        graph.newEdge(root, n4);
        graph.newEdge(root, n5);
        
        return graph;
    }
    

    @Test
    public void testCkyGraph() {
        MemHypergraph graph = getCkyGraph();        

        Scores expected = new Scores();
        expected.beta = null;
        expected.alpha = null;
        expected.marginal = new double[]{1.0, 1.0, 1.0, 1.0, 0.25, 0.75, 0.25, 0.75, 0.25, 0.5, 0.25, 0.25, 0.75, 1.0};
        expected.alphaAdj = null;
        expected.betaAdj = null;
        expected.weightAdj = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        
        expected = forwardBackwardCheck(graph, expected, new RealAlgebra(), null);
        forwardBackwardCheck(graph, expected, new LogSignAlgebra(), null);
        
        // Check that both semirings get the same answers when using random marginal adjoints.
        expected = new Scores();
        double[] marginalAdj = new double[graph.getNodes().size()];
        Arrays.fill(marginalAdj, 1.0);
        marginalAdj = Dirichlet.staticDraw(marginalAdj);
        expected = forwardBackwardCheck(graph, expected, new RealAlgebra(), marginalAdj);
        forwardBackwardCheck(graph, expected, new LogSignAlgebra(), marginalAdj);
    }
            
    private static MemHypergraph getCkyGraph() {
        MemHypergraph graph = new MemHypergraph();
        // "the old boat"
        MemHypernode the = graph.newNode("the");
        MemHypernode old = graph.newNode("old");
        MemHypernode boat = graph.newNode("boat");    
        // Preterminals
        MemHypernode det01 = graph.newNode("det01");
        MemHypernode adj12 = graph.newNode("adj12");
        MemHypernode n12 = graph.newNode("n12");
        MemHypernode v23 = graph.newNode("v23");
        MemHypernode n23 = graph.newNode("n23");
        // Nonterminals width 2
        MemHypernode np02 = graph.newNode("np02");
        MemHypernode np13 = graph.newNode("np13"); // two ways
        MemHypernode np13extra = graph.newNode("np13extra");
   
        // Nonterminals width 3
        MemHypernode s03 = graph.newNode("s03");
        MemHypernode np03 = graph.newNode("np03"); // two ways

        MemHypernode root = graph.newRoot("ROOT"); // three ways

        
        // Add an incoming edge to each node with no tail nodes.
        graph.newEdge(the);
        graph.newEdge(old);
        graph.newEdge(boat);
        
        // Directed edges between two nodes.
        graph.newEdge(det01, the);
        graph.newEdge(adj12, old);
        graph.newEdge(n12, old);
        graph.newEdge(v23, boat);
        graph.newEdge(n23, boat);
        
        graph.newEdge(np02, det01, n12);
        graph.newEdge(np13, adj12, n23);
        graph.newEdge(np13, n12, n23);
        graph.newEdge(np13extra, n12, n23);
        
        graph.newEdge(s03, np02, v23);
        graph.newEdge(np03, det01, np13);
        graph.newEdge(np03, det01, np13extra);
        
        graph.newEdge(root, s03);
        graph.newEdge(root, np03);
        
        return graph;
    }
    
}
