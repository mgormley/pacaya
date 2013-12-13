package edu.jhu.featurize;

import edu.jhu.data.concrete.SimpleAnnoSentence;

/**
 * Lazily created featurized sentence.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class FeaturizedSentence {

    private SimpleAnnoSentence sent;
    
    private FeaturizedToken[] toks;
    private FeaturizedTokenPair[][] pairs;
    private FeaturizedToken featuredHeadDefault;
    private FeaturizedToken featuredTailDefault;
    
    public FeaturizedSentence(SimpleAnnoSentence sent) {
        this.sent = sent;
        int[] parents = sent.getParents();
        this.toks = new FeaturizedToken[sent.size()];
        this.pairs = new FeaturizedTokenPair[sent.size()][sent.size()];
        this.featuredHeadDefault = new FeaturizedToken(-1, sent);
        this.featuredTailDefault = new FeaturizedToken(sent.size(), sent);
    }
    
    public FeaturizedToken getFeatTok(int idx) {
        if (idx < 0) {
            return featuredHeadDefault;
        } else if (idx >= toks.length) {
            return featuredTailDefault;
        }      
        if (toks[idx] == null) {
            this.toks[idx] = new FeaturizedToken(idx, sent);
        }
        return toks[idx];
    }
    
    public FeaturizedTokenPair getFeatTokPair(int pidx, int cidx) {
        if (pairs[pidx][cidx] == null) {
            pairs[pidx][cidx] = new FeaturizedTokenPair(pidx, cidx, getFeatTok(pidx), getFeatTok(cidx), sent.getParents());
        }
        return pairs[pidx][cidx];
    }
    
}
