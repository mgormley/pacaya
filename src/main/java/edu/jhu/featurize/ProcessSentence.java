package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.math3.util.Pair;

import data.DataSample;
import data.FeatureFile;
import data.FeatureInstance;
import data.RV;

import edu.berkeley.nlp.PCFGLA.smoothing.BerkeleySignatureBuilder;
import edu.jhu.data.Label;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.gm.Factor;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureExtractor;
import edu.jhu.gm.FeatureVector;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.Var;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;
import edu.jhu.gm.Var.VarType;
import edu.jhu.util.Alphabet;

public class ProcessSentence {
    public FactorGraph fg;
    
    public Set<Feature> allFeatures = new HashSet<Feature>();
    public Set<String> knownWords = new HashSet<String>();
    public Set<String> knownUnks = new HashSet<String>();
    // Currently not using this (will it matter?);
    public Set<Set<String>> knownBigrams = new HashSet<Set<String>>();
    public Set<String> knownPostags = new HashSet<String>();
    public Integer maxSentLength = 0;
    public List<String> linkStateNames;
    public List<String> roleStateNames;
    
    private VarConfig varAssignments;
    private FeatureExtractor featExtractor;
    private static Map<String,String> stringMap;
    private boolean hasPred;
    private boolean predsGiven;
    private boolean goldHead;
    private String language;
    private HashMap<String, Factor> facs;
    private HashMap<String, ArrayList<FeatureVector>> featureRefs;

    private Alphabet<Feature> alphabet;
    
    private static Alphabet<Label> lexAlphabet = new Alphabet<Label>();
    private static BerkeleySignatureBuilder sig = new BerkeleySignatureBuilder(lexAlphabet);
    
        
    public FgExample getFGExample(CoNLL09Sentence sent, boolean isTrain, boolean predsGiven, boolean goldHead, String language, Set<String> knownLinks, Set<String> knownRoles, Alphabet<Feature> alphabet) {
        this.goldHead = goldHead;
        this.predsGiven = predsGiven;
        this.language = language;
        this.linkStateNames.addAll(knownLinks);
        this.roleStateNames.addAll(knownRoles);
        // Saves the variable set for each feature instance to factor HashMappings
        this.facs = new HashMap<String, Factor>();
        // A mapping from a string identifier for a FeatureInstance, to a
        // list of FeatureVectors represented as HashMap<Feature,Double> 
        // (one for each configuration of the variables).
        this.featureRefs = new HashMap<String, ArrayList<FeatureVector>>();
        this.alphabet = alphabet;
        boolean hasPred = false;
        Map<Pair<Integer,Integer>,String> knownPairs = new HashMap<Pair<Integer,Integer>,String>();
        List<SrlEdge> srlEdges = sent.getSrlGraph().getEdges();
        Set<Integer> knownPreds = new HashSet<Integer>();
        // All the "Y"s
        for (SrlEdge e : srlEdges) {
            Integer a = e.getPred().getPosition();
            hasPred = true;
            knownPreds.add(a);
            // All the args for that Y.  Assigns one label for every arg the predicate selects for.
            for (SrlEdge e2 : e.getPred().getEdges()) {
                String[] splitRole = e2.getLabel().split("-");
                String role = splitRole[0].toLowerCase();
                Integer b = e2.getArg().getPosition();
                Pair<Integer, Integer> key = new Pair<Integer, Integer>(a, b);
                knownPairs.put(key, role);
            }
        }
        // Tells us whether or not we should train on this.
        setHasPred(hasPred);
        
        if (this.predsGiven) {
            // CoNLL-friendly model; preds given
            for (int i : knownPreds) {
                String pred = Integer.toString(sent.get(i).getId());
                for (int j = 0; j < sent.size();j++) {
                    String arg = Integer.toString(sent.get(j).getId());
                    extractFeatsAndVars(i, j, pred, arg, sent, knownPairs, knownPreds, srlEdges, isTrain);
                }
            }                
        } else {
            // n**2 model
            for (int i = 0; i < sent.size(); i++) {
                String pred = Integer.toString(sent.get(i).getId());
                for (int j = 0; j < sent.size();j++) {
                    String arg = Integer.toString(sent.get(j).getId());
                    extractFeatsAndVars(i, j, pred, arg, sent, knownPairs, knownPreds, srlEdges, isTrain);                       
                }
            }
        }
        FgExample fg = setFeatsAndVars();
        return fg;
    }
    
    public void extractFeatsAndVars(int i, int j, String pred, String arg, CoNLL09Sentence sent, Map<Pair<Integer,Integer>, String> knownPairs, Set<Integer> knownPreds, List<SrlEdge> srlEdges, boolean isTrain) {
        VarConfig varGroup = new VarConfig();
        Set<Feature> featGroup = new HashSet<Feature>();
        ArrayList<FeatureVector> featRef = new ArrayList<FeatureVector>();
        // Get variables for this feature instance
        varGroup = getVariables(i, j, pred, arg, sent, knownPairs, knownPreds, isTrain);
        // Set these variables
        addVarConfig(pred, arg, knownPreds, isTrain, varGroup);
        // Get features for this feature instance
        featGroup = getFeatures(i, j, sent, srlEdges, featGroup, knownPairs, isTrain);
        featRef = updateFacs(varGroup);
        updateFeatures(featGroup, varGroup, featRef);
    }

    
    public void updateFeatures(Set<Feature> featGroup, VarConfig varGroup, ArrayList<FeatureVector> featRef) {
        // For each feature in this feature instance's feature group
        // (featGroup), the feature is added to the feature vector for the
        // appropriate configuration of the variables.
        for (Feature feat : featGroup) {
            // Compute the state corresponding to the variable setting
            VarConfig varVals = new VarConfig();
            int k = 0;
            for(Var v : varGroup.getVars()) {
                varVals.put(v, k);
                k++;
            }
            int state = varVals.getConfigIndex();

            // Look up this feature's index.
            int featIdx = this.alphabet.lookupIndex(feat);           
            FeatureVector featureVector = featRef.get(state);
            // Add the feature weight for this feature to the feature vector.
            // Don't currently use this; I will set to 1.0.
            featureVector.add(featIdx, 1.0);                
        }
    }
    
    
    
    public ArrayList<FeatureVector> updateFacs(VarConfig varGroup) {
        String key = makeKey(varGroup);        
        // a factor
        Factor fac;
        ArrayList<FeatureVector> featRef;
        if (!this.facs.containsKey(key)) {
            VarSet Ivars = new VarSet();
            for (Var v : varGroup.getVars()){
                Ivars.add(v);
            }
            fac = new Factor(Ivars, 1.0);
            // key made out of this feature instance to a Factor from this variable configuration
            this.facs.put(key,fac);
            // One feature vector for each configuration of the variables for this factor.
            featRef = new ArrayList<FeatureVector>();
            int numConfigs = fac.getVars().calcNumConfigs();
            for (int t = 0; t < numConfigs; t++){
                featRef.add(new FeatureVector());
            }
            this.featureRefs.put(key, featRef);
        } else {
            fac = facs.get(key);
            featRef = featureRefs.get(key);
        }
        return featRef;
  }

    
    
    private FgExample setFeatsAndVars() {
        // An array list of factors, indexed by factor Id.
        ArrayList<Factor> facsVec = new ArrayList<Factor>();
        // An array of feature vectors, indexed by factor id and config index.
        ArrayList<ArrayList<FeatureVector>> featureRefVec = new ArrayList<ArrayList<FeatureVector>>();
        for (String factKey : this.facs.keySet()) {
            Factor fact = this.facs.get(factKey);
            facsVec.add(fact);
            ArrayList<FeatureVector> fr = this.featureRefs.get(factKey);
            featureRefVec.add(fr);
        }
        setFactorGraph(facsVec);
        
        VarConfig trainConfig = getVarConfig();
        // Create a feature extractor which just looks up the appropriate feature vectors in featureRefVec.
        FeatureExtractor featExtractor = new SimpleLookupFeatureExtractor(featureRefVec);
        
        FgExample fgEx = new FgExample(this.fg, trainConfig, featExtractor);
        return fgEx;
    }
        
        
    public String makeKey(VarConfig varGroup) {
        StringBuilder sb = new StringBuilder();
        // Not sure if this is the best way to make a key.
        for (Var v : varGroup.getVars()) {
            sb.append(v.getType().toString());
            sb.append(v.getName());
            String state = Integer.toString(varGroup.getState(v));
            sb.append(state);
        }
        return sb.toString();
    }
            
            
                
            
        

    // ----------------- Extracting Variables -----------------
    public VarConfig getVariables(int i, int j, String pred, String arg, CoNLL09Sentence sent, Map<Pair<Integer,Integer>, String> knownPairs, Set<Integer> knownPreds, boolean isTrain) {
        VarConfig vc = new VarConfig();
        // Observed input Link variables
        Var linkVar;
        String linkVarName = "Link_" + pred + "_" + arg;
        // Syntactic head, from dependency parse.
        int head = sent.get(j).getHead();
        String stateName;
        if (head != i) {
            stateName = "False";
        } else {
            stateName = "True";
        }
        linkVar = new Var(VarType.OBSERVED, this.linkStateNames.size(), linkVarName, this.linkStateNames);
        vc.put(linkVar, stateName);

        // Predicted Semantic roles
        Var roleVar;
        String roleVarName = "Role_" + pred + "_" + arg;
        int[] key = {i, j};
        // for training, we must know pred.
        // for testing, we don't know the pred if it's not CoNLL; 
        // but the features will be the same regardless of the state here.
        if (knownPreds.contains((Integer) i)) {
            if (knownPairs.containsKey(key)) {
                String label = knownPairs.get(key);
                stateName = label.toLowerCase();
            } else {
                stateName = "_";
            }
            roleVar = new Var(VarType.PREDICTED, this.roleStateNames.size(), roleVarName, this.roleStateNames);            
        } else { 
            roleVar = new Var(VarType.LATENT, 0, roleVarName, this.roleStateNames);
        }
        vc.put(roleVar, stateName);
        return vc;
    }    
    
    // ----------------- Extracting Features -----------------
    public Set<Feature> getFeatures(int pidx, int aidx, CoNLL09Sentence sent, List<SrlEdge> srlEdges, Set<Feature> feats, Map<Pair<Integer,Integer>,String> knownPairs, boolean isTrain) {
        // TBD:  Add basic features from BerkeleyOOV assigner (isCaps, etc).
        feats = getNaradowskyFeatures(pidx, aidx, sent, feats, isTrain);
        feats = getZhaoFeatures(pidx, aidx, sent, srlEdges, feats, knownPairs, isTrain);
        // feats = getNuguesFeatures();
        return feats;
    }
    
    public Set<Feature> getNaradowskyFeatures(int pidx, int aidx, CoNLL09Sentence sent, Set<Feature> feats, boolean isTrain) {
        CoNLL09Token pred = sent.get(pidx);
        CoNLL09Token arg = sent.get(aidx);
        String predForm = decideForm(pred.getForm(), pidx);
        String argForm = decideForm(arg.getForm(), aidx);
        String predPos = pred.getPos();
        String argPos = arg.getPos();
        // Add Arg-Bias:  Bias features everybody does; it's important (see Naradowsky).
        
        if (!this.goldHead) {
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
    
        Set<Feature> instFeats = new HashSet<Feature>();
        instFeats.add(new Feature("head_" + predForm + "dep_" + argForm + "_word"));
        instFeats.add(new Feature("head_" + predPos + "_dep_" + argPos + "_pos"));
        instFeats.add(new Feature("head_" + predForm + "_dep_" + argPos + "_wordpos"));
        instFeats.add(new Feature("head_" + predPos + "_dep_" + argForm + "_posword"));
        instFeats.add(new Feature("head_" + predForm + "_dep_" + argForm + "_head_" + predPos + "_dep_" + argPos + "_wordwordpospos"));
    
        instFeats.add(new Feature("head_" + predPos + "_dep_" + argPos + "_dist_" + dist + "_posdist"));
        instFeats.add(new Feature("head_" + predPos + "_dep_" + argPos + "_dir_" + dir + "_posdir"));
        instFeats.add(new Feature("head_" + predPos + "_dist_" + dist + "_dir_" + dir + "_posdistdir"));
        instFeats.add(new Feature("head_" + argPos + "_dist_" + dist + "_dir_" + dir + "_posdistdir"));
    
        instFeats.add(new Feature("slen_" + sent.size()));
        instFeats.add(new Feature("dir_" + dir));
        instFeats.add(new Feature("dist_" + dist));
        instFeats.add(new Feature("dir_dist_" + dir + dist));
    
        instFeats.add(new Feature("head_" + predForm + "_word"));
        instFeats.add(new Feature("head_" + predPos + "_tag"));
        instFeats.add(new Feature("arg_" + argForm + "_word"));
        instFeats.add(new Feature("arg_" + argPos + "_tag"));
        
        // TBD:  Add morph features for comparison with supervised case.
        /*     if (mode >= 4) {
      val m1s = pred.morph.split("\\|")
      val m2s = arg.morph.split("\\|")
      for (m1 <- m1s; m2 <- m2s) {
        feats += "P-%sxA-%s".format(m1, m2)
      } */
        for (Feature feat : instFeats) {
            if (isTrain || allFeatures.contains(feat)) {
                if (isTrain) {
                    allFeatures.add(feat);
                }
            }
        }
        return feats;
    }
        
    public Set<Feature> getZhaoFeatures(int pidx, int aidx, CoNLL09Sentence sent, List<SrlEdge> srlEdges, Set<Feature> feats, Map<Pair<Integer,Integer>,String> knownPairs, boolean isTrain) {
        // Features based on CoNLL 09:
        // "Multilingual Dependency Learning:
        // A Huge Feature Engineering Method to Semantic Dependency Parsing"
        // Hai Zhao, Wenliang Chen, Chunyu Kit, Guodong Zhou
        // Feature template 1:  Syntactic path based on semantic dependencies
        return feats;
    }
    
    private String decideForm(String wordForm, int idx) {
        if (!knownWords.contains(wordForm)) {
            wordForm = sig.getSignature(wordForm, idx, language);
            if (!knownUnks.contains(wordForm)) {
                wordForm = "UNK";
                return wordForm;
            }
        }
        Iterator<Entry<String, String>> it = stringMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            wordForm = wordForm.replace((String) pairs.getKey(), (String) pairs.getValue());
        }
        return wordForm;
    }
    
    
    /* for output ERMA format
     * public Set<String> getSuffixes(String pred, String arg, Set<Integer> knownPreds, boolean isTrain) {
        Set<String> suffixes = new HashSet<String>();
        // If this is testing data, or training data where i is a pred
        //if (!isTrain || knownPreds.contains(i)) {
        suffixes.add("_role(Role_" + pred + "_" + arg + ");");
        //}
        suffixes.add("_link_role(Link_" + pred + "_" + arg + ",Role_" + pred + "_" + arg + ");");
        return suffixes;
    }*/    
    
    // ----------------- Getters and setters
    public void setHasPred(boolean hasPred) {
        this.hasPred = hasPred;
    }
    
    public boolean getHasPred() {
        return this.hasPred;
    }

    public ArrayList<Feature> getFeatures() {
        return this.features;
    }
    
    public VarConfig getVarConfig() {
        return this.varAssignments;
    }
        
    public void addVarConfig(String pred, String arg, Set<Integer> knownPreds, boolean isTrain, VarConfig variables) {
        // Add variable assignments for this instance.
        for (Var v : variables.getVars()) {
            String state = variables.getStateName(v);
            this.varAssignments.put(v, state);
        }
    }
    
    public FactorGraph getFactorGraph() {
        return this.fg;
    }
    
    public void addFactor(VarSet variables) {
        /*  Used to be called from above:
         * VarSet factorVars = new VarSet();
        factorVars.addAll(variables.getVars());
        addFactor(factorVars); */
        // Should each variable have an associated weight?
        Factor f = new Factor(variables);
        this.fg.addFactor(f);
    }
    
    public void setFactorGraph(ArrayList<Factor> facsVec) {
        // Construct a new factor graph.
        FactorGraph fg = new FactorGraph();
        for (Factor factor : facsVec) {
            fg.addFactor(factor);
        }
    }
    
    private static class SimpleLookupFeatureExtractor implements FeatureExtractor {

        private ArrayList<ArrayList<FeatureVector>> feature_ref_vec;

        public SimpleLookupFeatureExtractor(
                ArrayList<ArrayList<FeatureVector>> feature_ref_vec) {
            this.feature_ref_vec = feature_ref_vec;
        }

        @Override
        public FeatureVector calcFeatureVector(int factorId, VarConfig varConfig) {
            int configId = varConfig.getConfigIndex();
            return feature_ref_vec.get(factorId).get(configId);
        }
        
    }
    
    
}