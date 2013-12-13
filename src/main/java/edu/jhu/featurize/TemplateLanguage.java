package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.berkeley.nlp.PCFGLA.smoothing.SrlBerkeleySignatureBuilder;
import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.concrete.SimpleAnnoSentence;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.srl.SrlRunner;
import edu.stanford.nlp.util.StringUtils;

/**
 * Defines a little language for structured feature templates, and the feature
 * extractors for those templates.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class TemplateLanguage {

    private static final Logger log = Logger.getLogger(TemplateLanguage.class);

    /** Annotation Type. */
    public enum AT {
        WORD, LEMMA, POS, BROWN, MORPHO, DEP_TREE, LABEL_DEP_TREE;
    }
    
    /** Word property. A mapping from a position to a string. */
    public enum TokProperty {
        WORD, LEMMA, POS, BC0, BC1, MORPHO, DEPREL, LC, UNK, CHPRE5; 
    }

    /** Word property list expansion. A mapping from a position to a list of strings. */ 
    public enum TokPropList {
        MORPHO, CH, CHPRE_N, CHSUF_N;
    }

    /** Directed Edge Property. A mapping from a directed edge to a string. */
    public enum EdgeProperty {
        DIR, DR_DIR;
    }

    /** Position Modifier. A mapping from one position to another. */
    public enum PositionModifier {
        BEFORE1, AFTER1,
        //
        HEAD, LMD, RMD, LNS, RNS, LMC, RMC, LNC, RNC,
        //
        LOW_SV, LOW_SN, HIGH_SV, HIGH_SN,
    }

    /** Position List. Mapping from one or two positions to a position list. */
    public enum PositionList {
        LINE_P_C, CHILDREN_P, NO_FAR_CHILDREN_P, PATH_P_C, PATH_P_ROOT, PATH_C_ROOT, PATH_LCA_ROOT,
    }

    /** List Modifiers. Mapping of a list of strings to a new list of strings. */
    public enum ListModifier {
        SEQ, BAG, NO_DUP,
    }

    /**
     * Additional Features. Mapping from parent and child positions to a
     * feature.
     */
    public enum OtherFeats {
        RELATIVE, DISTANCE, GENEOLOGY, DEP_SUB_CAT, PATH_LEN,
        // TODO: Not implemented:
        // PRED_VOICE_WORD_OR_POS,
        // PATH_GRAMS,
        // CONTINUITY,
    }

    /** Positions. */
    public enum Position {
        PARENT, CHILD;
    }

    private static ArrayList<Description> desc = new ArrayList<Description>();

    private static void desc(Object obj, String name, String description, AT... requiredLevels) {
        desc.add(new Description(obj, name, description, requiredLevels));
    }
    
    static {
        /** Word property. A mapping from a position to a string. */
        desc(TokProperty.WORD, "w", "Word", AT.WORD);
        desc(TokProperty.LEMMA, "l", "Lemma", AT.LEMMA);
        desc(TokProperty.POS, "t", "POS Tag", AT.POS);
        desc(TokProperty.BC0, "bc0", "Coarse-grained Brown cluster", AT.BROWN);
        desc(TokProperty.BC1, "bc1", "Fine-grained Brown cluster", AT.BROWN);
        desc(TokProperty.MORPHO, "m1", "Morphological features", AT.MORPHO);
        desc(TokProperty.DEPREL, "dr", "Dependency relation to head", AT.LABEL_DEP_TREE);
        desc(TokProperty.LC, "lc", "Lower-cased word", AT.WORD);
        desc(TokProperty.UNK, "unk", "Unknown word class", AT.WORD);
        desc(TokProperty.CHPRE5, "lc", "5-character prefix of a word", AT.WORD);
        
        /** Word property list expansion. A mapping from a position to a list of strings. */ 
        desc(TokPropList.MORPHO, "m1", "Morphological features", AT.MORPHO);
        desc(TokPropList.CH, "ch", "Each character of the word", AT.WORD);
        desc(TokPropList.CHPRE_N, "chpre_n", "Character n-gram prefix", AT.WORD);
        desc(TokPropList.CHSUF_N, "chsuf_n", "Character n-gram suffix", AT.WORD);
   
        /** Directed Edge Property. A mapping from a directed edge to a string. */
        desc(EdgeProperty.DIR, "dir", "Direction of an edge in a path", AT.DEP_TREE);
        desc(EdgeProperty.DR_DIR, "dr+dir", "Conjunction of dr and dir", AT.LABEL_DEP_TREE);

        /** Position Modifier. A mapping from one position to another. */
        desc(PositionModifier.BEFORE1, "w_{-1}", "1 before w", AT.WORD);
        desc(PositionModifier.AFTER1, "w_{1}", "1 after w", AT.WORD);
        //
        desc(PositionModifier.HEAD, "w_{head}", "Syntactic head of w", AT.DEP_TREE);
        desc(PositionModifier.LMD, "w_{lmd}", "Leftmost descendent", AT.DEP_TREE);
        desc(PositionModifier.RMD, "w_{rmd}", "Rightmost descendent", AT.DEP_TREE);
        desc(PositionModifier.LNS, "w_{lns}", "Left nearest sibling", AT.DEP_TREE);
        desc(PositionModifier.RNS, "w_{rns}", "Right nearest sibling", AT.DEP_TREE);
        desc(PositionModifier.LMC, "w_{lmc}", "Leftmost child", AT.DEP_TREE);
        desc(PositionModifier.RMC, "w_{rmc}", "Rightmost child", AT.DEP_TREE);
        desc(PositionModifier.LNC, "w_{lnc}", "Left nearest child", AT.DEP_TREE);
        desc(PositionModifier.RNC, "w_{rnc}", "Right nearest child", AT.DEP_TREE);
        //
        desc(PositionModifier.LOW_SV, "first(t, VERB, path(p, root))", "Low support Verb", AT.POS, AT.DEP_TREE);
        desc(PositionModifier.LOW_SN, "first(t, NOUN, path(p, root))", "Low support Noun", AT.POS, AT.DEP_TREE);
        desc(PositionModifier.HIGH_SV, "first(t, VERB, path(root, p))", "High support Verb", AT.POS, AT.DEP_TREE);
        desc(PositionModifier.HIGH_SN, "first(t, NOUN, path(root, p))", "High support Noun", AT.POS, AT.DEP_TREE);

        /** Position List. Mapping from one or two positions to a position list. */
        desc(PositionList.LINE_P_C, "line(p,c)", "horizontal path between p and c", AT.WORD);
        desc(PositionList.CHILDREN_P, "children(p)", "Children of p", AT.DEP_TREE);
        desc(PositionList.NO_FAR_CHILDREN_P, "noFarChildren(p)", "children without the leftmost or rightmost included", AT.DEP_TREE);
        desc(PositionList.PATH_P_C, "path(p,c)", "from parent to child", AT.DEP_TREE);
        desc(PositionList.PATH_P_ROOT, "path(p,root)", "from parent to root", AT.DEP_TREE);
        desc(PositionList.PATH_C_ROOT, "path(c,root)", "from child to root", AT.DEP_TREE);
        desc(PositionList.PATH_LCA_ROOT, "path(lca(p,c),root)", "from least-common-ancestor to root ", AT.DEP_TREE);
   
        /** List Modifiers. Mapping of a list of strings to a new list of strings. */
        desc(ListModifier.SEQ, "seq", "Identity function.");
        desc(ListModifier.BAG, "bag", "List to set.");
        desc(ListModifier.NO_DUP, "noDup", "Unix “uniq” on original list.");
   
        /** Additional Features. Mapping from parent and child positions to a feature. */
        desc(OtherFeats.RELATIVE, "relative(p,c)", "Relative position of p and c: before, after, on.", AT.WORD);
        desc(OtherFeats.DISTANCE, "distance(p,c)", "Distance binned into greater than: 2, 5, 10, 20, 30, or 40", AT.WORD);
        desc(OtherFeats.GENEOLOGY, "geneology(p,c)", "geneological relationship between p and c in a syntactic parse: parent, child, ancestor, descendent.", AT.DEP_TREE);
        desc(OtherFeats.DEP_SUB_CAT, "DepSubCat from Bjorkelund et al. (2009)", "??", AT.LABEL_DEP_TREE);
        desc(OtherFeats.PATH_LEN, "len(path(p,c))", "Path length binned into greater than: 2, 5, 10, 20, 30, or 40", AT.DEP_TREE);
        
        // TODO:
        //desc(OtherFeats.PRED_VOICE_WORD_OR_POS, "p.voice+a.word / p.voice+a.t", "The predicate voice and the  word/POS of the argument.", AT.LABEL_DEP_TREE);
        //desc(OtherFeats.PATH_GRAMS, "1,2,3-grams(path(p,c)).word/pos", "$1,2,3$-gram path features of words/POS tags", AT.DEP_TREE);
        //desc(OtherFeats.CONTINUITY, "continuity(path(p,c))", "The number of non-consecutive token pairs  in a predicate-argument path.", AT.DEP_TREE);

        /** Positions. */
        desc(Position.PARENT, "p", "Parent");
        desc(Position.CHILD, "c", "Child");
    }
    
    /** Feature function description. */
    public static class Description {
        private Object obj;
        private String name;
        private String description;
        private AT[] requiredLevels;

        /**
         * Private constructor.
         * 
         * @param obj Object being described.
         * @param name Name used in text for this function.
         * @param description Plain text description of this property.
         * @param requiredLevels Required levels of annotation.
         */
        private Description(Object obj, String name, String description, AT... requiredLevels) {
            this.obj = obj;
            this.name = name;
            this.description = description;
            this.requiredLevels = requiredLevels;
        }
    }
    
    public static abstract class FeatTemplate {
        private String name;
        public FeatTemplate(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }
    
    /**
     * For feature templates of the form: 
     *     p.bc1
     *     c_{head}.dr
     *     first(t, NOUN, path(p, root)).bc0
     */
    public class FeatTemplate1 extends FeatTemplate {
        public Position pos; 
        public PositionModifier mod; 
        public TokProperty prop;
        public String name;
        /**
         * Constructor.
         * @param pos Position to start from.
         * @param mod Modifier of the position.
         * @param prop Property to extract from the modified position.
         */
        public FeatTemplate1(Position pos, PositionModifier mod, TokProperty prop) {
            super(StringUtils.join(new String[]{pos.name(), mod.name(), prop.name()}, "."));
            this.pos = pos;
            this.mod = mod;
            this.prop = prop;
        }
    }
    
    /**
     * For feature templates of the form: 
     *     p.morpho
     * which extract multiple features of a single token.
     */
    public class FeatTemplate2 extends FeatTemplate {
        public Position pos;
        public PositionModifier mod; 
        public TokPropList prop; 
        public String name;
        /**
         * Constructor.
         * @param pos Position to start from.
         * @param mod Modifier of the position.
         * @param prop Property to extract from the modified position.
         */
        public FeatTemplate2(Position pos, PositionModifier mod, TokPropList prop) {
            super(StringUtils.join(new String[]{pos.name(), mod.name(), prop.name()}, "."));
            this.pos = pos;
            this.mod = mod;
            this.prop = prop;
        }
    }

    /**
     * For feature templates of the form: 
     *    path(lca(p,c),root).bc0+dir.noDup
     *    children(p).bc0.seq
     *    line(p,c).t.noDup
     */
    public class FeatTemplate3 extends FeatTemplate {
        public PositionList pl; 
        public TokProperty prop; 
        public boolean includeDir;
        public ListModifier lmod;
        public String name;
        /**
         * Constructor. 
         * @param pl Position list which is a function of the parent/child positions.
         * @param prop Property to extract from the position list.
         * @param includeDir Whether to include the direction of the edge when constructing a feature from a path.
         * @param lmod List modifier.
         */
        public FeatTemplate3(PositionList pl, TokProperty prop, boolean includeDir, ListModifier lmod) {
            super(StringUtils.join(new String[]{pl.name(), prop.name(), lmod.name()}, "."));
            this.pl = pl;
            this.prop = prop;
            this.lmod = lmod;
        }
    }
    
    /**
     * For bigram feature templates of the form:
     *     p.w+c_{-1}.bc0
     *     p.t+c.t
     */
    public class BigramTemplate extends FeatTemplate {
        public FeatTemplate tpl1;
        public FeatTemplate tpl2;
        public String name;
        public BigramTemplate(FeatTemplate tpl1, FeatTemplate tpl2) {
            super(StringUtils.join(new String[]{tpl1.getName(), tpl2.getName()}, "+"));
            this.tpl1 = tpl1;
            this.tpl2 = tpl2;
        }
    }
    
    //private final SimpleAnnoSentence sent;
    //private final CorpusStatistics cs;
    private final SrlBerkeleySignatureBuilder sig;
    private final FeaturizedSentence fSent; 
    
    public TemplateLanguage(SimpleAnnoSentence sent, CorpusStatistics cs) {
        //this.sent = sent;
        //this.cs = cs;
        this.sig = cs.sig;
        this.fSent = new FeaturizedSentence(sent);
    }
    
    public void addFeatures(FeatTemplate tpl, int pidx, int cidx, List<Object> feats) {
        int idx = pidx;
        if (tpl instanceof FeatTemplate1) {
            addTokenFeature((FeatTemplate1) tpl, idx, feats);
        } else if (tpl instanceof FeatTemplate2) {
            addTokenFeatures((FeatTemplate2) tpl, idx, feats);            
        } else if (tpl instanceof FeatTemplate3) {
            addListFeature((FeatTemplate3) tpl, pidx, cidx, feats);
        } else if (tpl instanceof BigramTemplate) {
            addBigramFeature((BigramTemplate) tpl, pidx, cidx, feats);
        }
    }
        
    /**
     * For bigram feature templates of the form:
     *     p.w+c_{-1}.bc0
     *     p.t+c.t
     * @param tpl Structured feature template.
     * @param pidx Token to which the parent position refers.
     * @param cidx Token to which the child position refers.
     * @param feats The feature list to which this will be added.
     */
    public void addBigramFeature(BigramTemplate tpl, int pidx, int cidx, List<Object> feats) {
        ArrayList<Object> feats1 = new ArrayList<Object>();
        ArrayList<Object> feats2 = new ArrayList<Object>();
        addFeatures(tpl.tpl1, pidx, cidx, feats1);
        addFeatures(tpl.tpl2, pidx, cidx, feats2);
        for (Object f1 : feats1) {
            for (Object f2 : feats2) {
                feats.add(toFeat(f1, f2));
            }
        }
    }

    /**
     * Adds features of the form: 
     *     p.bc1
     *     c_{head}.dr
     *     first(t, NOUN, path(p, root)).bc0
     * @param tpl Structured feature template.
     * @param idx Token to which the original position refers.
     * @param feats The feature list to which this will be added.
     */
    public void addTokenFeature(FeatTemplate1 tpl, int idx, List<Object> feats) {
        Position pos = tpl.pos; PositionModifier mod = tpl.mod; TokProperty prop = tpl.prop;
        idx = getModifiedPosition(mod, idx);
        String val = getTokProp(prop, idx);
        Object feat = toFeat(tpl.name, val);
        feats.add(feat);
    }
    
    /** Same as above except that it permits properties of the token which expand to multiple strings. */
    public void addTokenFeatures(FeatTemplate2 tpl, int idx, List<Object> feats) {
        Position pos = tpl.pos; PositionModifier mod = tpl.mod; TokPropList prop = tpl.prop;
        idx = getModifiedPosition(mod, idx);
        List<String> vals = getTokPropList(prop, idx);
        for (String val : vals) {
            feats.add(toFeat(tpl.name, val));
        }
    }

    /**
     * Gets features of the form: 
     *    path(lca(p,c),root).bc0+dir.noDup
     *    children(p).bc0.seq
     *    line(p,c).t.noDup
     *    
     * @param tpl Structured feature template.
     * @param pidx Token to which the parent position refers.
     * @param cidx Token to which the child position refers.
     * @param feats The feature list to which this will be added.
     */
    public void addListFeature(FeatTemplate3 tpl, int pidx, int cidx, List<Object> feats) {
        PositionList pl = tpl.pl; TokProperty prop = tpl.prop; boolean includeDir = tpl.includeDir; ListModifier lmod = tpl.lmod;
        Object feat;
        Collection<String> vals;
        switch (pl) {
        case CHILDREN_P: case NO_FAR_CHILDREN_P: case LINE_P_C:
            List<Integer> indices = getPositionList(pl, pidx, cidx);
            vals = getTokPropsForList(prop, indices);
            vals = getModifiedList(lmod, vals);
            feat = toFeat(tpl.name, vals);
            feats.add(feat);
            return;
        case PATH_P_C: case PATH_C_ROOT: case PATH_P_ROOT: case PATH_LCA_ROOT: 
            List<Pair<Integer, Dir>> path = getPath(pl, pidx, cidx);
            vals = getTokPropsForPath(prop, includeDir, path);
            vals = getModifiedList(lmod, vals);
            feat = toFeat(tpl.name, vals);
            feats.add(feat);
            return;
        default:
            throw new IllegalStateException();
        }
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

    private ArrayList<Integer> getPositionList(PositionList pl, int pidx, int cidx) {        
        FeaturizedToken ptok;
        switch (pl) {
        case CHILDREN_P: 
            ptok = getFeatTok(pidx);
            return ptok.getChildren();
        case NO_FAR_CHILDREN_P: 
            ptok = getFeatTok(pidx);
            return ptok.getNoFarChildren();
        case LINE_P_C: 
            FeaturizedTokenPair pair = getFeatTokPair(pidx, cidx);
            return pair.getLinePath();
        default:
            throw new IllegalStateException();
        }
    }
    
    private List<Pair<Integer, Dir>> getPath(PositionList pl, int pidx, int cidx) {
        FeaturizedTokenPair pair = getFeatTokPair(pidx, cidx);
        switch (pl) {
        case PATH_P_C:
            return pair.getDependencyPath();
        case PATH_C_ROOT:
            return pair.getDpPathArg();
        case PATH_P_ROOT:
            return pair.getDpPathPred();
        case PATH_LCA_ROOT:
            return pair.getDpPathShare();
        default:
            throw new IllegalStateException();
        }
    }
    
    public int getModifiedPosition(PositionModifier mod, int idx) {
        FeaturizedToken tok = null;
        if (mod != PositionModifier.BEFORE1 && mod != PositionModifier.AFTER1) {
            tok = getFeatTok(idx);
        }
        switch(mod) {
        case LOW_SV: case LOW_SN: case HIGH_SV: case HIGH_SN: 
            log.warn("Assuming Spanish when creating UNK feature.");
            break;
        default: break;
        }
        
        switch (mod) {
            // --------------------- Word ---------------------  
        case BEFORE1:
            return idx - 1;
        case AFTER1:
            return idx + 1;
            // --------------------- DepTree ---------------------  
        case HEAD:
            return tok.getParent();
        case LMD:
            // TODO: Implement this in FeaturizedToken.
            throw new IllegalStateException("not yet implemented");
        case RMD:
            // TODO: Implement this in FeaturizedToken.
            throw new IllegalStateException("not yet implemented");
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
    
    private List<String> getTokPropsForPath(TokProperty prop, boolean includeDir, List<Pair<Integer,Dir>> path) {
        List<String> props = new ArrayList<String>(path.size());
        for (Pair<Integer,Dir> edge : path) {
            props.add(getTokProp(prop, edge.get1()));
            if (includeDir) {
                props.add(edge.get2().name());
            }
        }
        return props;
    }

    private List<String> getTokPropsForList(TokProperty prop, List<Integer> indices) {
        List<String> props = new ArrayList<String>(indices.size());
        for (int idx : indices) {
            props.add(getTokProp(prop, idx));
        }
        return props;
    }

    private List<String> getTokPropList(TokPropList prop, int idx) {
        FeaturizedToken tok = getFeatTok(idx);
        switch (prop) {
        case MORPHO:
            return tok.getFeat();
        default:
            throw new IllegalStateException();
        }
    }
    
    private String getTokProp(TokProperty prop, int idx) {
        FeaturizedToken tok = getFeatTok(idx);
        switch (prop) {
        case WORD:
            return tok.getForm();
        case LC:
            //TODO: return tok.getFormLc();
            return tok.getForm().toLowerCase();
        case CHPRE5:
            String form = tok.getForm();
            if (form.length() > 5) {
                return form.substring(0, Math.max(form.length(), 5));    
            } else {
                return null;
            }            
        case LEMMA:
            return tok.getLemma();
        case POS:
            return tok.getPos();
        case BC0:
            //TODO: return tok.getBrownCluster(4);    
            throw new IllegalStateException();
        case BC1:
            //TODO: return tok.getBrownCluster();
            throw new IllegalStateException();
        case DEPREL:
            return tok.getDeprel();
        case MORPHO:
            return tok.getFeatStr();
        case UNK:
            log.warn("Assuming Spanish when creating UNK feature.");            
            return sig.getSignature(tok.getForm(), idx, "es");
        default:
            throw new IllegalStateException();
        }
    }
    
    public static <T> Collection<T> bag(Collection<T> elements) {
        // bag, which removes all duplicated strings and sort the rest
        return new TreeSet<T>(elements);
    }
    
    public static <T> ArrayList<T> noDup(Collection<T> elements) {
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
    
    private Object toFeat(String... vals) {
        return org.apache.commons.lang.StringUtils.join(vals, "_");
    }

    private Object toFeat(String name, Collection<String> vals) {
        return name + org.apache.commons.lang.StringUtils.join(vals, "_");
    }

    private Object toFeat(Object f1, Object f2) {
        if (f1 instanceof String && f2 instanceof String) {
            return f1 + "_" + f2;
        } else {
            return new Pair<Object,Object>(f1, f2);
        }
    }

}
