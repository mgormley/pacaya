package edu.jhu.featurize;

import java.util.HashSet;
import java.util.Set;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.CorpusStatistics.Normalize;
import edu.jhu.featurize.SrlFactorGraph.SrlFactorTemplate;
import edu.jhu.featurize.SrlFactorGraph.RoleVar;
import edu.jhu.featurize.SrlFactorGraph.SrlFactor;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureExtractor;
import edu.jhu.gm.FeatureVector;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;
import edu.jhu.util.Alphabet;

public class SrlFeatureExtractor implements FeatureExtractor {

    public static class SrlFeatureExtractorPrm {
        public boolean useGoldPos = false;
        public String language = "es";
    }
    
    // Parameters for feature extraction.
    private SrlFeatureExtractor.SrlFeatureExtractorPrm prm;
    
    // Cache of observation features.
    private Set<String>[][] obsFeats;
    
    // -- Inputs --
    private SrlFactorGraph sfg;
    private final Alphabet<Feature> alphabet;
    private final CorpusStatistics cs;
    private CoNLL09Sentence sent;
    
    public SrlFeatureExtractor(SrlFeatureExtractor.SrlFeatureExtractorPrm prm, CoNLL09Sentence sent,
            SrlFactorGraph sfg, Alphabet<Feature> alphabet, CorpusStatistics cs) {
        this.prm = prm;
        this.sent = sent;
        this.sfg = sfg;
        this.alphabet = alphabet;
        this.cs = cs;
    }
    
    @Override
    public FeatureVector calcFeatureVector(int factorId, VarConfig varConfig) {
        SrlFactor f = (SrlFactor) sfg.getFactor(factorId);
        SrlFactorTemplate ft = f.getFactorType();
        VarSet vars = varConfig.getVars();
        
        // Get the observation features.
        Set<String> obsFeats;
        if (ft == SrlFactorTemplate.LINK_ROLE || ft == SrlFactorTemplate.LINK_UNARY || ft == SrlFactorTemplate.ROLE_UNARY) {
            // Look at the variables to determine the parent and child.
            Var var = vars.iterator().next();
            int parent;
            int child;
            if (var instanceof LinkVar) {
                parent = ((LinkVar)var).getParent();
                child = ((LinkVar)var).getChild();
            } else {
                parent = ((RoleVar)var).getParent();
                child = ((RoleVar)var).getChild();
            }
            
            // Get features on the observations for a pair of words.
            obsFeats = getObsFeats(parent, child);
            // TODO: is it okay if this include the observed variables?                
        } else {
            throw new RuntimeException("Unsupported factor type: " + ft);
        }
        
        // Conjoin each observation feature with the string
        // representation of the given assignment to the given
        // variables.
        FeatureVector fv = new FeatureVector();
        String vcStr = varConfig.getStringName();
        for (String obsFeat : obsFeats) {
            String fname = vcStr + "_" + obsFeat;
            fv.add(alphabet.lookupIndex(new Feature(fname)), 1.0);
        }
        
        return fv;
    }

    private Set<String> getObsFeats(int parent, int child) {
        if (obsFeats[parent][child] == null) {
            // Lazily construct the observation features.
            obsFeats[parent][child] = getFeatures(parent, child);
        }
        return obsFeats[parent][child];
    }

    // ----------------- Extracting Features on the Observations ONLY -----------------
    
    public Set<String> getFeatures(int pidx, int aidx) {
        Set<String> feats = new HashSet<String>();
        // TBD:  Add basic features from BerkeleyOOV assigner (isCaps, etc).
        addNaradowskyFeatures(pidx, aidx, feats);
        addZhaoFeatures(pidx, aidx, feats);
        // feats = getNuguesFeatures();
        return feats;
    }
    
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