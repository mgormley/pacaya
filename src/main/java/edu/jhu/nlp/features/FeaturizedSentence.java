package edu.jhu.nlp.features;

import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.data.simple.AnnoSentence;

/**
 * Lazily created featurized sentence.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class FeaturizedSentence {

    private AnnoSentence sent;
    private CorpusStatistics cs;
    
    private FeaturizedToken[] toks;
    private FeaturizedTokenPair[][] pairs;
    
    public FeaturizedSentence(AnnoSentence sent, CorpusStatistics cs) {
        this.sent = sent;
        this.cs = cs;
        // We use 1-indexing internally. This allows us to cache a BOS and EOS
        // token at positions 0 and sent.size() + 1 respectively.
        int size = sent.size() + 2;
        this.toks = new FeaturizedToken[size];
        this.pairs = new FeaturizedTokenPair[size][size];
    }
    
    public FeaturizedToken getFeatTok(int idx) {
        int i = convertToInternal(idx);        
        if (toks[i] == null) {
            this.toks[i] = new FeaturizedToken(idx, sent, cs);
        }
        return toks[i];
    }
    
    public FeaturizedTokenPair getFeatTokPair(int pidx, int cidx) {
        int p = convertToInternal(pidx);
        int c = convertToInternal(cidx);
        if (pairs[p][c] == null) {
            pairs[p][c] = new FeaturizedTokenPair(pidx, cidx, getFeatTok(pidx), getFeatTok(cidx), sent);
        }
        return pairs[p][c];
    }

    public int size() {
        return sent.size();
    }
    
    private int convertToInternal(int idx) {
        idx = idx + 1;
        if (idx < 0) {
            idx = 0;
        } else if (idx >= toks.length){
            idx = toks.length - 1;
        }
        return idx;
    }
    
}
