package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.berkeley.nlp.PCFGLA.smoothing.SrlBerkeleySignatureBuilder;
import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.featurize.TemplateFeatureExtractor.LocalObservations;
import edu.jhu.featurize.TemplateLanguage.EdgeProperty;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate0;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate1;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate2;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate3;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate4;
import edu.jhu.featurize.TemplateLanguage.JoinTemplate;
import edu.jhu.featurize.TemplateLanguage.ListModifier;
import edu.jhu.featurize.TemplateLanguage.OtherFeat;
import edu.jhu.featurize.TemplateLanguage.Position;
import edu.jhu.featurize.TemplateLanguage.PositionList;
import edu.jhu.featurize.TemplateLanguage.PositionModifier;
import edu.jhu.featurize.TemplateLanguage.RulePiece;
import edu.jhu.featurize.TemplateLanguage.SymbolProperty;
import edu.jhu.featurize.TemplateLanguage.TokPropList;
import edu.jhu.featurize.TemplateLanguage.TokProperty;
import edu.jhu.parse.cky.Rule;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.util.SafeEquals;

/**
 * Defines a feature template extractor for templates based on a 'little
 * language'.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class TemplateFeatureExtractor {
   
    /**
     * The local observations. The union of this class with the sentence / corpus
     * stats member variables of the {@link TemplateFeatureExtractor} form the
     * full set of observed variables.
     * 
     * @author mgormley
     */
    public static class LocalObservations {
        public static final int UNDEF_INT = Integer.MIN_VALUE;
        private int pidx = UNDEF_INT;
        private int cidx = UNDEF_INT;
        private int midx = UNDEF_INT;
        private Rule rule = null;
        private int rStartIdx = UNDEF_INT;
        private int rMidIdx = UNDEF_INT;
        private int rEndIdx = UNDEF_INT;
        private LocalObservations() { }
        public LocalObservations(int pidx, int cidx, int midx, Rule rule, int rStartIdx, int rMidIdx, int rEndIdx) {
            super();
            this.pidx = pidx;
            this.cidx = cidx;
            this.midx = midx;
            this.rule = rule;
            this.rStartIdx = rStartIdx;
            this.rMidIdx = rMidIdx;
            this.rEndIdx = rEndIdx;
        }
        public int getPidx() {
            return getIfDefined(pidx, "parent index");
        }
        public int getCidx() {
            return getIfDefined(cidx, "child index");
        }
        public int getMidx() {
            return getIfDefined(midx, "modifier index");
        }
        public Rule getRule() {
            return getIfDefined(rule, "rule");
        }
        public int getRStartIdx() {
            return getIfDefined(rStartIdx, "Start of a rule span");
        }
        public int getRMidIdx() {
            return getIfDefined(rMidIdx, "Split point for a rule span");
        }
        public int getREndIdx() {
            return getIfDefined(rEndIdx, "End of a rule span");
        }
        private int getIfDefined(int idx, String obsName) {
            if (idx == UNDEF_INT) {
                throw new IllegalStateException("Local observation undefined: " + obsName);
            }
            return idx;
        }
        private <T> T getIfDefined(T obj, String obsName) {
            if (obj == null) {
                throw new IllegalStateException("Local observation undefined: " + obsName);
            }
            return obj;
        }
        /* ---------- Factory Methods ----------- */
        public static LocalObservations newPidxCidx(int pidx, int cidx) {
            LocalObservations pi = new LocalObservations();
            pi.pidx = pidx;
            pi.cidx = cidx;
            return pi;
        }
        public static LocalObservations newPidxCidxMidx(int pidx, int cidx, int midx) {
            LocalObservations pi = new LocalObservations();
            pi.pidx = pidx;
            pi.cidx = cidx;
            pi.midx = midx;
            return pi;
        }
        public static LocalObservations newPidx(int pidx) {
            LocalObservations pi = new LocalObservations();
            pi.pidx = pidx;
            return pi;
        }
        public static LocalObservations newRule(Rule rule) {
            LocalObservations pi = new LocalObservations();
            pi.rule = rule;
            return pi;
        }
        public static LocalObservations newRuleStartMidEnd(Rule r, int start, int mid, int end) {
            LocalObservations pi = new LocalObservations();
            pi.rule = r;
            pi.rStartIdx = start;
            pi.rMidIdx = mid;
            pi.rEndIdx = end;
            return pi;
        }
    }
    
    private static final Logger log = Logger.getLogger(TemplateFeatureExtractor.class);

    private final CorpusStatistics cs;
    private final SrlBerkeleySignatureBuilder sig;
    private final FeaturizedSentence fSent; 

    /**
     * This constructor is preferred as it allows the FeaturizedSentence to
     * share work across different feature extractors.
     */
    public TemplateFeatureExtractor(FeaturizedSentence fSent, CorpusStatistics cs) {        
        this.cs = cs;
        this.sig = cs.sig;
        this.fSent = fSent;
    }
    
    public TemplateFeatureExtractor(AnnoSentence sent, CorpusStatistics cs) {
        this.cs = cs;
        if (cs != null) {
            this.sig = cs.sig;
        } else {
            this.sig = null;
        }
        this.fSent = new FeaturizedSentence(sent, cs);
    }
            
    /** Adds features for a list of feature templates. */
    public void addFeatures(List<FeatTemplate> tpls, LocalObservations local, List<String> feats) {
        for (FeatTemplate tpl : tpls) {
            addFeatures(tpl, local, feats);
        }
    }
    
    /** Adds features for a single feature template. */
    public void addFeatures(FeatTemplate tpl, LocalObservations local, List<String> feats) {
        if (tpl instanceof FeatTemplate1) {
            addTokenFeature((FeatTemplate1) tpl, local, feats);
        } else if (tpl instanceof FeatTemplate2) {
            addTokenFeatures((FeatTemplate2) tpl, local, feats);            
        } else if (tpl instanceof FeatTemplate3) {
            addListFeature((FeatTemplate3) tpl, local, feats);      
        } else if (tpl instanceof FeatTemplate4) {
            addRuleFeature((FeatTemplate4) tpl, local, feats);
        } else if (tpl instanceof FeatTemplate0) {
            addOtherFeature((FeatTemplate0) tpl, local, feats);
        } else if (tpl instanceof JoinTemplate) {
            addJoinFeature((JoinTemplate) tpl, local, feats);
        } else {
            throw new IllegalStateException("Feature not supported: " + tpl);
        }
    }
    
    /** Adds features for a list of feature templates. (The parent index and child index are the only local observations.) */
    // TODO: Remove this method when convenient.
    void addFeatures(List<FeatTemplate> tpls, int pidx, int cidx, List<String> feats) {
        addFeatures(tpls, LocalObservations.newPidxCidx(pidx, cidx), feats);
    }
    
    /** Adds features for a single feature template. (The parent index and child index are the only local observations.) */
    // TODO: Remove this method when convenient.
    void addFeatures(FeatTemplate tpl, int pidx, int cidx, List<String> feats) {
        addFeatures(tpl, LocalObservations.newPidxCidx(pidx, cidx), feats);
    }
    
    /**
     * For n-gram feature templates of the form:
     *     p.w+c_{-1}.bc0
     *     p.t+c.t
     *     p.t+c.t+p.w
     * @param tpl Structured feature template.
     * @param local Local observations.
     * @param feats The feature list to which this will be added.
     */
    protected void addJoinFeature(JoinTemplate joinTpl, LocalObservations local, List<String> feats) {
        ArrayList<String> joined = new ArrayList<String>();
        addFeatures(joinTpl.tpls[0], local, joined);
        for (int i=1; i<joinTpl.tpls.length; i++) {
            ArrayList<String> tmpFeats = new ArrayList<String>();
            if (joined.size() == 0) {
                // Short circuit since we'll never create any features.
                return;
            }
            addFeatures(joinTpl.tpls[i], local, tmpFeats);
            joined = joinIntoBigrams(joined, tmpFeats);
        }
        feats.addAll(joined);
    }

    private ArrayList<String> joinIntoBigrams(ArrayList<String> feats1, ArrayList<String> feats2) {
        ArrayList<String> joined = new ArrayList<String>();
        for (String f1 : feats1) {
            for (String f2 : feats2) {
                joined.add(toFeat(f1, f2));
            }
        }
        return joined;
    }


    /**
     * Adds feature templates of the form: 
     *     ruleP.tag
     *     ruleLc.bTag
     * @param tpl Structured feature template.
     * @param local Local observations.
     * @param feats The feature list to which this will be added.
     */
    protected void addRuleFeature(FeatTemplate4 tpl, LocalObservations local, List<String> feats) {
        RulePiece piece = tpl.piece; SymbolProperty prop = tpl.prop;
        Rule rule = local.getRule();        
        
        // Get a symbol from the rule.
        String symbol;
        switch (piece) {
        case PARENT: symbol = rule.getParentLabel(); break;
        case LEFT_CHILD: symbol = rule.getLeftChildLabel(); break;
        case RIGHT_CHILD: symbol = rule.getRightChildLabel(); break;
        default: throw new IllegalStateException();
        }
        
        // Get a property of that symbol.
        String val;
        switch (prop) {
        case TAG: val = symbol; break;
        default: throw new IllegalStateException();
        }
        
        // Create the feature.
        if (val != null) {
            feats.add(toFeat(tpl.getName(), val));
        }
    }

    /**
     * Adds features of the form: 
     *     p.bc1
     *     c_{head}.dr
     *     first(t, NOUN, path(p, root)).bc0
     * @param tpl Structured feature template.
     * @param local Local observations.
     * @param feats The feature list to which this will be added.
     */
    protected void addTokenFeature(FeatTemplate1 tpl, LocalObservations local, List<String> feats) {
        Position pos = tpl.pos; PositionModifier mod = tpl.mod; TokProperty prop = tpl.prop;
        int idx = getIndexOfPosition(local, pos);
        idx = getModifiedPosition(mod, idx);
        String val = getTokProp(prop, idx);
        if (val != null) {
            feats.add(toFeat(tpl.getName(), val));
        }
    }

    private int getIndexOfPosition(LocalObservations local, Position pos) {
        switch (pos) {
        case PARENT: return local.getPidx();
        case CHILD: return local.getCidx();
        case MODIFIER: return local.getMidx();
        case RULE_START: return local.getRStartIdx();
        case RULE_MID: return local.getRMidIdx();
        case RULE_END: return local.getREndIdx();
        default: throw new IllegalStateException();
        }
    }
    
    /** Same as above except that it permits properties of the token which expand to multiple strings. */
    protected void addTokenFeatures(FeatTemplate2 tpl, LocalObservations local, List<String> feats) {
        Position pos = tpl.pos; PositionModifier mod = tpl.mod; TokPropList prop = tpl.prop;
        int idx = getIndexOfPosition(local, pos);
        idx = getModifiedPosition(mod, idx);
        List<String> vals = getTokPropList(prop, idx);
        for (String val : vals) {
            feats.add(toFeat(tpl.getName(), val));
        }
    }

    /**
     * Gets features of the form: 
     *    path(lca(p,c),root).bc0+dir.noDup
     *    children(p).bc0.seq
     *    line(p,c).t.noDup
     *    
     * @param tpl Structured feature template.
     * @param local Local observations.
     * @param feats The feature list to which this will be added.
     */
    protected void addListFeature(FeatTemplate3 tpl, LocalObservations local, List<String> feats) {
        PositionList pl = tpl.pl; TokProperty prop = tpl.prop; EdgeProperty eprop = tpl.eprop; ListModifier lmod = tpl.lmod;
        
        if (prop == null && eprop == null) {
            throw new IllegalStateException("Feature template extracts nothing. One of prop and eprop must be non-null.");
        }
        
        List<String> vals;
        switch (pl) {
        case CHILDREN_P: case NO_FAR_CHILDREN_P: case CHILDREN_C: case NO_FAR_CHILDREN_C: case LINE_P_C: case LINE_RI_RK: case BTWN_P_C:
            if (eprop != null) {
                throw new IllegalStateException("EdgeProperty " + eprop + " is only supported on paths. Offending template: " + tpl);
            } else if (prop == null) {
                throw new IllegalStateException("TokProperty must be non-null for position lists.");
            }
            List<Integer> posList = getPositionList(pl, local);
            vals = getTokPropsForList(prop, posList);
            listAndPathHelper(vals, lmod, tpl, feats);
            return;
        case PATH_P_C: case PATH_C_LCA: case PATH_P_LCA: case PATH_LCA_ROOT: 
            List<Pair<Integer, Dir>> path = getPath(pl, local);
            vals = getTokPropsForPath(prop, eprop, path);
            listAndPathHelper(vals, lmod, tpl, feats);
            return;
        default:
            throw new IllegalStateException();
        }
    }

    private void listAndPathHelper(List<String> vals, ListModifier lmod, FeatTemplate3 tpl, List<String> feats) {
        String feat;
        if (lmod == ListModifier.UNIGRAM) {
            for (String v : vals) {
                feat = toFeat(tpl.getName(), v);
                feats.add(feat);
            }
        } else if (lmod == ListModifier.BIGRAM) {
            for (int i = -1; i < vals.size() - 1; i++) {
                feat = toFeat(tpl.getName(), safeGet(vals, i), safeGet(vals, i+1));
                feats.add(feat);
            }
        } else if (lmod == ListModifier.TRIGRAM) {
            for (int i = -2; i < vals.size() - 2; i++) {
                feat = toFeat(tpl.getName(), safeGet(vals, i), safeGet(vals, i+1), safeGet(vals, i+2));
                feats.add(feat);
            }
        } else {
            Collection<String> modList = getModifiedList(lmod, vals);
            feat = toFeat(tpl.getName(), modList);
            feats.add(feat);
        }
    }

    private String safeGet(List<String> vals, int i) {
        if (i < 0) {
            return "BOS";
        } else if(vals.size() <= i) {
            return "EOS";
        } else {
            return vals.get(i);
        }
    }

    /**
     * Gets special features.
     *
     * @param tpl Structured feature template.
     * @param local Local observations.
     * @param feats The feature list to which this will be added.
     */
    protected void addOtherFeature(FeatTemplate0 tpl, LocalObservations local, List<String> feats) {
        OtherFeat template = tpl.feat;  
        switch (template) {
        case PATH_GRAMS:
            List<Pair<Integer,Dir>> path = getPath(PositionList.PATH_P_C, local);  
            addPathGrams(tpl, path, feats);
            return;
        default:  
            String val = getOtherFeatSingleton(tpl.feat, local);
            feats.add(toFeat(tpl.getName(), val));
            return;
        }
    }

    // TODO: This is a lot of logic...and should probably live elsewhere.
    private void addPathGrams(FeatTemplate0 tpl, List<Pair<Integer, Dir>> path, List<String> feats) {
        // For each path n-gram, for n in {1,2,3}:
        for (int n=1; n<=3; n++) {
            for (int start = 0; start <= path.size() - n; start++) {
                int end = start + n;
                List<Pair<Integer,Dir>> ngram = path.subList(start, end);
                // For each pattern of length n, comprised of WORD and POS.
                TokProperty[] props = new TokProperty[] { TokProperty.WORD, TokProperty.POS };
                int max = (int) Math.pow(2, n);
                for (int pattern = 0; pattern < max; pattern++) {
                    // Create the feature for this pattern.
                    List<String> vals = new ArrayList<String>(n);
                    for (int i=0; i<n; i++) {
                        // Get the appropriate type for this pattern:
                        // ((pattern>>>i) & 1) is 1 if the i'th bit is one and 0 otherwise.
                        TokProperty prop = props[(pattern>>>i) & 1];
                        vals.addAll(getTokPropsForPath(prop, null, ngram.subList(i, i+1)));
                    }
                    // Add the feature for this pattern.
                    feats.add(toFeat(tpl.getName(), vals));
                }
            }
        }
    }
    
    private String getOtherFeatSingleton(OtherFeat template, LocalObservations local) {
        int pidx = local.getPidx();
        int cidx = local.getCidx();
        FeaturizedTokenPair pair = getFeatTokPair(pidx, cidx);
        switch (template) {
        case DISTANCE:
            return Integer.toString(Math.abs(pidx - cidx));
        case GENEOLOGY:
            return pair.getGeneologicalRelation();
        case RELATIVE:
            return pair.getRelativePosition();
        case CONTINUITY:
            return Integer.toString(pair.getCountOfNonConsecutivesInPath());
        case PATH_LEN:            
            return Integer.toString(binInt(pair.getDependencyPath().size(), 0, 2, 5, 10, 20, 30, 40));
        case SENT_LEN:            
            return Integer.toString(binInt(fSent.size(), 0, 2, 5, 10, 20, 30, 40));
        case RULE_IS_UNARY:
            return local.getRule().isUnary() ? "T" : "F";
        default:
            throw new IllegalStateException();
        }
    }

    private int binInt(int size, int...bins) {
        for (int i=bins.length-1; i >= 0; i--) {
            if (size >= bins[i]) {
                return bins[i];
            }
        }
        return Integer.MIN_VALUE;
    }

    private <T> Collection<T> getModifiedList(ListModifier lmod, Collection<T> props) {
        switch (lmod) {
        case SEQ:
            return props;
        case BAG:
            return bag(props);
        case NO_DUP:
            return noDup(props);
        default:
            throw new IllegalStateException();
        }
    }

    private List<Integer> getPositionList(PositionList pl, LocalObservations local) {              
        FeaturizedToken tok;
        FeaturizedTokenPair pair;
        switch (pl) {
        case CHILDREN_P: 
            tok = getFeatTok(local.getPidx());
            return tok.getChildren();
        case NO_FAR_CHILDREN_P: 
            tok = getFeatTok(local.getPidx());
            return tok.getNoFarChildren();
        case CHILDREN_C: 
            tok = getFeatTok(local.getCidx());
            return tok.getChildren();
        case NO_FAR_CHILDREN_C: 
            tok = getFeatTok(local.getCidx());
            return tok.getNoFarChildren();
        case LINE_P_C: 
            pair = getFeatTokPair(local.getPidx(), local.getCidx());
            return pair.getLinePath();
        case LINE_RI_RK: 
            pair = getFeatTokPair(local.getRStartIdx(), local.getREndIdx());
            return pair.getLinePath();
        case BTWN_P_C:
            pair = getFeatTokPair(local.getPidx(), local.getCidx());
            List<Integer> posList = pair.getLinePath();
            if (posList.size() > 2) {
                posList = posList.subList(1, posList.size() - 1);
            } else {
                posList = Collections.emptyList();
            }
            return posList;
        default:
            throw new IllegalStateException();
        }
    }
    
    private List<Pair<Integer, Dir>> getPath(PositionList pl, LocalObservations local) {        
        FeaturizedTokenPair pair = getFeatTokPair(local.getPidx(), local.getCidx());
        switch (pl) {
        case PATH_P_C:
            return pair.getDependencyPath();
        case PATH_C_LCA:
            return pair.getDpPathArg();
        case PATH_P_LCA:
            return pair.getDpPathPred();
        case PATH_LCA_ROOT:
            return pair.getDpPathShare();
        default:
            throw new IllegalStateException();
        }
    }
    
    private int getModifiedPosition(PositionModifier mod, int idx) {
        FeaturizedToken tok = null;
        if (mod != PositionModifier.IDENTITY && mod != PositionModifier.AFTER1 && mod != PositionModifier.BEFORE1) {
            tok = getFeatTok(idx);
        }
        
        switch (mod) {
            // --------------------- Word ---------------------  
        case IDENTITY:
            return idx;
        case BEFORE1:
            return idx - 1;
        case AFTER1:
            return idx + 1;
            // --------------------- DepTree ---------------------  
        case HEAD:
            return tok.getParent();
        case LNS:
            return tok.getNearLeftSibling();
        case RNS:
            return tok.getNearRightSibling();
        case LMC:
            return tok.getFarLeftChild();
        case RMC:
            return tok.getFarRightChild();
        case LNC:
            return tok.getNearLeftChild();
        case RNC:
            return tok.getNearRightChild();     
        case LOW_SV:
            return tok.getLowSupportVerb();
        case LOW_SN:
            return tok.getLowSupportNoun();
        case HIGH_SV:
            return tok.getHighSupportVerb();
        case HIGH_SN:
            return tok.getHighSupportNoun();
        default:
            throw new IllegalStateException();
        }
    }
    
    private List<String> getTokPropsForPath(TokProperty prop, EdgeProperty eprop, List<Pair<Integer,Dir>> path) {
        List<String> props = new ArrayList<String>(path.size());
        for (int i=0; i<path.size(); i++) {
            Pair<Integer,Dir> edge = path.get(i);
            if (prop != null) {
                String val = getTokProp(prop, edge.get1());
                if (val != null) {
                    props.add(val);
                }
            }
            if (eprop != null && i < path.size() - 1) {
                switch (eprop) {
                case DIR: props.add(edge.get2().name()); break;
                case EDGEREL:
                    int idx1 = path.get(i).get1();
                    int idx2 = path.get(i+1).get1();
                    Dir d = path.get(i).get2();
                    int idx = (d == Dir.UP) ? idx1 : idx2;
                    props.add(getTokProp(TokProperty.DEPREL, idx));                    
                    break;
                default: throw new IllegalStateException();
                }
                
            }
        }
        return props;
    }

    private List<String> getTokPropsForList(TokProperty prop, List<Integer> posList) {
        List<String> props = new ArrayList<String>(posList.size());
        for (int idx : posList) {
            String val = getTokProp(prop, idx);
            if (val != null) {
                props.add(val);
            }
        }
        return props;
    }

    // package private for testing.
    List<String> getTokPropList(TokPropList prop, int idx) {
        FeaturizedToken tok = getFeatTok(idx);
        switch (prop) {
        case EACH_MORPHO:
            return tok.getFeat();
        default:
            throw new IllegalStateException();
        }
    }
    
    /**
     * @return The property or null if the property is not included.
     */
    // package private for testing.
    String getTokProp(TokProperty prop, int idx) {
        FeaturizedToken tok = getFeatTok(idx);
        String form;
        switch (prop) {
        case WORD:
            return tok.getForm();
        case INDEX:
            return Integer.toString(idx);
        case LC:
            //TODO: return tok.getFormLc();
            return tok.getForm().toLowerCase();
        case CAPITALIZED:
            return tok.isCapatalized() ? "UC" : "LC";
        case WORD_TOP_N:
            form = tok.getForm();
            if (cs.topNWords.contains(form)) {
                return form;
            } else {
                return null;
            }
        case CHPRE5:
            form = tok.getForm();
            if (form.length() > 5) {
                return form.substring(0, Math.min(form.length(), 5));    
            } else {
                return null;
            }
        case LEMMA:
            return tok.getLemma();
        case POS:
            return tok.getPos();
        case CPOS:
            return tok.getCpos();
        case BC0:
            String bc = tok.getCluster();
            return bc.substring(0, Math.min(bc.length(), 5));
        case BC1:
            return tok.getCluster();
        case DEPREL:
            return tok.getDeprel();
        case MORPHO:
            return tok.getFeatStr();
        case MORPHO1:
            return tok.getFeat6().get(0);
        case MORPHO2:
            return tok.getFeat6().get(1);
        case MORPHO3:
            return tok.getFeat6().get(2);
        case UNK:
            log.warn("Assuming Spanish when creating UNK feature.");            
            return sig.getSignature(tok.getForm(), idx, "es");
        default:
            throw new IllegalStateException();
        }
    }
    
    protected static <T> Collection<T> bag(Collection<T> elements) {
        // bag, which removes all duplicated strings and sort the rest
        return new TreeSet<T>(elements);
    }
    
    protected static <T> ArrayList<T> noDup(Collection<T> elements) {
        // noDup, which removes all duplicated neighbored strings.
        ArrayList<T> noDupElements = new ArrayList<T>();
        T lastA = null;
        for (T a : elements) {
            if (!a.equals(lastA)) {
                noDupElements.add(a);
            }
            lastA = a;
        }
        return noDupElements;
    }
    
    private FeaturizedToken getFeatTok(int idx) {
        return fSent.getFeatTok(idx);
    }
    
    private FeaturizedTokenPair getFeatTokPair(int pidx, int cidx) {
        return fSent.getFeatTokPair(pidx, cidx);
    }
    
    private String toFeat(String... vals) {
        return org.apache.commons.lang.StringUtils.join(vals, "_");
    }

    private String toFeat(String name, Collection<String> vals) {
        return name + "_" + org.apache.commons.lang.StringUtils.join(vals, "_");
    }

    private String toFeat(String f1, String f2) {
        return f1 + "_" + f2;
    }

}
