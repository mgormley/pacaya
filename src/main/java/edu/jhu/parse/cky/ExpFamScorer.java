package edu.jhu.parse.cky;

import edu.jhu.data.simple.SimpleAnnoSentence;

public class ExpFamScorer implements Scorer {

    private SimpleAnnoSentence sent;
        
    public ExpFamScorer(SimpleAnnoSentence sent) {
        this.sent = sent;
    }

    @Override
    public final double score(Rule r, int start, int mid, int end) {
        return r.getScore();
    }

}
