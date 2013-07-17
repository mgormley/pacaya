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

import edu.berkeley.nlp.PCFGLA.smoothing.BerkeleySignatureBuilder;
import edu.jhu.data.Label;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.FeatureExtractor;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.Var;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;
import edu.jhu.gm.Var.VarType;
import edu.jhu.util.Alphabet;

public class ProcessSentence {
    private boolean predsGiven;
    private boolean goldHead;
    private String language;
    public Set<String> allFeatures = new HashSet<String>();
    public Set<String> knownWords = new HashSet<String>();
    public Set<String> knownUnks = new HashSet<String>();
    // Currently not using this (will it matter?);
    public Set<Set<String>> knownBigrams = new HashSet<Set<String>>();
    public Set<String> knownPostags = new HashSet<String>();
    public Set<String> knownRoles = new HashSet<String>();
    public Set<String> knownLinks = new HashSet<String>();
    public Integer maxSentLength = 0;
    
    private static Map<String,String> stringMap;
    private boolean hasPred;
    private VarConfig varAssignments;
    
    private static Alphabet<Label> lexAlphabet = new Alphabet<Label>();
    private static BerkeleySignatureBuilder sig = new BerkeleySignatureBuilder(lexAlphabet);
        
    public void getFGExample(CoNLL09Sentence sent, boolean isTrain, boolean predsGiven, boolean goldHead, String language, Set<String> knownLinks, Set<String> knownRoles) {
        this.goldHead = goldHead;
        this.predsGiven = predsGiven;
        this.language = language;
        this.knownLinks = knownLinks;
        this.knownRoles = knownRoles;
        boolean hasPred = false;
        Map<Pair<Integer,Integer>,String> knownPairs = new HashMap<Pair<Integer,Integer>,String>();
        List<SrlEdge> srlEdges = sent.getSrlGraph().getEdges();
        Set<Integer> knownPreds = new HashSet<Integer>();
        Map<Var,String> variables = new HashMap<Var,String>();
        Set<String> features = new HashSet<String>();
        // all the "Y"s
        for (SrlEdge e : srlEdges) {
            Integer a = e.getPred().getPosition();
            hasPred = true;
            knownPreds.add(a);
            // all the args for that Y.  Assigns one label for every arg the predicate selects for.
            for (SrlEdge e2 : e.getPred().getEdges()) {
                String[] splitRole = e2.getLabel().split("-");
                String role = splitRole[0].toLowerCase();
                Integer b = e2.getArg().getPosition();
                Pair<Integer, Integer> key = new Pair(a, b);
                knownPairs.put(key, role);
            }
        }
        if (predsGiven) {
            // CoNLL-friendly model; preds given
            for (int i : knownPreds) {
                for (int j = 0; j < sent.size();j++) {
                    String pred = Integer.toString(sent.get(i).getId());
                    String arg = Integer.toString(sent.get(j).getId());
                    variables = getVariables(i, j, pred, arg, sent, knownPairs, knownPreds, isTrain);
                    setVars(pred, arg, knownPreds, isTrain, variables);
                    // For writing out ERMA-type output.
                    Set<String> suffixes = getSuffixes(pred, arg, knownPreds, isTrain);
                    features = getArgumentFeatures(i, j, suffixes, sent, srlEdges, features, knownPairs, isTrain);
                }
            }                
        } else {
            // n**2 model
            for (int i = 0; i < sent.size(); i++) {
                String pred = Integer.toString(sent.get(i).getId());
                for (int j = 0; j < sent.size();j++) {
                    String arg = Integer.toString(sent.get(j).getId());
                    variables = getVariables(i, j, pred, arg, sent, knownPairs, knownPreds, isTrain);
                    setVars(pred, arg, knownPreds, isTrain, variables);
                    // For writing out ERMA-type output.
                    Set<String> suffixes = getSuffixes(pred, arg, knownPreds, isTrain);
                    features = getArgumentFeatures(i, j, suffixes, sent, srlEdges, features, knownPairs, isTrain);
                }
            }
        }
        // TBD:  if CoNLL data
        setHasPred(hasPred);
    }
    

    // ----------------- Extracting Variables -----------------
    public Map<Var,String> getVariables(int i, int j, String pred, String arg, CoNLL09Sentence sent, Map<Pair<Integer,Integer>, String> knownPairs, Set<Integer> knownPreds, boolean isTrain) {
        // Observed input Link variables
        Var linkVar;
        Map<Var,String> variables = new HashMap<Var,String>();
        String linkVarName = "Link_" + pred + "_" + arg;
        // Syntactic head, from dependency parse.
        int head = sent.get(j).getHead();
        // To do:  Stop doing this for every i, j; only needs to be done once.
        List<String> stateNames = new ArrayList<String>();
        String stateName;
        stateNames.addAll(this.knownLinks);
        if (head != i) {
            stateName = "False";
        } else {
            stateName = "True";
        }
        linkVar = new Var(VarType.OBSERVED, stateNames.size(), linkVarName, stateNames);
        variables.put(linkVar, stateName);

        // Predicted Semantic roles
        Var roleVar;
        String roleVarName = "Role_" + pred + "_" + arg;
        stateNames = new ArrayList<String>();
        stateNames.addAll(this.knownRoles);
        int[] key = {i, j};
        // Just configured currently for CoNLL training?
        // TBD:  Make work with CoNLL unfriendly data.
        // add isTrain caveat.
        if (knownPreds.contains((Integer) i)) {
            if (knownPairs.containsKey(key)) {
                String label = knownPairs.get(key);
                stateName = label.toLowerCase();
            } else {
                stateName = "_";
            }
            roleVar = new Var(VarType.PREDICTED, stateNames.size(), roleVarName, stateNames);            
        } else { 
            roleVar = new Var(VarType.LATENT, 0, roleVarName, stateNames);
        }
        variables.put(roleVar, stateName);
        return variables;
    }
    
    
    
    // ----------------- Extracting Features -----------------
    // Naradowsky argument features.
    // TBD:  Change to "getFeatures"
    public Set<String> getArgumentFeatures(int pidx, int aidx, Set<String> suffixes, CoNLL09Sentence sent, List<SrlEdge> srlEdges, Set<String> feats, Map<Pair<Integer,Integer>,String> knownPairs, boolean isTrain) {
        // TBD:  Add basic features from BerkeleyOOV assigner (isCaps, etc).
        feats = getNaradowskyFeatures(pidx, aidx, suffixes, sent, feats, isTrain);
        feats = getZhaoFeatures(pidx, aidx, suffixes, sent, srlEdges, feats, knownPairs, isTrain);
        // feats = getNuguesFeatures();
        return feats;
    }
    
    public Set<String> getNaradowskyFeatures(int pidx, int aidx, Set<String> suffixes, CoNLL09Sentence sent, Set<String> feats, boolean isTrain) {
        CoNLL09Token pred = sent.get(pidx);
        CoNLL09Token arg = sent.get(aidx);
        String predForm = decideForm(pred.getForm(), pidx);
        String argForm = decideForm(arg.getForm(), aidx);
        String predPos = pred.getPos();
        String argPos = arg.getPos();
        // Add Arg-Bias:  Bias features everybody does; it's important (see Naradowsky).
        
        // Or is it this.goldHead?
        if (!goldHead) {
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
        for (String feat : instFeats) {
            if (isTrain || allFeatures.contains(feat)) {
                if (isTrain) {
                    //if (!allFeatures.contains(feat)) {
                        allFeatures.add(feat);
                    //}
                }
                // TBD:  Remove suffixes when not building ERMA files.
                for (String suf : suffixes) {
                    feats.add(feat + suf);
                }
            }
        }
        return feats;
    }
        
    public Set<String> getZhaoFeatures(int pidx, int aidx, Set<String> suffixes, CoNLL09Sentence sent, List<SrlEdge> srlEdges, Set<String> feats, Map<Pair<Integer,Integer>,String> knownPairs, boolean isTrain) {
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
    
    
    public Set<String> getSuffixes(String pred, String arg, Set<Integer> knownPreds, boolean isTrain) {
        Set<String> suffixes = new HashSet<String>();
        // If this is testing data, or training data where i is a pred
        //if (!isTrain || knownPreds.contains(i)) {
        suffixes.add("_role(Role_" + pred + "_" + arg + ");");
        //}
        suffixes.add("_link_role(Link_" + pred + "_" + arg + ",Role_" + pred + "_" + arg + ");");
        return suffixes;
    }

    public FgExample getFGExample(FactorGraph fg, VarConfig goldConfig, FeatureExtractor featExtractor) {
        return new FgExample(fg, goldConfig, featExtractor);
    }
    
    
    // ----------------- Getters and setters
    public void setHasPred(boolean hasPred) {
        this.hasPred = hasPred;
    }
    
    public boolean getHasPred() {
        return this.hasPred;
    }

    public VarConfig getVariableAssignments() {
        return this.varAssignments;
    }
        
    public void setVars(String pred, String arg, Set<Integer> knownPreds, boolean isTrain, Map<Var,String> variables) {
        // There should be a better way to do this.
        for (Var v : variables.keySet()) {
            String state = variables.get(v);
            this.varAssignments.put(v, state);
        }
    }
    
    // get Features  (check ERMA reader).

    
    
}
