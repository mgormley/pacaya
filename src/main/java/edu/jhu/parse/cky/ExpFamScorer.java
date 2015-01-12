package edu.jhu.parse.cky;

import edu.jhu.nlp.data.simple.AnnoSentence;

public class ExpFamScorer implements Scorer {

    private AnnoSentence sent;
        
    public ExpFamScorer(AnnoSentence sent) {
        this.sent = sent;
    }

    @Override
    public final double score(Rule r, int start, int mid, int end) {
        return r.getScore();
    }

}
