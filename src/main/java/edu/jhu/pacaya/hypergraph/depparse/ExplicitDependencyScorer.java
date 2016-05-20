package edu.jhu.pacaya.hypergraph.depparse;

import java.util.Arrays;

public class ExplicitDependencyScorer implements DependencyScorer {

    // Indexed by parent, child, other (i.e. sibling or grandparent)
    private double[][][] scores;
    private int n;
            
    public ExplicitDependencyScorer(double[][][] scores, int n) {
        super();
        this.scores = scores;
        this.n = n;
    }

    @Override
    public double getScore(int p, int c, int g) {
        return scores[p][c][g];
    }

    @Override
    public int getNumTokens() {
        return n;
    }
    
    public String toString() {
        return Arrays.deepToString(scores);
    }
}