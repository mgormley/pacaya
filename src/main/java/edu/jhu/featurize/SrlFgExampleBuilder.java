package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.featurize.CorpusStatistics.Normalize;
import edu.jhu.featurize.SrlFactorGraph.FactorType;
import edu.jhu.featurize.SrlFactorGraph.LinkVar;
import edu.jhu.featurize.SrlFactorGraph.RoleVar;
import edu.jhu.featurize.SrlFactorGraph.SrlFactor;
import edu.jhu.gm.Factor;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureExtractor;
import edu.jhu.gm.FeatureVector;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.Var;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;
import edu.jhu.util.Alphabet;

/**
 * @author mmitchell
 * @author mgormley
 */
public class SrlFgExampleBuilder {
    
    public static class SrlFgExampleBuilderPrm {
        private ModelStructure structure = ModelStructure.ALL_PAIRS;
        private boolean useGoldPos = false;
        private String language = "es";
    }
    
    public enum ModelStructure {
        /** Defines Role variables each of the "known" predicates with all possible arguments. */
        PREDS_GIVEN,
        /** The N**2 model. */
        ALL_PAIRS,
    }
    
    /** The factor graph for this sentence. */
    public FactorGraph fg;
    /** The assignment to variables given in the training data. */
    private VarConfig varAssignments;

    // Caching... hopefully remove all these.
    private HashMap<String, Factor> facs;
    private HashMap<String, ArrayList<FeatureVector>> featureRefs;
    private HashMap<VarConfig,Set<Feature>> configToFeature;

    private final SrlFgExampleBuilderPrm prm;
    private final Alphabet<Feature> alphabet;
    private final CorpusStatistics cs;

    public SrlFgExampleBuilder(SrlFgExampleBuilderPrm prm, Alphabet<Feature> alphabet, CorpusStatistics cs) {
        this.prm = prm;
        this.alphabet = alphabet;
        this.cs = cs;
    }
    
    public FgExample getFGExample(CoNLL09Sentence sent) {
        // Precompute a few things.
        SrlGraph srlGraph = sent.getSrlGraph();
        
        // Tells us whether or not we should train on this.
        // TODO: This was never used. Should we remove it?
        boolean hasPred = false;
        Map<Pair<Integer,Integer>,String> knownPairs = new HashMap<Pair<Integer,Integer>,String>();
        List<SrlEdge> srlEdges = srlGraph.getEdges();
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
        
        // Construct the factor graph.
        SrlFactorGraph sfg = new SrlFactorGraph(sent, knownPreds);        
        FactorGraph fg = sfg.getFactorGraph();
        // Get the variable assignments given in the training data.
        VarConfig trainConfig = getTrainAssignment(sent, srlGraph, sfg);        
        // Create a feature extractor for this example.
        FeatureExtractor featExtractor = getFeatureExtractor(sfg);
        
        return new FgExample(fg, trainConfig, featExtractor);
    }

    private VarConfig getTrainAssignment(CoNLL09Sentence sent, SrlGraph srlGraph, SrlFactorGraph sfg) {
        VarConfig vc = new VarConfig();

        // Add all the training data assignments to the link variables, if they are not latent.
        for (int i=0; i<sent.size(); i++) {
            for (int j=0; j<sent.size(); j++) {
                if (j != i && sfg.getLinkVar(i, j) != null) {
                    LinkVar linkVar = sfg.getLinkVar(i, j);
                    if (linkVar.getType() != VarType.LATENT) {
                        // Syntactic head, from dependency parse.
                        int head = sent.get(j).getHead();
                        int state;
                        if (head != i) {
                            state = LinkVar.FALSE;
                        } else {
                            state = LinkVar.TRUE;
                        }
                        vc.put(linkVar, state);
                    }
                }
            }
        }
        
        // Add all the training data assignments to the role variables, if they are not latent.
        for (SrlEdge edge : srlGraph.getEdges()) {
            int i = edge.getArg().getPosition();
            int j = edge.getPred().getPosition();
            String roleName = edge.getLabel();
            
            RoleVar roleVar = sfg.getRoleVar(i, j);
            if (roleVar != null && roleVar.getType() != VarType.LATENT) {
                vc.put(roleVar, roleName);
            }
        }
    }

    private FeatureExtractor getFeatureExtractor(SrlFactorGraph sfg) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public class SrlFeatureExtractor implements FeatureExtractor {

        private Set<String>[][] obsFeats;
        private SrlFactorGraph sfg;

        public SrlFeatureExtractor(SrlFactorGraph sfg) {
            this.sfg = sfg;
        }
        
        @Override
        public FeatureVector calcFeatureVector(int factorId, VarConfig varConfig) {
            SrlFactor f = (SrlFactor) sfg.getFactor(factorId);
            FactorType ft = f.getFactorType();
            VarSet vars = varConfig.getVars();
            
            // Get the observation features.
            Set<String> obsFeats;
            if (ft == FactorType.LINK_ROLE || ft == FactorType.LINK_UNARY || ft == FactorType.ROLE_UNARY) {
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
        
    }
    
    // ----------------- Extracting Features -----------------
    public Set<String> getFeatures(int pidx, int aidx, CoNLL09Sentence sent, List<SrlEdge> srlEdges, Map<Pair<Integer,Integer>,String> knownPairs) {
        Set<String> feats = new HashSet<String>();
        // TBD:  Add basic features from BerkeleyOOV assigner (isCaps, etc).
        feats = getNaradowskyFeatures(pidx, aidx, sent, feats);
        feats = getZhaoFeatures(pidx, aidx, sent, srlEdges, feats, knownPairs);
        // feats = getNuguesFeatures();
        return feats;
    }
    
    public Set<String> getSenseFeatures(int pidx, CoNLL09Sentence sent, List<SrlEdge> srlEdges, Map<Pair<Integer,Integer>,String> knownPairs) {
        Set<String> senseFeats = new HashSet<String>();
        return senseFeats;
    }
    
    public Set<String> getNaradowskyFeatures(int pidx, int aidx, CoNLL09Sentence sent, Set<String> feats) {
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
        return feats;
    }
        
    public Set<String> getZhaoFeatures(int pidx, int aidx, CoNLL09Sentence sent, List<SrlEdge> srlEdges, Set<String> feats, Map<Pair<Integer,Integer>,String> knownPairs) {
        // Features based on CoNLL 09:
        // "Multilingual Dependency Learning:
        // A Huge Feature Engineering Method to Semantic Dependency Parsing"
        // Hai Zhao, Wenliang Chen, Chunyu Kit, Guodong Zhou
        // Feature template 1:  Syntactic path based on semantic dependencies
        return feats;
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
