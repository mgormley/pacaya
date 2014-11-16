package edu.jhu.hypergraph.depparse;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hypergraph.BasicHypernode;
import edu.jhu.hypergraph.Hyperedge;
import edu.jhu.hypergraph.Hypergraph;
import edu.jhu.hypergraph.Hypernode;
import edu.jhu.hypergraph.Hyperpotential;
import edu.jhu.hypergraph.WeightedHyperedge;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.util.semiring.Semiring;

/**
 * Hypergraph for single-root or multi-root first-order projective dependency parsing.
 * This follows the Eisner (2000) algorithm.
 * 
 * @author mgormley
 */
public class O1DpHypergraph implements Hypergraph {
    
    private static final Logger log = Logger.getLogger(O1DpHypergraph.class);

    private static final int NOT_INITIALIZED = -1;
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int INCOMPLETE = 0;
    private static final int COMPLETE = 1;
    
    // Number of words in the sentence plus one.
    private int nplus;
    private Hypernode root;
    private Hypernode[][][][] chart;   
    private List<Hypernode> nodes;
    // Number of hyperedges.
    private int numEdges = NOT_INITIALIZED;
    // Dependency arc scores.
    private double[][] scores;
    private Semiring semiring;
    private final boolean singleRoot;
    
    public O1DpHypergraph(double[] rootScores, double[][] childScores, Semiring semiring, boolean singleRoot) {
        this.scores = EdgeScores.combine(rootScores, childScores);
        this.semiring = semiring;
        this.singleRoot = singleRoot;
        this.nplus = scores.length;
        this.nodes = new ArrayList<Hypernode>();    
        this.chart = new Hypernode[nplus][nplus][2][2];
        
        // Create all hypernodes.
        createHypernodes();
    }

    private void createHypernodes() {
        int id=0;
        for (int s=0; s<nplus; s++) {
            for (int d=0; d<2; d++) {
                String label = null;
                if (log.isTraceEnabled()) {
                    label = String.format("chart[%d][%d][%d][%d]", s,s,d,COMPLETE);
                }
                BasicHypernode node = new BasicHypernode(label, id++);
                nodes.add(node);
                chart[s][s][d][COMPLETE] = node;
            }
        }
        for (int width = 1; width < nplus; width++) {
            for (int s = 0; s < nplus - width; s++) {
                int t = s + width;                
                for (int d=0; d<2; d++) {
                    for (int c=0; c<2; c++) {
                        String label = null;
                        if (log.isTraceEnabled()) {
                            label = String.format("chart[%d][%d][%d][%d]", s,t,d,c);
                        }
                        Hypernode node;
                        // Subtract one to get the parents array indexing.
                        int parent = (d == LEFT) ? t-1 : s-1;
                        int child  = (d == LEFT) ? s-1 : t-1;
                        if (c == INCOMPLETE && child != -1) {
                            node = new PCBasicHypernode(label, id++, parent, child);
                        } else {
                            node = new BasicHypernode(label, id++);
                        }
                            
                        nodes.add(node);
                        chart[s][t][d][c] = node;
                    }
                }
            }
        }
        this.root = new BasicHypernode("ROOT", id++);
        nodes.add(root);
    }
    
    @Override
    public Hypernode getRoot() {
        return root;
    }

    @Override
    public List<Hypernode> getNodes() {
        return nodes;    
    }

    @Override
    public int getNumEdges() {
        if (numEdges == NOT_INITIALIZED) {
            // numEdges is initialized by any call to applyTopoSort().
            applyTopoSort(new HyperedgeFn() {                
                @Override
                public void apply(Hyperedge e) { }
            });
            assert numEdges != NOT_INITIALIZED;
        }
        return numEdges;
    }
    
    public Hyperpotential getPotentials() {
        return new Hyperpotential() {            
            @Override
            public double getScore(Hyperedge e, Semiring s) {
                return ((WeightedHyperedge)e).getWeight();
            }
        };
    }

    @Override
    public void applyTopoSort(final HyperedgeFn fn) {
        final int startIdx = singleRoot ? 1 : 0;         

        int id = 0;
        WeightedHyperedge e = new WeightedHyperedge(id, null);
        
        // Initialize.
        for (int s = 0; s < nplus; s++) {
            // inChart.scores[s][s][RIGHT][COMPLETE] = 0.0;
            // inChart.scores[s][s][LEFT][COMPLETE] = 0.0;
            for (int d=0; d<2; d++) {
                e.setHeadNode(chart[s][s][d][COMPLETE]);
                e.setTailNodes();
                e.setWeight(semiring.one());
                e.setId(id++);
                fn.apply(e);
            }
        }
        
        // Parse.
        for (int width = 1; width < nplus; width++) {
            for (int s = startIdx; s < nplus - width; s++) {
                int t = s + width;
                
                // First create incomplete items.
                for (int r=s; r<t; r++) {
                    for (int d=0; d<2; d++) {                        
                        // double edgeScore = (d==LEFT) ? scores[t][s] : scores[s][t];
                        // double score = inChart.scores[s][r][RIGHT][COMPLETE] +
                        //               inChart.scores[r+1][t][LEFT][COMPLETE] +  
                        //               edgeScore;
                        // inChart.updateCell(s, t, d, INCOMPLETE, score, r);
                        e.setHeadNode(chart[s][t][d][INCOMPLETE]);
                        e.setTailNodes(chart[s][r][RIGHT][COMPLETE],
                                       chart[r+1][t][LEFT][COMPLETE]);
                        e.setWeight((d==LEFT) ? scores[t][s] : scores[s][t]);
                        e.setId(id++);
                        fn.apply(e);
                    }
                }
                
                // Second create complete items.
                // -- Left side.
                for (int r=s; r<t; r++) {
                    final int d = LEFT;
                    // double score = inChart.scores[s][r][d][COMPLETE] +
                    //               inChart.scores[r][t][d][INCOMPLETE];
                    // inChart.updateCell(s, t, d, COMPLETE, score, r);
                    e.setHeadNode(chart[s][t][d][COMPLETE]);
                    e.setTailNodes(chart[s][r][d][COMPLETE],
                                   chart[r][t][d][INCOMPLETE]);
                    e.setWeight(semiring.one());
                    e.setId(id++);
                    fn.apply(e);
                }
                // -- Right side.
                for (int r=s+1; r<=t; r++) {
                    final int d = RIGHT;
                    // double score = inChart.scores[s][r][d][INCOMPLETE] +
                    //                inChart.scores[r][t][d][COMPLETE];
                    // inChart.updateCell(s, t, d, COMPLETE, score, r);
                    e.setHeadNode(chart[s][t][d][COMPLETE]);
                    e.setTailNodes(chart[s][r][d][INCOMPLETE],
                                   chart[r][t][d][COMPLETE]);
                    e.setWeight(semiring.one());
                    e.setId(id++);
                    fn.apply(e);
                }
            }
        }
        
        if (singleRoot) {
            // Build goal constituents by combining left and right complete
            // constituents, on the left and right respectively. This corresponds to
            // left and right triangles. (Note: this is the opposite of how we
            // build an incomplete constituent.)
            for (int r=1; r<nplus; r++) {
                //    double score = inChart.scores[1][r][LEFT][COMPLETE] +
                //                   inChart.scores[r][nplus-1][RIGHT][COMPLETE] + 
                //                   scores[0][r];
                //    inChart.updateCell(0, r, RIGHT, INCOMPLETE, score, r);                
                e.setHeadNode(chart[0][r][RIGHT][INCOMPLETE]);
                e.setTailNodes(chart[1][r][LEFT][COMPLETE],
                               chart[r][nplus-1][RIGHT][COMPLETE]);
                e.setWeight(scores[0][r]);
                e.setId(id++);
                fn.apply(e);
                
                //    inChart.updateCell(0, nplus-1, RIGHT, COMPLETE, score, r);
                e.setHeadNode(chart[0][nplus-1][RIGHT][COMPLETE]);
                e.setTailNodes(chart[0][r][RIGHT][INCOMPLETE]);
                e.setWeight(semiring.one());
                e.setId(id++);
                fn.apply(e);
            }
        }
        
        // Single hyperedge from wall to ROOT.
        //
        // Build the goal constituent by combining the left and right complete
        // constituents. This corresponds to left and right triangles. 
        // (Note: this is the opposite of how we build an incomplete constituent.)
        // 
        // Here r = 0.
        e.setHeadNode(root);
        e.setTailNodes(chart[0][0][LEFT][COMPLETE],
                       chart[0][nplus-1][RIGHT][COMPLETE]);
        e.setWeight(semiring.one());
        e.setId(id++);
        fn.apply(e);
        
        numEdges = id;
    }

    @Override
    public void applyRevTopoSort(final HyperedgeFn fn) {
        final int startIdx = singleRoot ? 1 : 0;         
        int id = getNumEdges()-1;
        WeightedHyperedge e = new WeightedHyperedge(id, null);

        // Single hyperedge from wall to ROOT.
        e.setHeadNode(root);
        e.setTailNodes(chart[0][0][LEFT][COMPLETE],
                       chart[0][nplus-1][RIGHT][COMPLETE]);
        e.setWeight(semiring.one());
        e.setId(id--);
        fn.apply(e);
                
        if (singleRoot) {
            // Build goal constituents by combining left and right complete
            // constituents, on the left and right respectively. This corresponds to
            // left and right triangles. (Note: this is the opposite of how we
            // build an incomplete constituent.)
            for (int r=nplus-1; r>=1; r--) {
                //    inChart.updateCell(0, nplus-1, RIGHT, COMPLETE, score, r);
                e.setHeadNode(chart[0][nplus-1][RIGHT][COMPLETE]);
                e.setTailNodes(chart[0][r][RIGHT][INCOMPLETE]);
                e.setWeight(semiring.one());
                e.setId(id--);
                fn.apply(e);
                
                //    double score = inChart.scores[1][r][LEFT][COMPLETE] +
                //                   inChart.scores[r][nplus-1][RIGHT][COMPLETE] + 
                //                   scores[0][r];
                //    inChart.updateCell(0, r, RIGHT, INCOMPLETE, score, r);                
                e.setHeadNode(chart[0][r][RIGHT][INCOMPLETE]);
                e.setTailNodes(chart[1][r][LEFT][COMPLETE],
                               chart[r][nplus-1][RIGHT][COMPLETE]);
                e.setWeight(scores[0][r]);
                e.setId(id--);
                fn.apply(e);                
            }
        }
        
        // Parse.
        for (int width = nplus - 1; width >= 1; width--) {
            for (int s = nplus - width - 1; s >= startIdx; s--) {
                int t = s + width;
                
                // Create complete items.
                // -- Right side.
                for (int r=t; r>=s+1; r--) {
                    final int d = RIGHT;
                    // double score = inChart.scores[s][r][d][INCOMPLETE] +
                    //                inChart.scores[r][t][d][COMPLETE];
                    // inChart.updateCell(s, t, d, COMPLETE, score, r);
                    e.setHeadNode(chart[s][t][d][COMPLETE]);
                    e.setTailNodes(chart[s][r][d][INCOMPLETE],
                                   chart[r][t][d][COMPLETE]);
                    e.setWeight(semiring.one());
                    e.setId(id--);
                    fn.apply(e);
                }       
                // -- Left side.
                for (int r=t-1; r>=s; r--) {
                    final int d = LEFT;
                    // double score = inChart.scores[s][r][d][COMPLETE] +
                    //               inChart.scores[r][t][d][INCOMPLETE];
                    // inChart.updateCell(s, t, d, COMPLETE, score, r);
                    e.setHeadNode(chart[s][t][d][COMPLETE]);
                    e.setTailNodes(chart[s][r][d][COMPLETE],
                                   chart[r][t][d][INCOMPLETE]);
                    e.setWeight(semiring.one());
                    e.setId(id--);
                    fn.apply(e);
                }

                // Create incomplete items.
                for (int r=t-1; r>=s; r--) {
                    for (int d=1; d>=0; d--) {
                        // double edgeScore = (d==LEFT) ? scores[t][s] : scores[s][t];
                        // double score = inChart.scores[s][r][RIGHT][COMPLETE] +
                        //               inChart.scores[r+1][t][LEFT][COMPLETE] +  
                        //               edgeScore;
                        // inChart.updateCell(s, t, d, INCOMPLETE, score, r);
                        e.setHeadNode(chart[s][t][d][INCOMPLETE]);
                        e.setTailNodes(chart[s][r][RIGHT][COMPLETE],
                                       chart[r+1][t][LEFT][COMPLETE]);
                        e.setWeight((d==LEFT) ? scores[t][s] : scores[s][t]);
                        e.setId(id--);
                        fn.apply(e);
                    }
                }                
            }
        }
                
        // Initialize.
        for (int s = nplus-1; s >= 0; s--) {
            // inChart.scores[s][s][RIGHT][COMPLETE] = 0.0;
            // inChart.scores[s][s][LEFT][COMPLETE] = 0.0;
            for (int d=1; d>=0; d--) {
                e.setHeadNode(chart[s][s][d][COMPLETE]);
                e.setTailNodes();
                e.setWeight(semiring.one());
                e.setId(id--);
                fn.apply(e);
            }
        }
        
        assert id == -1 : "id="+id;
    }

    public Hypernode[][][][] getChart() {
        return chart;
    }
    
    public int getNumTokens() {
        return nplus-1;
    }
    
}
