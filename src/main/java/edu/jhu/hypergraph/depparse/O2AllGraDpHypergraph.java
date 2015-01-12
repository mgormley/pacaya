package edu.jhu.hypergraph.depparse;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.hypergraph.BasicHypernode;
import edu.jhu.hypergraph.Hyperedge;
import edu.jhu.hypergraph.Hypergraph;
import edu.jhu.hypergraph.Hypernode;
import edu.jhu.hypergraph.Hyperpotential;
import edu.jhu.hypergraph.WeightedHyperedge;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Semiring;

/**
 * Hypergraph for second-order dependency parsing with all grandchild factors. This algorithm comes
 * from Koo & Collins (2010).
 * 
 * @author mgormley
 */
public class O2AllGraDpHypergraph implements Hypergraph {

    private static final Logger log = LoggerFactory.getLogger(O1DpHypergraph.class);

    private static final int NOT_INITIALIZED = -1;
    public static final int NIL = 0;
    public static final int INCOMPLETE = 0;
    public static final int COMPLETE = 1;
    
    // Number of words in the sentence plus one.
    private int nplus;
    private Hypernode root;
    // Indexed by start, end, grandparent, completedness.
    private Hypernode[][][][] chart;
    private List<Hypernode> nodes;
    // Number of hyperedges.
    private int numEdges = NOT_INITIALIZED;    
    private DependencyScorer scorer;
    private Algebra a;
    private boolean singleRoot;
    
    public interface DependencyScorer {
        double getScore(int p, int c, int g);
        int getNumTokens();
    }

    public O2AllGraDpHypergraph(DependencyScorer scorer, Algebra a, boolean singleRoot) {
        super();
        this.scorer = scorer;
        this.a = a;
        this.singleRoot = singleRoot;
        this.nplus = scorer.getNumTokens()+1;
        this.nodes = new ArrayList<Hypernode>();    
        this.chart = new Hypernode[nplus][nplus][nplus][2];
        
        // Create all hypernodes.
        createHypernodes();
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

    private void createHypernodes() {
        int id=0;
        for (int s=0; s<nplus; s++) {
            for (int g=0; g<nplus; g++) {
                if (g == s && s != 0) { continue; }
                String label = log.isTraceEnabled() ? String.format("chart[%d][%d][%d][%d]", s,s,g,COMPLETE) : null;
                BasicHypernode node = new BasicHypernode(label, id++);
                nodes.add(node);
                chart[s][s][g][COMPLETE] = node;
            }
        }
        for (int width = 1; width < nplus; width++) {
            for (int i = 0; i < nplus - width; i++) {
                int j = i + width;
                for (int g=0; g<nplus; g++) {
                    if (i <= g && g <= j && !(i==0 && g==NIL)) { continue; }
                    for (int c=0; c<2; c++) {
                        String label;
                        // Right.
                        label = log.isTraceEnabled() ? String.format("chart[%d][%d][%d][%d]", i,j,g,c) : null;
                        chart[i][j][g][c] = new BasicHypernode(label, id++);
                        nodes.add(chart[i][j][g][c]);
                        // Left.
                        label = log.isTraceEnabled() ? String.format("chart[%d][%d][%d][%d]", j,i,g,c) : null;
                        chart[j][i][g][c] = new BasicHypernode(label, id++);
                        nodes.add(chart[j][i][g][c]);
                    }
                }
            }
        }
        this.root = new BasicHypernode("ROOT", id++);
        nodes.add(root);
    }
    

    @Override
    public void applyTopoSort(final HyperedgeFn fn) {
        final int startIdx = singleRoot ? 1 : 0;         
        int id = 0;
        WeightedHyperedge e = new WeightedHyperedge(id, null);
        
        // Initialize.
        for (int s = 0; s < nplus; s++) {
            for (int g=0; g<nplus; g++) {
                if (g == s && s != 0) { continue; }
                e.setHeadNode(chart[s][s][g][COMPLETE]);
                e.setTailNodes();
                e.setWeight(a.one());
                e.setId(id++);
                fn.apply(e);
            }
        }
        
        // Parse.
        for (int width = 1; width < nplus; width++) {
            for (int i = startIdx; i < nplus - width; i++) {
                int j = i + width;
                for (int g = 0; g < nplus; g++) {
                    if (i <= g && g <= j && !(i==0 && g==NIL)) { continue; }
                    // Incomplete items.
                    double sij = scorer.getScore(i, j, g);
                    double sji = scorer.getScore(j, i, g);
                    for (int r=i; r<j; r++) {
                        // Right.
                        e.setHeadNode(chart[i][j][g][INCOMPLETE]);
                        e.setTailNodes(chart[i][r][g][COMPLETE],
                                       chart[j][r+1][i][COMPLETE]);
                        e.setWeight(sij);
                        e.setId(id++);
                        fn.apply(e);
                        // Left.
                        e.setHeadNode(chart[j][i][g][INCOMPLETE]);
                        e.setTailNodes(chart[j][r+1][g][COMPLETE],
                                       chart[i][r][j][COMPLETE]);
                        e.setWeight(sji);
                        e.setId(id++);
                        fn.apply(e);
                    }
                    
                    // Complete items.
                    for (int r=i+1; r<=j; r++) {
                        // Right
                        e.setHeadNode(chart[i][j][g][COMPLETE]);
                        e.setTailNodes(chart[i][r][g][INCOMPLETE],
                                       chart[r][j][i][COMPLETE]);
                        e.setWeight(a.one());
                        e.setId(id++);
                        fn.apply(e);
                    }
                    for (int r=i; r<j; r++) {
                        // Left
                        e.setHeadNode(chart[j][i][g][COMPLETE]);
                        e.setTailNodes(chart[j][r][g][INCOMPLETE],
                                       chart[r][i][j][COMPLETE]);
                        e.setWeight(a.one());
                        e.setId(id++);
                        fn.apply(e);
                    }
                }
            }
        }
        
        if (singleRoot) {
            // Build goal constituents by combining left and right complete
            // g-spans, on the left and right respectively. This corresponds to
            // left and right triangles. (Note: this is the opposite of how we
            // build an incomplete constituent.)
            for (int r=1; r<nplus; r++) {  
                // Single-root.
                e.setHeadNode(chart[0][r][NIL][INCOMPLETE]);
                e.setTailNodes(chart[r][1][0][COMPLETE],
                               chart[r][nplus-1][0][COMPLETE]);
                e.setWeight(scorer.getScore(0, r, NIL));
                e.setId(id++);
                fn.apply(e);
                
                // Finalize.
                e.setHeadNode(chart[0][nplus-1][NIL][COMPLETE]);
                e.setTailNodes(chart[0][r][NIL][INCOMPLETE]);
                e.setWeight(a.one());
                e.setId(id++);
                fn.apply(e);
            }
        }
        
        // Single hyperedge from wall to ROOT.
        //
        // Here g = nil.
        e.setHeadNode(root);
        e.setTailNodes(//chart[0][0][NIL][COMPLETE],
                       chart[0][nplus-1][NIL][COMPLETE]);
        e.setWeight(a.one());
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
        e.setTailNodes(//chart[0][0][NIL][COMPLETE],
                       chart[0][nplus-1][NIL][COMPLETE]);
        e.setWeight(a.one());
        e.setId(id--);
        fn.apply(e);
        
        if (singleRoot) {
            // Build goal constituents by combining left and right complete
            // g-spans, on the left and right respectively. This corresponds to
            // left and right triangles. (Note: this is the opposite of how we
            // build an incomplete constituent.)
            for (int r=nplus-1; r >= 1; r--) {
                // Finalize.
                e.setHeadNode(chart[0][nplus-1][NIL][COMPLETE]);
                e.setTailNodes(chart[0][r][NIL][INCOMPLETE]);
                e.setWeight(a.one());
                e.setId(id--);
                fn.apply(e);
                
                // Single-root.
                e.setHeadNode(chart[0][r][NIL][INCOMPLETE]);
                e.setTailNodes(chart[r][1][0][COMPLETE],
                               chart[r][nplus-1][0][COMPLETE]);
                e.setWeight(scorer.getScore(0, r, NIL));
                e.setId(id--);
                fn.apply(e);
            }
        }
        
        // Parse.
        for (int width = nplus - 1; width >= 1; width--) {
            for (int i = nplus - width - 1; i >= startIdx; i--) {
                int j = i + width;
                for (int g = nplus-1; g >= 0; g--) {
                    if (i <= g && g <= j && !(i==0 && g==NIL)) { continue; }
                                        
                    // Complete items.
                    for (int r=j-1; r>=i; r--) {
                        // Left
                        e.setHeadNode(chart[j][i][g][COMPLETE]);
                        e.setTailNodes(chart[j][r][g][INCOMPLETE],
                                       chart[r][i][j][COMPLETE]);
                        e.setWeight(a.one());
                        e.setId(id--);
                        fn.apply(e);
                    }
                    for (int r=j; r>=i+1; r--) {
                        // Right
                        e.setHeadNode(chart[i][j][g][COMPLETE]);
                        e.setTailNodes(chart[i][r][g][INCOMPLETE],
                                       chart[r][j][i][COMPLETE]);
                        e.setWeight(a.one());
                        e.setId(id--);
                        fn.apply(e);
                    }
                    
                    // Incomplete items.
                    double sij = scorer.getScore(i, j, g);
                    double sji = scorer.getScore(j, i, g);
                    for (int r=j-1; r>=i; r--) {
                        // Left.
                        e.setHeadNode(chart[j][i][g][INCOMPLETE]);
                        e.setTailNodes(chart[j][r+1][g][COMPLETE],
                                       chart[i][r][j][COMPLETE]);
                        e.setWeight(sji);
                        e.setId(id--);
                        fn.apply(e);
                        // Right.
                        e.setHeadNode(chart[i][j][g][INCOMPLETE]);
                        e.setTailNodes(chart[i][r][g][COMPLETE],
                                       chart[j][r+1][i][COMPLETE]);
                        e.setWeight(sij);
                        e.setId(id--);
                        fn.apply(e);
                    }
                }
            }
        }

        // Initialize.
        for (int s = nplus-1; s >= 0; s--) {
            for (int g = nplus - 1; g >= 0; g--) {
                if (g == s && s != 0) { continue; }
                e.setHeadNode(chart[s][s][g][COMPLETE]);
                e.setTailNodes();
                e.setWeight(a.one());
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

    public Algebra getAlgebra() {
        return a;
    }
    
}
