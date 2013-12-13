package edu.jhu.featurize;

import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.srl.CorpusStatistics;

/**
 * Lazily created featurized sentence.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class FeaturizedSentence {

    private SimpleAnnoSentence sent;
    private CorpusStatistics cs;
    
    private FeaturizedToken[] toks;
    private FeaturizedTokenPair[][] pairs;
    private FeaturizedToken featuredHeadDefault;
    private FeaturizedToken featuredTailDefault;
    
    public FeaturizedSentence(SimpleAnnoSentence sent, CorpusStatistics cs) {
        this.sent = sent;
        this.cs = cs;
        this.toks = new FeaturizedToken[sent.size()];
        this.pairs = new FeaturizedTokenPair[sent.size()][sent.size()];
        this.featuredHeadDefault = new FeaturizedToken(-1, sent, cs);
        this.featuredTailDefault = new FeaturizedToken(sent.size(), sent, cs);
    }
    
    public FeaturizedToken getFeatTok(int idx) {
        if (idx < 0) {
            return featuredHeadDefault;
        } else if (idx >= toks.length) {
            return featuredTailDefault;
        }      
        if (toks[idx] == null) {
            this.toks[idx] = new FeaturizedToken(idx, sent, cs);
        }
        return toks[idx];
    }
    
    public FeaturizedTokenPair getFeatTokPair(int pidx, int cidx) {
        if (pairs[pidx][cidx] == null) {
            pairs[pidx][cidx] = new FeaturizedTokenPair(pidx, cidx, getFeatTok(pidx), getFeatTok(cidx), sent);
        }
        return pairs[pidx][cidx];
    }
    
}
