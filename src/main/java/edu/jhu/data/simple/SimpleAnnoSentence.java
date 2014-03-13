package edu.jhu.data.simple;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.jhu.data.DepEdgeMask;
import edu.jhu.data.DepTree;
import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.Span;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.featurize.TemplateLanguage.AT;
import edu.jhu.parse.cky.data.BinaryTree;
import edu.jhu.prim.arrays.IntArrays;
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
public class SimpleAnnoSentence {

    // TODO: maybe change this to something like underscore?
    private static final String SPAN_STR_SEP = " ";
    
    private List<String> words;
    private List<String> lemmas;
    private List<String> posTags;
    private List<String> cposTags;
    private List<String> clusters;
    private ArrayList<List<String>> feats;
    private List<String> deprels;
    /**
     * Internal representation of a dependency parse: parents[i] gives the index
     * of the parent of the word at index i. The Wall node has index -1. If a
     * word has no parent, it has index -2 (e.g. if punctuation was not marked
     * with a head).
     */
    private int[] parents;
    private DepEdgeMask depEdgeMask;
    private SrlGraph srlGraph;
    /** Constituency parse. */    
    private BinaryTree binaryTree;
    
    // TODO: add NER
    // TODO: add Relations (e.g. ACE relations)
    // TODO: add Token offsets.
    
    /** The original object (e.g. CoNLL09Sentence) used to create this sentence. */
    private Object sourceSent;
    
    public SimpleAnnoSentence() {

    }

    /**
     * Fairly deep copy constructor. Everything is deeply copied except for the
     * source sentence and the SRL graph, the features, and the constituency parse.
     */
    public SimpleAnnoSentence(SimpleAnnoSentence other) {
        this.words = Lists.copyOf(other.words);
        this.lemmas = Lists.copyOf(other.lemmas);
        this.posTags = Lists.copyOf(other.posTags);
        this.cposTags = Lists.copyOf(other.cposTags);
        this.clusters = Lists.copyOf(other.clusters);
        this.deprels = Lists.copyOf(other.deprels);
        this.parents = IntArrays.copyOf(other.parents);
        this.depEdgeMask = depEdgeMask == null ? null : new DepEdgeMask(depEdgeMask);
        this.sourceSent = other.sourceSent;
        // TODO: this should be a deep copy.
        this.feats = Lists.copyOf(other.feats);
        // TODO: this should be a deep copy.
        this.srlGraph = other.srlGraph;
        // TODO: this should be a deep copy.
        this.binaryTree = other.binaryTree;
    }
    
    /** Gets the i'th word as a String. */
    public String getWord(int i) {
        return words.get(i);
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
    private static List<String> getSpan(List<String> seq, Span span) {
        assert (span != null);
        List<String> list = new ArrayList<String>();
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
        
    // TODO: Return an IntHashSet after supporting iteration.
    public Set<Integer> getKnownPreds() {
        if (srlGraph == null) {
            return null;
        }
        Set<Integer> knownPreds = new HashSet<Integer>();
        // All the "Y"s
        for (SrlPred pred : srlGraph.getPreds()) {
            knownPreds.add(pred.getPosition());
        }
        return knownPreds;
    }

    public Integer size() {
        return words.size();
    }
    
    /* ----------- Getters/Setters for internal storage ------------ */
        
    public List<String> getWords() {
        return words;
    }

    public void setWords(List<String> words) {
        this.words = words;
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

    public SrlGraph getSrlGraph() {
        return srlGraph;
    }
    
    public void setSrlGraph(SrlGraph srlGraph) {
        this.srlGraph = srlGraph;
    }
    
    public List<String> getDeprels() {
        return deprels;
    }

    public void setDeprels(List<String> deprels) {
        this.deprels = deprels;
    }

    public ArrayList<List<String>> getFeats() {
        return feats;
    }

    public void setFeats(ArrayList<List<String>> feats) {
        this.feats = feats;
    }
    
    public BinaryTree getBinaryTree() {
        return binaryTree;
    }

    public void setBinaryTree(BinaryTree binaryTree) {
        this.binaryTree = binaryTree;
    }
    
    /** Gets the original object (e.g. CoNLL09Sentence) used to create this sentence. */
    public Object getSourceSent() {
        return sourceSent;
    }
    
    /** Sets the original object (e.g. CoNLL09Sentence) used to create this sentence. */
    public void setSourceSent(Object sourceSent) {
        this.sourceSent = sourceSent;
    }

    public void removeAts(List<AT> removeAts) {
        for (AT at : removeAts) {
            removeAt(at);
        }
    }

    public void removeAt(AT at) {
        switch (at) {
        case WORD: this.words = null; break;
        case BROWN: this.clusters = null; break;
        case LEMMA: this.lemmas = null; break;
        case POS: this.posTags = null; break;
        case CPOS: this.cposTags = null; break;
        case MORPHO: this.feats = null; break;
        case DEP_TREE: this.parents = null; break; // TODO: Should DEP_TREE also remove the labels? Not clear.
        case DEPREL: this.deprels = null; break;
        case DEP_EDGE_MASK: this.depEdgeMask = null; break;
        case SRL: this.srlGraph = null; break;
        case BINARY_TREE: this.binaryTree = null; break;
        default: throw new RuntimeException("not implemented for " + at);
        }
    }
    
    public boolean hasAt(AT at) {
        switch (at) {
        case WORD: return this.words != null;
        case BROWN: return this.clusters != null;
        case LEMMA: return this.lemmas != null;
        case POS: return this.posTags != null;
        case CPOS: return this.cposTags != null;
        case MORPHO: return this.feats != null;
        case DEP_TREE: return this.parents != null;
        case DEPREL: return this.deprels != null;
        case DEP_EDGE_MASK: return this.depEdgeMask != null;
        case SRL: return this.srlGraph != null;
        case BINARY_TREE: return this.binaryTree != null;
        default: throw new RuntimeException("not implemented for " + at);
        }
    }
    
    public void intern() {
        Lists.intern(words);
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
        if (binaryTree != null) {
            binaryTree.intern();
        }
        // TODO: this.srlGraph.intern();
    }

    @Override
    public String toString() {
        return "SimpleAnnoSentence [\n words=" + words + ",\n lemmas=" + lemmas + ",\n posTags=" + posTags + ",\n cposTags="
                + cposTags + ",\n clusters=" + clusters + ",\n feats=" + feats + ",\n deprels=" + deprels + ",\n parents="
                + Arrays.toString(parents) + ",\n depEdgeMask=" + depEdgeMask + ",\n srlGraph=" + srlGraph
                + ",\n binaryTree=" + binaryTree + ",\n sourceSent=" + sourceSent + "]";
    }
    
}
