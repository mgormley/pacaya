package edu.jhu.featurize;

import java.util.HashSet;
import java.util.Set;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.CorpusStatistics.Normalize;

/**
 * Feature extraction from the observations on a sentence.
 * 
 * @author mmitchell
 * @author mgormley
 */
public class SentFeatureExtractor {

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
    
    public Set<String> createFeatureSet(int idx) {
        Set<String> feats = new HashSet<String>();
        feats.add("BIAS_FEATURE");
        addSenseFeatures(idx, feats);
        return feats;
    }
    
    public Set<String> createFeatureSet(int pidx, int aidx) {
        Set<String> feats = new HashSet<String>();
        // TBD:  Add basic features from BerkeleyOOV assigner (isCaps, etc).
        addNaradowskyFeatures(pidx, aidx, feats);
        addZhaoFeatures(pidx, aidx, feats);
        // feats = getNuguesFeatures();
        return feats;
    }
    
    // TODO: These features would be used for both the Unary link factors
    // where the parent is the wall node, and the sense factors.
    public void addSenseFeatures(int pidx, Set<String> feats) {
        // TODO: 
    }
    
    public void addNaradowskyFeatures(int pidx, int aidx, Set<String> feats) {
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
    
        Set<String> instFeats = new HashSet<String>();
        instFeats.add("head_" + predForm + "dep_" + argForm + "_word");
        instFeats.add("head_" + predPos + "_dep_" + argPos + "_pos");
        instFeats.add("head_" + predForm + "_dep_" + argPos + "_wordpos");
        instFeats.add("head_" + predPos + "_dep_" + argForm + "_posword");
        instFeats.add("head_" + predForm + "_dep_" + argForm + "_head_" + predPos + "_dep_" + argPos + "_wordwordpospos");
    
        instFeats.add("head_" + predPos + "_dep_" + argPos + "_dist_" + dist + "_posdist");
        instFeats.add("head_" + predPos + "_dep_" + argPos + "_dir_" + dir + "_posdir");
        instFeats.add("head_" + predPos + "_dist_" + dist + "_dir_" + dir + "_posdistdir");
        instFeats.add("head_" + argPos + "_dist_" + dist + "_dir_" + dir + "_posdistdir");
    
        instFeats.add("slen_" + sent.size());
        instFeats.add("dir_" + dir);
        instFeats.add("dist_" + dist);
        instFeats.add("dir_dist_" + dir + dist);
    
        instFeats.add("head_" + predForm + "_word");
        instFeats.add("head_" + predPos + "_tag");
        instFeats.add("arg_" + argForm + "_word");
        instFeats.add("arg_" + argPos + "_tag");
        
        // TBD:  Add morph features for comparison with supervised case.
        /*     if (mode >= 4) {
      val m1s = pred.morph.split("\\|")
      val m2s = arg.morph.split("\\|")
      for (m1 <- m1s; m2 <- m2s) {
        feats += "P-%sxA-%s".format(m1, m2)
      } */
    }
        
    public void addZhaoFeatures(int pidx, int aidx, Set<String> feats) {
        // Features based on CoNLL 09:
        // "Multilingual Dependency Learning:
        // A Huge Feature Engineering Method to Semantic Dependency Parsing"
        // Hai Zhao, Wenliang Chen, Chunyu Kit, Guodong Zhou
        // Feature template 1:  Syntactic path based on semantic dependencies            
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
