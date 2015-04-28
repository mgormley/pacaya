package edu.jhu.pacaya.hypergraph.depparse;

public interface DependencyScorer {
    double getScore(int p, int c, int g);
    int getNumTokens();
}