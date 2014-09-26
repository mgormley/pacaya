package edu.jhu.nlp.features;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.util.collections.Lists;

/**
 * Defines a 'little language' for structured feature templates.
 * 
 * @author mgormley
 */
public class TemplateLanguage {
    
    private static final Logger log = Logger.getLogger(TemplateLanguage.class);

    private TemplateLanguage() {
        // Private constructor.
    }
    
    /* -------------------- Structures of the Little Language ---------------- */
        
    /** Word property. A mapping from a position to a string. */
    public enum TokProperty {
        INDEX, WORD, LEMMA, POS, CPOS, BC0, BC1, MORPHO, DEPREL, LC, UNK, CHPRE5, CAPITALIZED, WORD_TOP_N,
        //
        MORPHO1, MORPHO2, MORPHO3;        
    }

    /** Word property list expansion. A mapping from a position to a list of strings. */ 
    public enum TokPropList {
        EACH_MORPHO,
        // Not implemented:
        // CH, CHPRE_N, CHSUF_N;
    }

    /** Directed Edge Property. A mapping from a directed edge to a string. */
    public enum EdgeProperty {
        DIR, EDGEREL
    }

    /** Positions. */
    public enum Position {
        // Dependency parsing
        PARENT, CHILD, MODIFIER,
        // Constituency parsing
        RULE_START, RULE_MID, RULE_END,
        // Mention start, end (inclusive), and head positions
        M_1_START, M_1_END, M_1_HEAD,
        M_2_START, M_2_END, M_2_HEAD,
    }

    /** Position List. Mapping from one or two positions to a position list. */
    public enum PositionList {
        CHILDREN_P, NO_FAR_CHILDREN_P, CHILDREN_C, NO_FAR_CHILDREN_C,
        //
        LINE_P_C, LINE_RI_RK, BTWN_P_C, PATH_P_C, PATH_P_LCA, PATH_C_LCA, PATH_LCA_ROOT;

        public boolean isPath() {
            switch (this) {
            case PATH_P_C: case PATH_P_LCA: case PATH_C_LCA: case PATH_LCA_ROOT: return true;
            default: return false;
            }
        }
    }

    /** Position Modifier. A mapping from one position to another. */
    public enum PositionModifier {
        IDENTITY, BEFORE1, BEFORE2, AFTER1, AFTER2,
        //
        HEAD, LNS, RNS, LMC, RMC, LNC, RNC,
        //
        LOW_SV, LOW_SN, HIGH_SV, HIGH_SN,
    }

    /** List Modifiers. Mapping of a list of strings to a new list of strings. */
    public enum ListModifier {
        SEQ, BAG, NO_DUP, UNIGRAM, BIGRAM, TRIGRAM;
    }

    /**
     * Additional Features. Mapping from parent and child positions to a
     * feature.
     */
    public enum OtherFeat {
        RELATIVE, DISTANCE, GENEOLOGY, PATH_LEN, CONTINUITY, PATH_GRAMS, SENT_LEN,
        //
        RULE_IS_UNARY;
        // TODO: Not implemented:
        // PRED_VOICE_WORD_OR_POS,        
    }
    
    /** The pieces of a CNF rule. */
    public enum RulePiece {
        PARENT, LEFT_CHILD, RIGHT_CHILD
    }
    
    /** Symbol property. A mapping from a rule piece to a string. */
    public enum SymbolProperty {
        TAG, 
        // Not implemented: 
        // BASE_TAG, PARENT_TAG, BASE_PARENT_TAG
    }
    
    /** Named entity mentions. */
    public enum Mention {
        M_1, M_2,
    }
    
    /** Mention properties. */
    public enum MentionProperty {
        ENTITY_TYPE, PHRASE_TYPE,
    }
    
    /* -------------------- Descriptions of the Language Elements ---------------- */

    /**
     * Annotation Type. These describe which part of a AnnoSentence must
     * be present in order to utilize each structure.
     */
    public enum AT {
        WORD, PREFIX, LEMMA, POS, CPOS, BROWN, EMBED, MORPHO, CHUNKS, DEP_TREE, DEPREL, 
        DEP_EDGE_MASK, SRL_PRED_IDX, SRL, NARY_TREE, NER, NE_PAIRS, RELATIONS, REL_LABELS;
    }
        
    public static Description getDescByName(String name) {
        name = Description.normalizeName(name);
        return nameDescMap.get(name);
    }
    
    public static String getNameFromDesc(FeatTemplate tpl) {
        List<Enum<?>> struc = tpl.getStructure();
        StringBuilder name = new StringBuilder();
        for (int i=0; i<struc.size(); i++) {
            Enum<?> e = struc.get(i);
            if (e == null || e == PositionModifier.IDENTITY) {
                continue;
            }
            Description d = enumDescMap.get(e);
            if (d == null) {
                throw new IllegalStateException("Enum not found in map: " + e);
            }
            name.append(d.name);
            if (i < struc.size()-1) {
                name.append("(");
            }
        }
        for (int i=1; i<struc.size(); i++) {
            Enum<?> e = struc.get(i);
            if (e == null || e == PositionModifier.IDENTITY) {
                continue;
            }
            name.append(")");
        }
        return name.toString();
    }
    
    private static ArrayList<Description> desc = new ArrayList<Description>();
    private static Map<Enum<?>, Description> enumDescMap = new HashMap<Enum<?>, Description>();
    private static Map<String, Description> nameDescMap = new HashMap<String, Description>();
    
    private static void desc(Enum<?> obj, String name, String description, AT... requiredLevels) {
        desc.add(new Description(obj, name, description, requiredLevels));
    }
    
    static {
        /** Positions. */
        desc(Position.PARENT, "p", "Parent");
        desc(Position.CHILD, "c", "Child");
        desc(Position.MODIFIER, "m", "Modifier");
        desc(Position.RULE_START, "ri", "Rule start");
        desc(Position.RULE_MID, "rj", "Rule mid");
        desc(Position.RULE_END, "rk", "Rule end");
        desc(Position.M_1_START, "m1s", "Mention 1 start"); // TODO: Change to start(span(m1))?
        desc(Position.M_1_END, "m1e", "Mention 1 end");
        desc(Position.M_1_HEAD, "m1h", "Mention 1 head");
        desc(Position.M_2_START, "m2s", "Mention 2 start");
        desc(Position.M_2_END, "m2e", "Mention 2 end");
        desc(Position.M_2_HEAD, "m2h", "Mention 2 head");
        
        /** CNF Rule pieces. */
        desc(RulePiece.PARENT, "ruleP", "Rule parent");
        desc(RulePiece.LEFT_CHILD, "ruleLc", "Rule left child");
        desc(RulePiece.RIGHT_CHILD, "ruleRc", "Rule right child");
        
        /** Symbol property. A mapping from a rule piece to a string. */
        desc(SymbolProperty.TAG, "tag", "Symbol");
        // TODO: Add ATs
        //desc(SymbolProperty.BASE_TAG, "bTag", "Base symbol");
        //desc(SymbolProperty.PARENT_TAG, "pTag", "Parent symbol annotation");
        //desc(SymbolProperty.BASE_PARENT_TAG, "bpTag", "Base of parent symbol annotation");
        
        /** Named entity mentions. */
        desc(Mention.M_1, "m1", "Mention 1");
        desc(Mention.M_2, "m2", "Mention 2");
        
        /** Mention properties. */
        desc(MentionProperty.ENTITY_TYPE, "entityType", "Entity type");
        desc(MentionProperty.PHRASE_TYPE, "mentionType", "Phrase type");
        
        /** Word property. A mapping from a position to a string. */
        desc(TokProperty.WORD, "word", "Word", AT.WORD);
        desc(TokProperty.LEMMA, "lemma", "Lemma", AT.LEMMA);
        desc(TokProperty.POS, "pos", "POS Tag", AT.POS);
        desc(TokProperty.CPOS, "cpos", "Coarse POS Tag", AT.CPOS);
        desc(TokProperty.BC0, "bc0", "Coarse-grained Brown cluster", AT.BROWN);
        desc(TokProperty.BC1, "bc1", "Fine-grained Brown cluster", AT.BROWN);
        desc(TokProperty.MORPHO, "morpho", "Morphological features", AT.MORPHO);
        desc(TokProperty.DEPREL, "deprel", "Dependency relation to head", AT.DEPREL);
        desc(TokProperty.LC, "lc", "Lower-cased word", AT.WORD);
        desc(TokProperty.UNK, "unk", "Unknown word class", AT.WORD);
        desc(TokProperty.CHPRE5, "chpre5", "5-character prefix of a word", AT.WORD);
        desc(TokProperty.CAPITALIZED, "capitalized", "Whether this word starts with a capital letter", AT.WORD);
        desc(TokProperty.WORD_TOP_N, "wordTopN", "Word if it's in the top N", AT.WORD);
        desc(TokProperty.INDEX, "index", "The position itself", AT.WORD);
        
        desc(TokProperty.MORPHO1, "morpho1", "Morphological feature 1", AT.MORPHO);
        desc(TokProperty.MORPHO2, "morpho2", "Morphological feature 2", AT.MORPHO);
        desc(TokProperty.MORPHO3, "morpho3", "Morphological feature 3", AT.MORPHO);
        
        /** Word property list expansion. A mapping from a position to a list of strings. */ 
        desc(TokPropList.EACH_MORPHO, "eachmorpho", "Morphological features", AT.MORPHO);
        // TODO: 
        // desc(TokPropList.CH, "ch", "Each character of the word", AT.WORD);
        // desc(TokPropList.CHPRE_N, "chpre_n", "Character n-gram prefix", AT.WORD);
        // desc(TokPropList.CHSUF_N, "chsuf_n", "Character n-gram suffix", AT.WORD);
   
        /** Directed Edge Property. A mapping from a directed edge to a string. */
        desc(EdgeProperty.DIR, "dir", "Direction of an edge in a path", AT.DEP_TREE);
        desc(EdgeProperty.EDGEREL, "edgerel", "Dependency relation of an edge in a path", AT.DEP_TREE);

        /** Position Modifier. A mapping from one position to another. */
        desc(PositionModifier.IDENTITY, "0", "No modification", AT.WORD);
        desc(PositionModifier.BEFORE1, "-1", "1 before w", AT.WORD);
        desc(PositionModifier.BEFORE2, "-2", "2 before w", AT.WORD);
        desc(PositionModifier.AFTER1, "1", "1 after w", AT.WORD);
        desc(PositionModifier.AFTER2, "2", "2 after w", AT.WORD);
        //
        desc(PositionModifier.HEAD, "head", "Syntactic head of w", AT.DEP_TREE);
        desc(PositionModifier.LNS, "lns", "Left nearest sibling", AT.DEP_TREE);
        desc(PositionModifier.RNS, "rns", "Right nearest sibling", AT.DEP_TREE);
        desc(PositionModifier.LMC, "lmc", "Leftmost child", AT.DEP_TREE);
        desc(PositionModifier.RMC, "rmc", "Rightmost child", AT.DEP_TREE);
        desc(PositionModifier.LNC, "lnc", "Left nearest child", AT.DEP_TREE);
        desc(PositionModifier.RNC, "rnc", "Right nearest child", AT.DEP_TREE);
        //
        //desc(PositionModifier.LOW_SV, "first(pos, VERB, path(w, root))", "Low support Verb", AT.POS, AT.DEP_TREE);
        //desc(PositionModifier.LOW_SN, "first(pos, NOUN, path(w, root))", "Low support Noun", AT.POS, AT.DEP_TREE);
        //desc(PositionModifier.HIGH_SV, "first(pos, VERB, path(root, w))", "High support Verb", AT.POS, AT.DEP_TREE);
        //desc(PositionModifier.HIGH_SN, "first(pos, NOUN, path(root, w))", "High support Noun", AT.POS, AT.DEP_TREE);
        desc(PositionModifier.LOW_SV, "lowsv", "Low support Verb", AT.POS, AT.DEP_TREE);
        desc(PositionModifier.LOW_SN, "lowsn", "Low support Noun", AT.POS, AT.DEP_TREE);
        desc(PositionModifier.HIGH_SV, "highsv", "High support Verb", AT.POS, AT.DEP_TREE);
        desc(PositionModifier.HIGH_SN, "highsn", "High support Noun", AT.POS, AT.DEP_TREE);

        /** Position List. Mapping from one or two positions to a position list. */
        desc(PositionList.LINE_P_C, "line(p,c)", "horizontal path between p and c (inclusive)", AT.WORD);
        desc(PositionList.LINE_RI_RK, "line(ri,rk)", "horizontal path between ri and rj (inclusive)", AT.WORD);
        desc(PositionList.BTWN_P_C, "btwn(p,c)", "Horizontal path between p and c (exclusive)", AT.WORD);
        desc(PositionList.CHILDREN_P, "children(p)", "Children of p", AT.DEP_TREE);
        desc(PositionList.NO_FAR_CHILDREN_P, "noFarChildren(p)", "Children without the leftmost or rightmost included", AT.DEP_TREE);
        desc(PositionList.CHILDREN_C, "children(c)", "Children of c", AT.DEP_TREE);
        desc(PositionList.NO_FAR_CHILDREN_C, "noFarChildren(c)", "Children without the leftmost or rightmost included", AT.DEP_TREE);
        desc(PositionList.PATH_P_C, "path(p,c)", "from parent to child", AT.DEP_TREE);
        desc(PositionList.PATH_P_LCA, "path(p,lca(p,c))", "from parent to least-common-ancestor", AT.DEP_TREE);
        desc(PositionList.PATH_C_LCA, "path(c,lca(p,c))", "from child to least-common-ancestor", AT.DEP_TREE);
        desc(PositionList.PATH_LCA_ROOT, "path(lca(p,c),root)", "from least-common-ancestor to root ", AT.DEP_TREE);

        /** List Modifiers. Mapping of a list of strings to a new list of strings. */
        desc(ListModifier.SEQ, "seq", "Identity function.");
        desc(ListModifier.BAG, "bag", "List to set.");
        desc(ListModifier.NO_DUP, "noDup", "Unix “uniq” on original list.");
        desc(ListModifier.UNIGRAM, "1gram", "Creates a separate feature for each element of the list.");
        desc(ListModifier.BIGRAM, "2gram", "Creates a separate feature for each bigram in the list.");
        desc(ListModifier.TRIGRAM, "3gram", "Creates a separate feature for each trigram in the list.");
        
        /** Additional Features. Mapping from parent and child positions to a feature. */
        desc(OtherFeat.RELATIVE, "relative(p,c)", "Relative position of p and c: before, after, on.", AT.WORD);
        desc(OtherFeat.DISTANCE, "distance(p,c)", "Distance binned into greater than: 2, 5, 10, 20, 30, or 40", AT.WORD);
        desc(OtherFeat.GENEOLOGY, "geneology(p,c)", "geneological relationship between p and c in a syntactic parse: parent, child, ancestor, descendent.", AT.DEP_TREE);
        desc(OtherFeat.PATH_LEN, "len(path(p,c))", "Path length binned into greater than: 2, 5, 10, 20, 30, or 40", AT.DEP_TREE);
        desc(OtherFeat.PATH_GRAMS, "pathGrams", "$1,2,3$-gram path features of words/POS tags", AT.DEP_TREE);
        desc(OtherFeat.CONTINUITY, "continuity(path(p,c))", "The number of non-consecutive token pairs  in a predicate-argument path.", AT.DEP_TREE);
        desc(OtherFeat.SENT_LEN, "sentlen", "Sentence length binned into greater than: 2, 5, 10, 20, 30, or 40", AT.DEP_TREE);
        desc(OtherFeat.RULE_IS_UNARY, "ruleIsUnary", "Whether a rule is unary");
        // TODO:
        //desc(OtherFeat.PRED_VOICE_WORD_OR_POS, "p.voice+a.word / p.voice+a.t", "The predicate voice and the  word/POS of the argument.", AT.LABEL_DEP_TREE);
        
        for (Description d : desc) {
            // Create the mapping of enums to their descriptions.
            enumDescMap.put(d.getObj(), d);
            // Create the mapping of names to descriptions.
            if (enumDescMap.containsKey(d.getName())) {
                log.warn("Multiple structures with the same name: " + d.getName());
            }
            nameDescMap.put(d.getName(), d);
        }        
    }
    
    /** Feature function description. */
    public static class Description {
        public static final Pattern whitespace = Pattern.compile("\\s+");

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
            this.name = normalizeName(name);
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
        public static String normalizeName(String name) {
            // Remove whitespace from names.
            name = whitespace.matcher(name).replaceAll("");
            return name;
        }
        public String toString() {
            return String.format("%-17s %-27s %-20s %-20s", obj, name, Arrays.toString(requiredLevels), description);
        }
    }
    
    /* -------------------- Structure Feature Templates ---------------- */

    public static final String TEMPLATE_SEP = "+";
    public static final String STRUCTURE_SEP = ".";

    public static abstract class FeatTemplate implements Serializable {
        protected String name;
        private static final long serialVersionUID = 1L;
        public FeatTemplate() { }
        public String getName() {
            if (name == null) {
                name = getNameFromDesc(this);
            }
            return name;
        }
        public abstract List<Enum<?>> getStructure();
        public String toString() {
            return getName();
        }
        public int hashCode() {
            return getStructure().hashCode();
        }
        public boolean equals(Object o) {
            if (o instanceof FeatTemplate) {
                return this.getStructure().equals(((FeatTemplate)o).getStructure());
            }
            return false;
        }
    }
    
    /**
     * For feature templates of the form: 
     *     p.bc1
     *     c_{head}.dr
     *     first(t, NOUN, path(p, root)).bc0
     */
    public static class FeatTemplate1 extends FeatTemplate {
        private static final long serialVersionUID = 1L;
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
            this.pos = pos;
            this.mod = mod;
            this.prop = prop;
        }
        public List<Enum<?>> getStructure() {
            return Lists.getList(prop, mod, pos);
        }
    }
    
    /**
     * For feature templates of the form: 
     *     p.morpho
     * which extract multiple features of a single token.
     */
    public static class FeatTemplate2 extends FeatTemplate {
        private static final long serialVersionUID = 1L;
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
            this.pos = pos;
            this.mod = mod;
            this.prop = prop;
        }
        public List<Enum<?>> getStructure() {
            return Lists.getList(prop, mod, pos);
        }
    }

    /**
     * For feature templates of the form: 
     *    path(lca(p,c),root).bc0+dir.noDup
     *    children(p).bc0.seq
     *    line(p,c).t.noDup
     */
    public static class FeatTemplate3 extends FeatTemplate {
        private static final long serialVersionUID = 1L;
        public PositionList pl; 
        public TokProperty prop; 
        public EdgeProperty eprop;
        public ListModifier lmod;
        /**
         * Constructor. 
         * @param pl Position list which is a function of the parent/child positions.
         * @param prop (OPTIONAL) Property to extract from the position list or null.
         * @param eprop (OPTIONAL) Edge property to extract from the path or null.
         * @param lmod List modifier.
         */
        public FeatTemplate3(PositionList pl, TokProperty prop, EdgeProperty eprop, ListModifier lmod) {
            this.pl = pl;
            this.prop = prop;
            this.eprop = eprop;
            this.lmod = lmod;
        }
        public List<Enum<?>> getStructure() {
            return Lists.getList(prop, eprop, lmod, pl);
        }
    }
    
    /**
     * For feature templates of the form: 
     *     ruleP.tag
     *     ruleLc.bTag
     */
    public static class FeatTemplate4 extends FeatTemplate {
        private static final long serialVersionUID = 1L;
        public RulePiece piece; 
        public SymbolProperty prop;
        /**
         * Constructor.
         * @param pos The piece of the rule to extract.
         * @param prop Property to extract from the rule symbol.
         */
        public FeatTemplate4(RulePiece piece, SymbolProperty prop) {
            this.piece = piece;
            this.prop = prop;
        }
        public List<Enum<?>> getStructure() {
            return Lists.getList(prop, piece);
        }
    }
    
    /**
     * For standalone feature templates of the form: 
     *    DepSubCat
     *    geneology(p,c)
     */
    public static class FeatTemplate0 extends FeatTemplate {
        private static final long serialVersionUID = 1L;
        public OtherFeat feat;
        /**
         * Constructor. 
         * @param feat The special feature.
         */
        public FeatTemplate0(OtherFeat feat) {
            this.feat = feat;
        }
        public List<Enum<?>> getStructure() {
            //return (List<Enum<?>>) Lists.getList(feat);
            List<Enum<?>> s = new ArrayList<Enum<?>>();
            s.add(feat);
            return s;
        }
    }
        
    /**
     * For n-gram feature templates of the form:
     *     p.w + c_{-1}.bc0
     *     p.t + c.t
     *     p.t + c.t + p.w
     */
    public static class JoinTemplate extends FeatTemplate {
        private static final long serialVersionUID = 1L;
        public FeatTemplate[] tpls;
        public JoinTemplate(FeatTemplate... tpls) {
            if (tpls.length < 2) {
                throw new IllegalStateException("JoinTemplates must consist of 2 or more templates: " + Arrays.toString(tpls));
            }
            this.tpls = tpls;
        }
        public List<Enum<?>> getStructure() {
            List<Enum<?>> s = new ArrayList<Enum<?>>();
            for (FeatTemplate tpl : tpls) {
                s.addAll(tpl.getStructure());
            }
            return s;
        }
        public String getName() {
            if (name == null) {
                name = StringUtils.join(toNames(tpls), " " + TEMPLATE_SEP + " ");
            }
            return name;
        }
        private static String[] toNames(FeatTemplate[] tpls) {
            String[] names = new String[tpls.length];
            for (int i=0; i<tpls.length; i++) {
                names[i] = tpls[i].getName();                
            }
            return names;
        }
    }
    
    /* -------------------- Utilities for Checking Feature Template Sets ---------------- */
    
    public static boolean hasRequiredAnnotationTypes(AnnoSentence sent, List<FeatTemplate> tpls) {
        return hasRequiredAnnotationTypes(sent, getRequiredAnnotationTypes(tpls));
    }
    
    public static boolean hasRequiredAnnotationTypes(AnnoSentence sent, Set<AT> types) {
        for (AT type : types) {
            if (!hasRequiredAnnotationType(sent, type)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean hasRequiredAnnotationType(AnnoSentence sent, AT type) {
        return sent.hasAt(type);
    }


    public static void assertRequiredAnnotationTypes(AnnoSentence sent, List<FeatTemplate> tpls) {
        assertRequiredAnnotationTypes(sent, getRequiredAnnotationTypes(tpls));
    }
    
    public static void assertRequiredAnnotationTypes(AnnoSentence sent, Set<AT> types) {
        for (AT type : types) {
            assertRequiredAnnotationType(sent, type);
        }
    }
    
    public static void assertRequiredAnnotationType(AnnoSentence sent, AT type) {
        if (!hasRequiredAnnotationType(sent, type)) {
            throw new IllegalStateException("Missing required annotation type: " + type);
        }        
    }
    
    public static Set<AT> getRequiredAnnotationTypes(List<FeatTemplate> tpls) {
        HashSet<AT> types = new HashSet<AT>();
        for (FeatTemplate tpl : tpls) {
            for (Enum<?> obj : tpl.getStructure()) {
                if (obj != null) {
                    for (AT type : enumDescMap.get(obj).getRequiredLevels()) {
                        types.add(type);
                    }
                }
            }
        }
        return types;
    }
    
    public static void main(String[] args) {
        for (Description d : desc) {
            System.out.println(d);
        }
    }

    /** Filters out feature templates requiring the given annotation type. */
    public static List<FeatTemplate> filterOutRequiring(List<FeatTemplate> tpls, AT type) {
        ArrayList<FeatTemplate> tpls2 = new ArrayList<FeatTemplate>();
        for (FeatTemplate tpl : tpls) {
            if (!TemplateLanguage.getRequiredAnnotationTypes(Lists.getList(tpl)).contains(type)) {
                tpls2.add(tpl);
            }
        }
        return tpls2;
    }

    /** Filters out feature templates which contain the specified enum. */
    public static ArrayList<FeatTemplate> filterOutFeats(ArrayList<FeatTemplate> tpls, Enum<?> enumMatch) {
        ArrayList<FeatTemplate> tplsNew = new ArrayList<FeatTemplate>();
        for (FeatTemplate tpl : tpls) {
            boolean keep = true;
            for (Enum<?> e : tpl.getStructure()) {
                if (e == enumMatch) {
                    keep = false;
                }
            }
            if (keep) {
                tplsNew.add(tpl);
            }
        }
        return tplsNew;
    }
    
}
