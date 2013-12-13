package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.util.collections.Lists;
import edu.stanford.nlp.util.StringUtils;

/**
 * Defines a 'little language' for structured feature templates.
 * 
 * @author mgormley
 */
public class TemplateLanguage {

    private TemplateLanguage() {
        // Private constructor.
    }
    
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
        MORPHO,
        // Not implemented:
        // CH, CHPRE_N, CHSUF_N;
    }

    /** Directed Edge Property. A mapping from a directed edge to a string. */
    public enum EdgeProperty {
        DIR, DR_DIR;
    }

    /** Position Modifier. A mapping from one position to another. */
    public enum PositionModifier {
        IDENTITY, BEFORE1, AFTER1,
        //
        HEAD, LNS, RNS, LMC, RMC, LNC, RNC,
        // Not implemented: LMD, RMD,         
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
    private static Map<Enum<?>, Description> descMap = new HashMap<Enum<?>, Description>();
    
    private static void desc(Enum<?> obj, String name, String description, AT... requiredLevels) {
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
        // TODO: 
        // desc(TokPropList.CH, "ch", "Each character of the word", AT.WORD);
        // desc(TokPropList.CHPRE_N, "chpre_n", "Character n-gram prefix", AT.WORD);
        // desc(TokPropList.CHSUF_N, "chsuf_n", "Character n-gram suffix", AT.WORD);
   
        /** Directed Edge Property. A mapping from a directed edge to a string. */
        desc(EdgeProperty.DIR, "dir", "Direction of an edge in a path", AT.DEP_TREE);
        desc(EdgeProperty.DR_DIR, "dr+dir", "Conjunction of dr and dir", AT.LABEL_DEP_TREE);

        /** Position Modifier. A mapping from one position to another. */
        desc(PositionModifier.IDENTITY, "w", "No modification", AT.WORD);
        desc(PositionModifier.BEFORE1, "w_{-1}", "1 before w", AT.WORD);
        desc(PositionModifier.AFTER1, "w_{1}", "1 after w", AT.WORD);
        //
        desc(PositionModifier.HEAD, "w_{head}", "Syntactic head of w", AT.DEP_TREE);
        desc(PositionModifier.LNS, "w_{lns}", "Left nearest sibling", AT.DEP_TREE);
        desc(PositionModifier.RNS, "w_{rns}", "Right nearest sibling", AT.DEP_TREE);
        desc(PositionModifier.LMC, "w_{lmc}", "Leftmost child", AT.DEP_TREE);
        desc(PositionModifier.RMC, "w_{rmc}", "Rightmost child", AT.DEP_TREE);
        desc(PositionModifier.LNC, "w_{lnc}", "Left nearest child", AT.DEP_TREE);
        desc(PositionModifier.RNC, "w_{rnc}", "Right nearest child", AT.DEP_TREE);
        // TODO:
        //desc(PositionModifier.LMD, "w_{lmd}", "Leftmost descendent", AT.DEP_TREE);
        //desc(PositionModifier.RMD, "w_{rmd}", "Rightmost descendent", AT.DEP_TREE);
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
        
        // Create the mapping of enums to their descriptions.
        for (Description d : desc) {
            descMap.put(d.getObj(), d);
        }
    }
    
    /** Feature function description. */
    public static class Description {
        private Enum<?> obj;
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
        private Description(Enum<?> obj, String name, String description, AT... requiredLevels) {
            this.obj = obj;
            this.name = name;
            this.description = description;
            this.requiredLevels = requiredLevels;
        }
        public Enum<?> getObj() {
            return obj;
        }
        public String getName() {
            return name;
        }
        public String getDescription() {
            return description;
        }
        public AT[] getRequiredLevels() {
            return requiredLevels;
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
        public abstract List<Enum<?>> getStructure();
    }
    
    /**
     * For feature templates of the form: 
     *     p.bc1
     *     c_{head}.dr
     *     first(t, NOUN, path(p, root)).bc0
     */
    public static class FeatTemplate1 extends FeatTemplate {
        public Position pos; 
        public PositionModifier mod; 
        public TokProperty prop;
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
        public List<Enum<?>> getStructure() {
            return Lists.getList(pos, mod, prop);
        }
    }
    
    /**
     * For feature templates of the form: 
     *     p.morpho
     * which extract multiple features of a single token.
     */
    public static class FeatTemplate2 extends FeatTemplate {
        public Position pos;
        public PositionModifier mod; 
        public TokPropList prop; 
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
        public List<Enum<?>> getStructure() {
            return Lists.getList(pos, mod, prop);
        }
    }

    /**
     * For feature templates of the form: 
     *    path(lca(p,c),root).bc0+dir.noDup
     *    children(p).bc0.seq
     *    line(p,c).t.noDup
     */
    public static class FeatTemplate3 extends FeatTemplate {
        public PositionList pl; 
        public TokProperty prop; 
        public boolean includeDir;
        public ListModifier lmod;
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
        public List<Enum<?>> getStructure() {
            if (includeDir) {
                return Lists.getList(pl, prop, EdgeProperty.DIR, prop);
            } else {
                return Lists.getList(pl, prop, prop);
            }
        }
    }
    
    /**
     * For bigram feature templates of the form:
     *     p.w+c_{-1}.bc0
     *     p.t+c.t
     */
    public static class BigramTemplate extends FeatTemplate {
        public FeatTemplate tpl1;
        public FeatTemplate tpl2;
        public BigramTemplate(FeatTemplate tpl1, FeatTemplate tpl2) {
            super(StringUtils.join(new String[]{tpl1.getName(), tpl2.getName()}, "+"));
            this.tpl1 = tpl1;
            this.tpl2 = tpl2;
        }
        public List<Enum<?>> getStructure() {
            List<Enum<?>> s = new ArrayList<Enum<?>>(tpl1.getStructure());
            s.addAll(tpl2.getStructure());
            return s;
        }
    }
    
    /* ---------------------------- Utilities for Checking Feature Template Sets -------------------- */
    
    public static boolean hasRequiredAnnotationTypes(SimpleAnnoSentence sent, Set<AT> types) {
        for (AT type : types) {
            if (!hasRequiredAnnotationType(sent, type)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean hasRequiredAnnotationType(SimpleAnnoSentence sent, AT type) {
        switch (type) {
        case WORD:
            return sent.getWords() != null;
        case LEMMA:
            return sent.getLemmas() != null;
        case POS:
            return sent.getPosTags() != null;
        case BROWN:
            return sent.getClusters() != null;
        case DEP_TREE:
            return sent.getParents() != null;
        case LABEL_DEP_TREE:
            return sent.getParents() != null && sent.getDeprels() != null;
        case MORPHO:
            return sent.getFeats() != null;
        default:
            throw new IllegalStateException();
        }
    }

    public static Set<AT> getRequiredAnnotationTypes(List<FeatTemplate> tpls) {
        HashSet<AT> types = new HashSet<AT>();
        for (FeatTemplate tpl : tpls) {
            for (Enum<?> obj : tpl.getStructure()) {
                for (AT type : descMap.get(obj).getRequiredLevels()) {
                    types.add(type);
                }
            }
        }
        return types;
    }
    
}
