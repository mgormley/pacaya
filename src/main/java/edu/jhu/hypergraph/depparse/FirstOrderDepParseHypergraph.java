package edu.jhu.hypergraph.depparse;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hypergraph.BasicHypernode;
import edu.jhu.hypergraph.Hyperalgo;
import edu.jhu.hypergraph.Hyperedge;
import edu.jhu.hypergraph.Hypergraph;
import edu.jhu.hypergraph.Hypernode;
import edu.jhu.hypergraph.Hyperpotential;
import edu.jhu.hypergraph.WeightedHyperedge;
import edu.jhu.nlp.MutableInt;
import edu.jhu.util.semiring.Semiring;

public class FirstOrderDepParseHypergraph implements Hypergraph {

    public static class PCBasicHypernode extends BasicHypernode {

        private int p;
        private int c;
        
        public PCBasicHypernode(String label, int id, int p, int c) {
            super(label, id);
            this.p = p;
            this.c = c;
        }
        

        public PCBasicHypernode(String label, int id) {
            super(label, id);
            this.p = -2;
            this.c = -2;
        }

        public int getP() {
            return p;
        }

        public int getC() {
            return c;
        }
        
        public void setPC(int p, int c) {
            this.p = p;
            this.c = c;
        }
        
    }
    
    private static final Logger log = Logger.getLogger(FirstOrderDepParseHypergraph.class);

    private static final int NOT_INITIALIZED = -1;
    private static int LEFT = 0;
    private static int RIGHT = 1;
    private static int INCOMPLETE = 0;
    private static int COMPLETE = 1;
    
    // Number of words in the sentence.
    private int n;
    private Hypernode root;
    private Hypernode[] wallChart;
    private Hypernode[][][][] childChart;   
    private List<Hypernode> nodes;
    // Number of hyperedges.
    private int numEdges = NOT_INITIALIZED;
    // Dependency arc scores.
    private double[] rootScores;
    private double[][] childScores;
    private Semiring semiring;
    
    public FirstOrderDepParseHypergraph(double[] rootScores, double[][] scores, Semiring semiring) {
        this.rootScores = rootScores;
        this.childScores = scores;
        this.semiring = semiring;
        this.n = scores.length;
        this.nodes = new ArrayList<Hypernode>();    
        this.wallChart = new Hypernode[n];
        this.childChart = new Hypernode[n][n][2][2];
        
        // Create all hypernodes.
        createHypernodes();
    }

    private void createHypernodes() {
        int id=0;
        for (int s=0; s<n; s++) {
            for (int d=0; d<2; d++) {
                String label = null;
                if (log.isTraceEnabled()) {
                    label = String.format("chart[%d][%d][%d][%d]", s,s,d,COMPLETE);
                }
                BasicHypernode node = new BasicHypernode(label, id++);
                nodes.add(node);
                childChart[s][s][d][COMPLETE] = node;
            }
        }
        for (int width = 1; width < n; width++) {
            for (int s = 0; s < n - width; s++) {
                int t = s + width;                
                for (int d=0; d<2; d++) {
                    for (int c=0; c<2; c++) {
                        String label = null;
                        if (log.isTraceEnabled()) {
                            label = String.format("chart[%d][%d][%d][%d]", s,t,d,c);
                        }
                        Hypernode node;
                        if (c == INCOMPLETE) {
                            int parent = (d == LEFT) ? t : s;
                            int child  = (d == LEFT) ? s : t;
                            node = new PCBasicHypernode(label, id++, parent, child);
                        } else {
                            node = new BasicHypernode(label, id++);
                        }
                            
                        nodes.add(node);
                        childChart[s][t][d][c] = node;
                    }
                }
            }
        }
        for (int s=0; s<n; s++) {
            String label = null;
            if (log.isTraceEnabled()) {
                label = String.format("wall[%d]", s);
            }
            PCBasicHypernode node = new PCBasicHypernode(label, id++, -1, s);
            nodes.add(node);
            wallChart[s] = node;
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
        // TODO: Fold this into the apply.
        final MutableInt count = new MutableInt(0);
        if (numEdges == NOT_INITIALIZED) {
            applyTopoSort(new HyperedgeFn() {                
                @Override
                public void apply(Hyperedge e) { count.increment(); }
            });
            numEdges = count.get();
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
        int id = 0;
        WeightedHyperedge e = new WeightedHyperedge(id, null);
        
        // Initialize.
        for (int s = 0; s < n; s++) {
            // inChart.scores[s][s][RIGHT][COMPLETE] = 0.0;
            // inChart.scores[s][s][LEFT][COMPLETE] = 0.0;
            for (int d=0; d<2; d++) {
                e.setHeadNode(childChart[s][s][d][COMPLETE]);
                e.setTailNodes();
                e.setWeight(semiring.one());
                e.setId(id++);
                fn.apply(e);
            }
        }
                
        // Parse.
        for (int width = 1; width < n; width++) {
            for (int s = 0; s < n - width; s++) {
                int t = s + width;
                
                // First create incomplete items.
                for (int r=s; r<t; r++) {
                    for (int d=0; d<2; d++) {                        
                        // double edgeScore = (d==LEFT) ? scores[t][s] : scores[s][t];
                        // double score = inChart.scores[s][r][RIGHT][COMPLETE] +
                        //               inChart.scores[r+1][t][LEFT][COMPLETE] +  
                        //               edgeScore;
                        // inChart.updateCell(s, t, d, INCOMPLETE, score, r);
                        e.setHeadNode(childChart[s][t][d][INCOMPLETE]);
                        e.setTailNodes(childChart[s][r][RIGHT][COMPLETE],
                                       childChart[r+1][t][LEFT][COMPLETE]);
                        e.setWeight((d==LEFT) ? childScores[t][s] : childScores[s][t]);
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
                    e.setHeadNode(childChart[s][t][d][COMPLETE]);
                    e.setTailNodes(childChart[s][r][d][COMPLETE],
                                   childChart[r][t][d][INCOMPLETE]);
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
                    e.setHeadNode(childChart[s][t][d][COMPLETE]);
                    e.setTailNodes(childChart[s][r][d][INCOMPLETE],
                                   childChart[r][t][d][COMPLETE]);
                    e.setWeight(semiring.one());
                    e.setId(id++);
                    fn.apply(e);
                }
            }
        }
        
        // Build goal constituents by combining left and right complete
        // constituents, on the left and right respectively. This corresponds to
        // left and right triangles. (Note: this is the opposite of how we
        // build an incomplete constituent.)
        for (int r=0; r<n; r++) {
            // double score = inChart.scores[0][r][LEFT][COMPLETE] +
            //                inChart.scores[r][n-1][RIGHT][COMPLETE] + 
            //                fracRoot[r];
            // inChart.updateGoalCell(r, score);
            e.setHeadNode(wallChart[r]);
            e.setTailNodes(childChart[0][r][LEFT][COMPLETE],
                           childChart[r][n-1][RIGHT][COMPLETE]);
            e.setWeight(rootScores[r]);
            e.setId(id++);
            fn.apply(e);
        }
        
        // Edges from wall to ROOT.
        for (int r=0; r<n; r++) {
            e.setHeadNode(root);
            e.setTailNodes(wallChart[r]);
            e.setWeight(semiring.one());
            e.setId(id++);
            fn.apply(e);
        }
        
        numEdges = id;
    }

    @Override
    public void applyRevTopoSort(final HyperedgeFn fn) {   
        int id = 0;
        WeightedHyperedge e = new WeightedHyperedge(id, "edge");

        // Edges from wall to ROOT.
        for (int r=0; r<n; r++) {
            e.setHeadNode(root);
            e.setTailNodes(wallChart[r]);
            e.setWeight(semiring.one());
            e.setId(id++);
            fn.apply(e);
        }
        
        // Build goal constituents by combining left and right complete
        // constituents, on the left and right respectively. This corresponds to
        // left and right triangles. (Note: this is the opposite of how we
        // build an incomplete constituent.)
        for (int r=0; r<n; r++) {
            // double score = inChart.scores[0][r][LEFT][COMPLETE] +
            //                inChart.scores[r][n-1][RIGHT][COMPLETE] + 
            //                fracRoot[r];
            // inChart.updateGoalCell(r, score);
            e.setHeadNode(wallChart[r]);
            e.setTailNodes(childChart[0][r][LEFT][COMPLETE],
                           childChart[r][n-1][RIGHT][COMPLETE]);
            e.setWeight(rootScores[r]);
            e.setId(id++);
            fn.apply(e);
        }
                
        // Parse.
        for (int width = n - 1; width >= 1; width--) {
            for (int s = 0; s < n - width; s++) {
                int t = s + width;
                
                // Create complete items.
                // -- Left side.
                for (int r=s; r<t; r++) {
                    final int d = LEFT;
                    // double score = inChart.scores[s][r][d][COMPLETE] +
                    //               inChart.scores[r][t][d][INCOMPLETE];
                    // inChart.updateCell(s, t, d, COMPLETE, score, r);
                    e.setHeadNode(childChart[s][t][d][COMPLETE]);
                    e.setTailNodes(childChart[s][r][d][COMPLETE],
                                   childChart[r][t][d][INCOMPLETE]);
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
                    e.setHeadNode(childChart[s][t][d][COMPLETE]);
                    e.setTailNodes(childChart[s][r][d][INCOMPLETE],
                                   childChart[r][t][d][COMPLETE]);
                    e.setWeight(semiring.one());
                    e.setId(id++);
                    fn.apply(e);
                }       

                // Create incomplete items.
                for (int r=s; r<t; r++) {
                    for (int d=0; d<2; d++) {                        
                        // double edgeScore = (d==LEFT) ? scores[t][s] : scores[s][t];
                        // double score = inChart.scores[s][r][RIGHT][COMPLETE] +
                        //               inChart.scores[r+1][t][LEFT][COMPLETE] +  
                        //               edgeScore;
                        // inChart.updateCell(s, t, d, INCOMPLETE, score, r);
                        e.setHeadNode(childChart[s][t][d][INCOMPLETE]);
                        e.setTailNodes(childChart[s][r][RIGHT][COMPLETE],
                                       childChart[r+1][t][LEFT][COMPLETE]);
                        e.setWeight((d==LEFT) ? childScores[t][s] : childScores[s][t]);
                        e.setId(id++);
                        fn.apply(e);
                    }
                }                
            }
        }
                
        // Initialize.
        for (int s = 0; s < n; s++) {
            // inChart.scores[s][s][RIGHT][COMPLETE] = 0.0;
            // inChart.scores[s][s][LEFT][COMPLETE] = 0.0;
            for (int d=0; d<2; d++) {
                e.setHeadNode(childChart[s][s][d][COMPLETE]);
                e.setTailNodes();
                e.setWeight(semiring.one());
                e.setId(id++);
                fn.apply(e);
            }
        }
        
        numEdges = id;        
    }

    public Hypernode[] getWallChart() {
        return wallChart;
    }

    public Hypernode[][][][] getChildChart() {
        return childChart;
    }
    
    public int getNumTokens() {
        return n;
    }
    
}
