package edu.jhu.pacaya.parse.cky;

public class RuleScorer implements Scorer {

    @Override
    public final double score(Rule r, int start, int mid, int end) {
        return r.getScore();
    }

}
