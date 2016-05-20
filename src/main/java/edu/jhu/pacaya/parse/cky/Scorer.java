package edu.jhu.pacaya.parse.cky;

public interface Scorer {

    double score(Rule r, int start, int mid, int end);
    
}
