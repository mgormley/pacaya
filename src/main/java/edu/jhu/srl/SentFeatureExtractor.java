package edu.jhu.srl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.srl.CorpusStatistics.Normalize;

/**
 * Feature extraction from the observations on a particular sentence.
 * 
 * @author mmitchell
 * @author mgormley
 */
// TODO: (Eventually) move this out of the SRL package.
public class SentFeatureExtractor {

    /**
     * Parameters for the SentFeatureExtractor.
     * @author mgormley
     */
    public static class SentFeatureExtractorPrm {
        public boolean useGoldPos = false;
        public String language = "es";
    }
    
    // Parameters for feature extraction.
    private SentFeatureExtractorPrm prm;
    
    private final CoNLL09Sentence sent;
    private final CorpusStatistics cs;
        
    public SentFeatureExtractor(SentFeatureExtractorPrm prm, CoNLL09Sentence sent, CorpusStatistics cs) {
        this.prm = prm;
        this.sent = sent;
        this.cs = cs;
    }

    // ----------------- Extracting Features on the Observations ONLY -----------------

    /**
     * Creates a feature set for the given word position.
     * 
     * This defines a feature function of the form f(x, i), where x is a vector
     * representing all the observations about a sentence and i is a position in
     * the sentence.
     * 
     * Examples where this feature function would be used include a unary factor
     * on a Sense variable in SRL, or a syntactic Link variable where the parent
     * is the "Wall" node.
     * 
     * @param idx The position of a word in the sentence.
     * @return The features.
     */
    public Set<String> createFeatureSet(int idx) {
        Set<String> feats = new HashSet<String>();
        feats.add("BIAS_FEATURE");
        addNaradowskySoloFeatures(idx, feats);
        addZhaoSoloFeatures(idx, feats);
        return feats;
    }
    
    /**
     * Creates a feature set for the given pair of word positions.
     * 
     * This defines a feature function of the form f(x, i, j), where x is a
     * vector representing all the observations about a sentence and i and j are
     * positions in the sentence.
     * 
     * Examples where this feature function would be used include factors
     * including a Role or Link variable in the SRL model, where both the parent
     * and child are tokens in the sentence.
     * 
     * @param pidx The "parent" position.
     * @param aidx The "child" position.
     * @return The features.
     */
    public Set<String> createFeatureSet(int pidx, int aidx) {
        Set<String> feats = new HashSet<String>();
        // TBD:  Add basic features from BerkeleyOOV assigner (isCaps, etc).
        addNaradowskyPairFeatures(pidx, aidx, feats);
        addZhaoPairFeatures(pidx, aidx, feats);
        // feats = getNuguesFeatures();
        return feats;
    }
    
    public void addNaradowskySoloFeatures(int pidx, Collection<String> feats) {
        // TODO: 
    }
    
    public void addNaradowskyPairFeatures(int pidx, int aidx, Collection<String> feats) {
        CoNLL09Token pred = sent.get(pidx);
        CoNLL09Token arg = sent.get(aidx);
        String predForm = decideForm(pred.getForm(), pidx);
        String argForm = decideForm(arg.getForm(), aidx);
        String predPos = pred.getPos();
        String argPos = arg.getPos();
        // Add Arg-Bias:  Bias features everybody does; it's important (see Naradowsky).
        
        if (!prm.useGoldPos) {
            predPos = pred.getPpos();
            argPos = arg.getPpos();
        } 
        String dir;
        int dist = Math.abs(aidx - pidx);
        if (aidx > pidx) 
            dir = "RIGHT";
        else if (aidx < pidx) 
            dir = "LEFT";
        else 
            dir = "SAME";
    
        feats.add("head_" + predForm + "dep_" + argForm + "_word");
        feats.add("head_" + predPos + "_dep_" + argPos + "_pos");
        feats.add("head_" + predForm + "_dep_" + argPos + "_wordpos");
        feats.add("head_" + predPos + "_dep_" + argForm + "_posword");
        feats.add("head_" + predForm + "_dep_" + argForm + "_head_" + predPos + "_dep_" + argPos + "_wordwordpospos");
    
        feats.add("head_" + predPos + "_dep_" + argPos + "_dist_" + dist + "_posdist");
        feats.add("head_" + predPos + "_dep_" + argPos + "_dir_" + dir + "_posdir");
        feats.add("head_" + predPos + "_dist_" + dist + "_dir_" + dir + "_posdistdir");
        feats.add("head_" + argPos + "_dist_" + dist + "_dir_" + dir + "_posdistdir");
    
        feats.add("slen_" + sent.size());
        feats.add("dir_" + dir);
        feats.add("dist_" + dist);
        feats.add("dir_dist_" + dir + dist);
    
        feats.add("head_" + predForm + "_word");
        feats.add("head_" + predPos + "_tag");
        feats.add("arg_" + argForm + "_word");
        feats.add("arg_" + argPos + "_tag");
        
        // TBD:  Add morph features for comparison with supervised case.
        /*     if (mode >= 4) {
      val m1s = pred.morph.split("\\|")
      val m2s = arg.morph.split("\\|")
      for (m1 <- m1s; m2 <- m2s) {
        feats += "P-%sxA-%s".format(m1, m2)
      } */
    }

    public void addZhaoSoloFeatures(int idx, Collection<String> feats) {
        // TODO:
    }    
    
    public void addZhaoPairFeatures(int pidx, int aidx, Collection<String> feats) {
        // Features based on CoNLL 09:
        // "Multilingual Dependency Learning:
        // A Huge Feature Engineering Method to Semantic Dependency Parsing"
        // Hai Zhao, Wenliang Chen, Chunyu Kit, Guodong Zhou
        // Feature template 1:  Syntactic path based on semantic dependencies
        
        // TODO:
    }
    
    private String decideForm(String wordForm, int idx) {
        String cleanWord = Normalize.clean(wordForm);

        if (!cs.knownWords.contains(cleanWord)) {
            String unkWord = cs.sig.getSignature(cleanWord, idx, prm.language);
            unkWord = Normalize.escape(unkWord);
            if (!cs.knownUnks.contains(unkWord)) {
                unkWord = "UNK";
                return unkWord;
            }
        }
        
        return cleanWord;
    }
    
}
