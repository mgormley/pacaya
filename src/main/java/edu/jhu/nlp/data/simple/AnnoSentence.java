package edu.jhu.nlp.data.simple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.jhu.nlp.data.DepEdgeMask;
import edu.jhu.nlp.data.DepTree;
import edu.jhu.nlp.data.DepTree.Dir;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.NerMentions;
import edu.jhu.nlp.data.RelationMentions;
import edu.jhu.nlp.data.Span;
import edu.jhu.nlp.data.conll.SrlGraph;
import edu.jhu.nlp.data.conll.SrlGraph.SrlPred;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.parse.cky.data.NaryTree;
import edu.jhu.prim.arrays.IntArrays;
import edu.jhu.prim.set.IntHashSet;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.collections.Lists;

/**
 * Simple representation of a single sentence with many annotations.
 * 
 * This representation only uses strings, without String objects or Alphabet objects.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class AnnoSentence {

    // TODO: maybe change this to something like underscore?
    private static final String SPAN_STR_SEP = " ";
    
    private List<String> words;
    // 5-gram prefix if the word is longer than 5 characters.
    private List<String> prefixes;
    private List<String> lemmas;
    private List<String> posTags;
    private List<String> cposTags;
    private List<String> clusters;
    private List<double[]> embeds;
    private List<List<String>> feats;
    private List<String> deprels;
    /**
     * Internal representation of a dependency parse: parents[i] gives the index
     * of the parent of the word at index i. The Wall node has index -1. If a
     * word has no parent, it has index -2 (e.g. if punctuation was not marked
     * with a head).
     */
    private int[] parents;
    private DepEdgeMask depEdgeMask;
    private IntHashSet knownPreds;
    // TODO: This should be broken into semantic-roles and word senses.
    private SrlGraph srlGraph;
    /** Constituency parse. */
    private NaryTree naryTree;
    // The standard set of named entities.
    private NerMentions namedEntities;
    // Pairs of named entities to be considered for relation extraction.
    // This set could be all pairs, all ordered pairs, or some other definition.
    private List<Pair<NerMention,NerMention>> nePairs;
    // Labels for the pairs of named entities.
    private List<String> relLabels;
    // The standard set of relation mentions.
    private RelationMentions relations;
    
    /** The original object (e.g. CoNLL09Sentence) used to create this sentence. */
    private Object sourceSent;
    
    public AnnoSentence() {

    }

    /**
     * Fairly deep copy constructor. Everything is deeply copied except for the
     * source sentence and the SRL graph, the features, and the constituency parse.
     */
    @Deprecated
    public AnnoSentence getFairlyDeepCopy() {
        AnnoSentence newSent = new AnnoSentence();
        newSent.words = Lists.copyOf(this.words);
        newSent.prefixes = Lists.copyOf(this.prefixes);
        newSent.lemmas = Lists.copyOf(this.lemmas);
        newSent.posTags = Lists.copyOf(this.posTags);
        newSent.cposTags = Lists.copyOf(this.cposTags);
        newSent.clusters = Lists.copyOf(this.clusters);
        newSent.embeds = Lists.copyOf(this.embeds);
        newSent.deprels = Lists.copyOf(this.deprels);
        newSent.parents = IntArrays.copyOf(this.parents);
        newSent.depEdgeMask = (this.depEdgeMask == null) ? null : new DepEdgeMask(this.depEdgeMask);
        newSent.knownPreds = (this.knownPreds == null) ? null : new IntHashSet(this.knownPreds);
        newSent.namedEntities = new NerMentions(this.namedEntities);
        newSent.nePairs = Lists.copyOf(nePairs);
        newSent.relLabels = Lists.copyOf(relLabels);
        newSent.relations = new RelationMentions(this.relations);
        newSent.sourceSent = this.sourceSent;
        // TODO: this should be a deep copy.
        newSent.feats = Lists.copyOf(this.feats);
        // TODO: this should be a deep copy.
        newSent.srlGraph = this.srlGraph;
        // TODO: this should be a deep copy.
        newSent.naryTree = this.naryTree;
        return newSent;
    }
    
    public AnnoSentence getShallowCopy() {
        AnnoSentence newSent = new AnnoSentence();
        for (AT at : AT.values()) {
        	copyShallow(this, newSent, at);
        }
        return newSent;
    }

    public static void copyShallow(AnnoSentence src, AnnoSentence dest, AT at) {
        switch (at) {
        case WORD: dest.words = src.words; break;
        case PREFIX: dest.prefixes = src.prefixes; break;
        case LEMMA: dest.lemmas = src.lemmas; break;
        case POS: dest.posTags = src.posTags; break;
        case CPOS: dest.cposTags = src.cposTags; break;
        case BROWN: dest.clusters = src.clusters; break;
        case EMBED: dest.embeds = src.embeds; break;
        case MORPHO: dest.feats = src.feats; break;
        case DEP_TREE: dest.parents = src.parents; break;
        case DEPREL: dest.deprels = src.deprels; break;
        case DEP_EDGE_MASK: dest.depEdgeMask = src.depEdgeMask; break;
        case SRL_PRED_IDX: dest.knownPreds = src.knownPreds; break;
        case SRL: dest.srlGraph = src.srlGraph; break;
        case NARY_TREE: dest.naryTree = src.naryTree; break;
        case NER: dest.namedEntities = src.namedEntities; break;
        case NE_PAIRS: dest.nePairs = src.nePairs; break;
        case REL_LABELS: dest.relLabels = src.relLabels; break;
        case RELATIONS: dest.namedEntities = src.namedEntities; break;
        default: throw new RuntimeException("not implemented for " + at);
        }
    }

    public void removeAts(List<AT> removeAts) {
        for (AT at : removeAts) {
            removeAt(at);
        }
    }

    public void removeAt(AT at) {
        switch (at) {
        case WORD: this.words = null; break;
        case PREFIX: this.prefixes = null; break;
        case LEMMA: this.lemmas = null; break;
        case POS: this.posTags = null; break;
        case CPOS: this.cposTags = null; break;
        case BROWN: this.clusters = null; break;
        case EMBED: this.embeds = null; break;
        case MORPHO: this.feats = null; break;
        case DEP_TREE: this.parents = null; break; // TODO: Should DEP_TREE also remove the labels? Not clear.
        case DEPREL: this.deprels = null; break;
        case DEP_EDGE_MASK: this.depEdgeMask = null; break;
        case SRL_PRED_IDX: this.knownPreds = null; break;
        case SRL: this.srlGraph = null; break;
        case NARY_TREE: this.naryTree = null; break;
        case NER: this.namedEntities = null; break;
        case NE_PAIRS: this.nePairs = null; break;
        case REL_LABELS: this.relLabels = null; break;
        case RELATIONS: this.relations = null; break;
        default: throw new RuntimeException("not implemented for " + at);
        }
    }
    
    public boolean hasAt(AT at) {
        switch (at) {
        case WORD: return this.words != null;
        case PREFIX: return this.prefixes != null;
        case LEMMA: return this.lemmas != null;
        case POS: return this.posTags != null;
        case CPOS: return this.cposTags != null;
        case BROWN: return this.clusters != null;
        case EMBED: return this.embeds != null;
        case MORPHO: return this.feats != null;
        case DEP_TREE: return this.parents != null;
        case DEPREL: return this.deprels != null;
        case DEP_EDGE_MASK: return this.depEdgeMask != null;
        case SRL_PRED_IDX: return this.knownPreds != null;
        case SRL: return this.srlGraph != null;
        case NARY_TREE: return this.naryTree != null;
        case NER: return this.namedEntities != null;
        case NE_PAIRS: return this.nePairs != null;
        case REL_LABELS: return this.relLabels != null;
        case RELATIONS: return this.relations != null;        
        default: throw new RuntimeException("not implemented for " + at);
        }
    }
    
    public void intern() {
        Lists.intern(words);
        Lists.intern(prefixes);
        Lists.intern(lemmas);
        Lists.intern(posTags);
        Lists.intern(cposTags);
        Lists.intern(clusters);
        if (feats != null) {
            for (int i=0; i<feats.size(); i++) {
                Lists.intern(feats.get(i));
            }
        }
        Lists.intern(deprels);        
        if (naryTree != null) {
            naryTree.intern();
        }
        if (namedEntities != null) {
            namedEntities.intern();
        }
        // TODO: Lists.intern(nePairs);
        Lists.intern(relLabels);
        if (relations != null) {
            relations.intern();
        }
        // TODO: this.srlGraph.intern();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        appendIfNotNull(sb, "words", words);
        appendIfNotNull(sb, "prefixes", prefixes);
        appendIfNotNull(sb, "lemmas", lemmas);
        appendIfNotNull(sb, "tags", posTags);
        appendIfNotNull(sb, "cposTags", cposTags);
        appendIfNotNull(sb, "clusters", clusters);
        appendIfNotNull(sb, "embeds", embeds);
        appendIfNotNull(sb, "feats", feats);
        if (parents != null) {
            sb.append("parents=");
            sb.append(Arrays.toString(parents));
            sb.append(",\n");
        }
        appendIfNotNull(sb, "deprels", deprels);
        appendIfNotNull(sb, "depEdgeMask", depEdgeMask);
        appendIfNotNull(sb, "srlGraph", srlGraph);
        appendIfNotNull(sb, "knownPreds", knownPreds);
        appendIfNotNull(sb, "naryTree", naryTree);
        appendIfNotNull(sb, "namedEntities", namedEntities);
        if (namedEntities != null) { appendIfNotNull(sb, "namedEntities (context)", namedEntities.toString(words)); }
        appendIfNotNull(sb, "nePairs", nePairs);
        appendIfNotNull(sb, "relLabels", relLabels);
        appendIfNotNull(sb, "relations", relations);
        if (relations != null) { appendIfNotNull(sb, "relations (context)", relations.toString(words)); }
        appendIfNotNull(sb, "sourceSent", sourceSent);
        sb.append("]");
        return sb.toString();
    }

    private void appendIfNotNull(StringBuilder sb, String name, Object l) {
        if (l != null) {
            sb.append(name);
            sb.append("=");
            sb.append(l);
            sb.append(",\n");
        }
    }
    
    /* -------------------------------- Interesting getters / setters ---------------------------- */
    
    /** Gets the i'th word as a String. */
    public String getWord(int i) {
        return words.get(i);
    }
    
    /** Gets the i'th prefix of 5 characters as a String. */
    public String getPrefix(int i) {
        return prefixes.get(i);
    }

    /** Gets the i'th POS tag as a String. */
    public String getPosTag(int i) {
        return posTags.get(i);
    }
    
    /** Gets the i'th Coarse POS tag as a String. */
    public String getCposTag(int i) {
        return cposTags.get(i);
    }

    /** Gets the i'th Distributional Similarity Cluster ID as a String. */
    public String getCluster(int i) {
        return clusters.get(i);
    }
    
    public double[] getEmbed(int i) {
        return embeds.get(i);
    }
    
    /** Gets the i'th lemma as a String. */
    public String getLemma(int i) {
        return lemmas.get(i);
    }
    
    /** Gets the index of the parent of the i'th word. */
    public int getParent(int i) {
        return parents[i];
    }

    /** Returns whether the corresponding dependency arc should be pruned. */
    public boolean isDepEdgePruned(int parent, int child) {
        return depEdgeMask.isPruned(parent, child);
    }
    
    /** Gets the features (e.g. morphological features) of the i'th word. */
    public List<String> getFeats(int i) {
        return feats.get(i);
    }

    /** Gets the dependency relation label for the arc from the i'th word to its parent. */
    public String getDeprel(int i) {
        // TODO: Decide whether we should always return null for these sorts of get calls.
        if (deprels == null) { return null; }
        return deprels.get(i);
    }
        
    /**
     * Gets a list of words corresponding to a token span.
     */
    public List<String> getWords(Span span) {
        return getSpan(words, span);
    }
    
    /**
     * Gets a list of words corresponding to a token span.
     */
    public List<String> getPrefixes(Span span) {
        return getSpan(prefixes, span);
    }

    /**
     * Gets a list of parent indices corresponding to a token span.
     */
    public List<Integer> getParents(Span span) {
        return getSpan(parents, span);
    }
    
    /**
     * Gets a list of POS tags corresponding to a token span.
     */
    public List<String> getPosTags(Span span) {
        return getSpan(posTags, span);
    }
    
    /**
     * Gets a list of coarse POS tags corresponding to a token span.
     */
    public List<String> getCposTags(Span span) {
        return getSpan(cposTags, span);
    }
    
    /**
     * Gets a list of Distributional Similarity Cluster IDs corresponding to a token span.
     */
    public List<String> getClusters(Span span) {
        return getSpan(clusters, span);
    }
    
    /**
     * Gets a list of word embeddings corresponding to a token span.
     */
    public List<double[]> getEmbeds(Span span) {
        return getSpan(embeds, span);
    }

    /**
     * Gets a list of lemmas corresponding to a token span.
     */
    public List<String> getLemmas(Span span) {
        return getSpan(lemmas, span);
    }    
    
    /**
     * Gets a list of word/POS tags corresponding to a token span.
     */
    public List<String> getWordPosTags(Span span) {
        assert (span != null);
        List<String> list = new ArrayList<String>();
        for (int i = span.start(); i < span.end(); i++) {
            list.add(words.get(i) + "/" + posTags.get(i));
        }
        return list;
    }
    
    /**
     * Gets a single string representing the words in a given token span.
     * 
     * @param span
     */
    public String getWordsStr(Span span) {
        return getSpanStr(words, span);
    }
    
    /**
     * Gets a single string representing the words in a given token span.
     * 
     * @param span
     */
    public String getPrefixesStr(Span span) {
        return getSpanStr(prefixes, span);
    }

    /**
     * Gets a single string representing the POS tags in a given token span.
     * 
     * @param span
     */
    public String getPosTagsStr(Span span) {
        return getSpanStr(posTags, span);
    }

    /**
     * Gets a single string representing the coarse POS tags in a given token span.
     * 
     * @param span
     */
    public String getCposTagsStr(Span span) {
        return getSpanStr(cposTags, span);
    }
    
    /**
     * Gets a single string representing the Distributional Similarity Cluster IDs in a given token span.
     * 
     * @param span
     */
    public String getClustersStr(Span span) {
        return getSpanStr(clusters, span);
    }

    /**
     * Gets a single string representing the lemmas in a given token span.
     * 
     * @param span
     */
    public String getLemmasStr(Span span) {
        return getSpanStr(lemmas, span);
    }

    /**
     * Gets a single string representing the Word/POS in a given token span.
     * 
     * @param span
     */
    public String getWordPosTagsStr(Span span) {
        assert (span != null);
        StringBuilder sb = new StringBuilder();
        for (int i = span.start(); i < span.end(); i++) {
            if (i > span.start()) {
                sb.append(SPAN_STR_SEP);
            }
            sb.append(words.get(i));
            sb.append("/");
            sb.append(posTags.get(i));
        }
        return sb.toString();
    }
    
    // TODO: Consider moving this to LabelSequence.
    private static <T> List<T> getSpan(List<T> seq, Span span) {
        assert (span != null);
        List<T> list = new ArrayList<>();
        for (int i = span.start(); i < span.end(); i++) {
            list.add(seq.get(i));
        }
        return list;
    }

    private static List<Integer> getSpan(int[] parents, Span span) {
        assert (span != null);
        List<Integer> list = new ArrayList<Integer>();
        for (int i = span.start(); i < span.end(); i++) {
            list.add(i);
        }
        return list;
    }

    
    // TODO: Consider moving this to LabelSequence.
    private static String getSpanStr(List<String> seq, Span span) {
        assert (span != null);
        StringBuilder sb = new StringBuilder();
        for (int i = span.start(); i < span.end(); i++) {
            if (i > span.start()) {
                sb.append(SPAN_STR_SEP);
            }
            sb.append(seq.get(i));
        }
        return sb.toString();
    }
    
    /**
     * Gets the shortest dependency path between two tokens.
     * 
     * <p>
     * For the tree: x0 <-- x1 --> x2, represented by parents=[1, -1, 1] the
     * dependency path from x0 to x2 would be a list [(0, UP), (1, DOWN)]
     * </p>
     * 
     * <p>
     * See DepTreeTest for examples.
     * </p>
     * 
     * @param start The position of the start token.
     * @param end The position of the end token.
     * @return The path as a list of pairs containing the word positions and the
     *         direction of the edge, inclusive of the start position and
     *         exclusive of the end.
     */
    public List<Pair<Integer, Dir>> getDependencyPath(int start, int end) {
        return DepTree.getDependencyPath(start, end, parents);
    }
    
    public int size() {
        return words.size();
    }
    
    public boolean isKnownPred(int i) {
        return knownPreds.contains(i);
    }
    
    public void setKnownPredsFromSrlGraph() {
        if (srlGraph == null) {
            throw new IllegalStateException("This can only be called if srlGraph is non-null.");
        }
        knownPreds = new IntHashSet();
        // All the "Y"s
        for (SrlPred pred : srlGraph.getPreds()) {
            knownPreds.add(pred.getPosition());
        }
    }
    
    /* ----------- Getters/Setters for internal storage ------------ */
        
    public List<String> getWords() {
        return words;
    }

    public void setWords(List<String> words) {
        this.words = words;
    }
    
    public List<String> getPrefixes() {
        return prefixes;
    }

    public void setPrefixes(List<String> prefixes) {
        this.prefixes = prefixes;
    }

    public List<String> getLemmas() {
        return lemmas;
    }

    public void setLemmas(List<String> lemmas) {
        this.lemmas = lemmas;
    }

    public List<String> getPosTags() {
        return posTags;
    }

    public void setPosTags(List<String> posTags) {
        this.posTags = posTags;
    }
    
    public List<String> getCposTags() {
        return cposTags;
    }

    public void setCposTags(List<String> cposTags) {
        this.cposTags = cposTags;
    }

    public List<String> getClusters() {
        return clusters;
    }

    public void setClusters(List<String> clusters) {
        this.clusters = clusters;
    }
        
    public List<double[]> getEmbeds() {
        return embeds;
    }

    public void setEmbeds(List<double[]> embeds) {
        this.embeds = embeds;
    }

    public int[] getParents() {
        return parents;
    }

    public void setParents(int[] parents) {
        this.parents = parents;
    }

    public DepEdgeMask getDepEdgeMask() {
        return depEdgeMask;
    }

    public void setDepEdgeMask(DepEdgeMask depEdgeMask) {
        this.depEdgeMask = depEdgeMask;
    }

    public IntHashSet getKnownPreds() {
        return knownPreds;
    }
    
    public void setKnownPreds(IntHashSet knownPreds) {
        this.knownPreds = knownPreds;
    }

    public SrlGraph getSrlGraph() {
        return srlGraph;
    }
    
    /** Sets the SRL graph and also the known predicate positions. */
    public void setSrlGraph(SrlGraph srlGraph) {
        this.srlGraph = srlGraph;
        this.setKnownPredsFromSrlGraph();
    }
    
    public List<String> getDeprels() {
        return deprels;
    }

    public void setDeprels(List<String> deprels) {
        this.deprels = deprels;
    }

    public List<List<String>> getFeats() {
        return feats;
    }

    public void setFeats(List<List<String>> feats) {
        this.feats = feats;
    }
    
    public NaryTree getNaryTree() {
        return naryTree;
    }

    public void setNaryTree(NaryTree naryTree) {
        this.naryTree = naryTree;
    }
    
    /** Gets the original object (e.g. CoNLL09Sentence) used to create this sentence. */
    public Object getSourceSent() {
        return sourceSent;
    }
    
    /** Sets the original object (e.g. CoNLL09Sentence) used to create this sentence. */
    public void setSourceSent(Object sourceSent) {
        this.sourceSent = sourceSent;
    }

    public NerMentions getNamedEntities() {
        return namedEntities;
    }
    
    public void setNamedEntities(NerMentions namedEntities) {
        this.namedEntities = namedEntities;
    }
    
    public List<Pair<NerMention, NerMention>> getNePairs() {
		return nePairs;
	}

	public void setNePairs(List<Pair<NerMention, NerMention>> nePairs) {
		this.nePairs = nePairs;
	}

	public List<String> getRelLabels() {
        return relLabels;
    }

    public void setRelLabels(List<String> relLabels) {
        this.relLabels = relLabels;
    }

    public RelationMentions getRelations() {
        return relations;
    }

    public void setRelations(RelationMentions relations) {
        this.relations = relations;
    }
    
}
