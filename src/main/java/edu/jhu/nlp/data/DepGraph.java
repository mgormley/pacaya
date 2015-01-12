package edu.jhu.nlp.data;

import edu.jhu.nlp.data.conll.SrlGraph;
import edu.jhu.nlp.data.conll.SrlGraph.SrlArg;
import edu.jhu.nlp.data.conll.SrlGraph.SrlEdge;
import edu.jhu.nlp.data.conll.SrlGraph.SrlPred;

/**
 * Labeled dependency graph. This can be used (for example) to represent a semantic dependency graph
 * for semantic role labeling.
 * 
 * @author mgormley
 */
public class DepGraph {

    // Encodes a labeled dependency graph. An edge is indicated by any non-null string. Null
    // indicates no edge. The index 0 corresponds to the virtual root node (i.e. the wall node in
    // dependency parsing). The sentence's tokens are 1-indexed. Note that the API exposes a
    // different representation where the virtual root has index -1 and the tokens are 0-indexed.
    private String[][] graph;
    // The sentence length.
    private int n;
    
    public DepGraph(int n) {
        this.n = n;
        this.graph = new String[n+1][n+1];
    }
    
    public DepGraph(SrlGraph srl) {
        this(srl.getNumTokens());
        for (SrlPred pred : srl.getPreds()) {
            this.set(-1, pred.getPosition(), pred.getLabel());
        }
        for (SrlEdge edge : srl.getEdges()) {
            this.set(edge.getPred().getPosition(),
                    edge.getArg().getPosition(),
                    edge.getLabel());            
        }
    }

    public String get(int p, int c) {
        checkIndices(p, c);
        return graph[p+1][c+1];
    }
    
    public String set(int p, int c, String label) {
        checkIndices(p, c);
        String prev = graph[p+1][c+1];
        graph[p+1][c+1] = label;
        return prev;
    }
        
    public SrlGraph toSrlGraph() {
        SrlGraph srl = new SrlGraph(n);
        for (int p = -1; p<n; p++) {
            for (int c=0; c<n; c++) {
                String label = get(p, c);
                if (label == null) {
                    continue;
                }
                if (p == -1) {
                    srl.addPred(new SrlPred(c, label));
                } else {
                    SrlPred pred = srl.getPredAt(p);
                    pred = (pred == null) ? new SrlPred(p, "_") : pred;
                    SrlArg arg = srl.getArgAt(c);
                    arg = (arg == null) ? new SrlArg(c) : arg;
                    srl.addEdge(new SrlEdge(pred, arg, label));
                }
            }
        }
        return srl;
    }

    private void checkIndices(int p, int c) {
        checkParentIndex(p);
        checkChildIndex(c);
    }

    private void checkChildIndex(int c) {
        if (c < 0 || n <= c) {
            throw new IllegalArgumentException("Invalid child index: " + c);
        }
    }

    private void checkParentIndex(int p) {
        if (p < -1 || n <= p) {
            throw new IllegalArgumentException("Invalid parent index: " + p);
        }
    }
    
}
