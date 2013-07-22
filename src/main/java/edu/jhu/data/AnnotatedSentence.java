package edu.jhu.data;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.parse.cky.BinaryTree;
import edu.jhu.parse.cky.NaryTree;

/**
 * Representation of a single sentence with many annotations.
 * @author mgormley
 */
public class AnnotatedSentence {

    /*
     * TODO: Additional annotations we should add:
     * 1. NER
     * 2. Relations (e.g. ACE relations)
     * 3. Token offsets
     * 4. Labeled dependency tree.
     */
    
    private LabelSequence<Word> words;
    private LabelSequence<Lemma> lemmas;
    private LabelSequence<Tag> posTags;
    private DepTree depTree;
    private NaryTree naryTree;
    private BinaryTree binaryTree;
    private SrlGraph srlGraph;
    private boolean hasPred;
        
    public AnnotatedSentence() {
        
    }
    
//    public AnnotatedSentence(CoNLL09Sentence cos, boolean useGold) {
//        cos.getWords();
//        cos.getLemmas();
//        cos.getPredictedPosTags();
//        cos.getParents();
//        srlGraph = cos.getSrlGraph();
//    }
    
    /** Gets the i'th word as a String. */
    public String getWord(int i) {
        return words.get(i).getLabel();
    }
    
    /** Gets the i'th POS tag as a String. */
    public String getPosTag(int i) {
        return posTags.get(i).getLabel();
    }
    
    /** Gets the i'th lemma as a String. */
    public String getLemma(int i) {
        return lemmas.get(i).getLabel();
    }
    
    /**
     * Gets a list of words corresponding to a token span.
     */
    public List<String> getWords(Span span) {
        return getSpan(words, span);
    }

    /**
     * Gets a list of POS tags corresponding to a token span.
     */
    public List<String> getPosTags(Span span) {
        return getSpan(posTags, span);
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
            list.add(words.get(i).getWord() + "/" + posTags.get(i).getTag());
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
                sb.append(" ");
            }
            sb.append(words.get(i).getLabel());
            sb.append("/");
            sb.append(posTags.get(i).getLabel());
        }
        return sb.toString();
    }
    
    // TODO: Consider moving this to LabelSequence.
    private static List<String> getSpan(LabelSequence<? extends Label> seq, Span span) {
        assert (span != null);
        List<String> list = new ArrayList<String>();
        for (int i = span.start(); i < span.end(); i++) {
            list.add(seq.get(i).getLabel());
        }
        return list;
    }

    // TODO: Consider moving this to LabelSequence.
    private static String getSpanStr(LabelSequence<? extends Label> seq, Span span) {
        assert (span != null);
        StringBuilder sb = new StringBuilder();
        for (int i = span.start(); i < span.end(); i++) {
            if (i > span.start()) {
                sb.append(" ");
            }
            sb.append(seq.get(i).getLabel());
        }
        return sb.toString();
    }
    
    /* ----------- Getters/Setters for internal storage ------------ */
    
    public LabelSequence<Word> getWords() {
        return words;
    }
    public void setWords(LabelSequence<Word> words) {
        this.words = words;
    }
    public LabelSequence<Lemma> getLemmas() {
        return lemmas;
    }
    public void setLemmas(LabelSequence<Lemma> lemmas) {
        this.lemmas = lemmas;
    }
    public LabelSequence<Tag> getPosTags() {
        return posTags;
    }
    public void setPosTags(LabelSequence<Tag> posTags) {
        this.posTags = posTags;
    }
    public DepTree getDepTree() {
        return depTree;
    }
    public void setDepTree(DepTree depTree) {
        this.depTree = depTree;
    }
    public NaryTree getNaryTree() {
        return naryTree;
    }
    public void setNaryTree(NaryTree naryTree) {
        this.naryTree = naryTree;
    }
    public BinaryTree getBinaryTree() {
        return binaryTree;
    }
    public void setBinaryTree(BinaryTree binaryTree) {
        this.binaryTree = binaryTree;
    }
    public SrlGraph getSrlGraph() {
        return srlGraph;
    }
    public void setSrlGraph(SrlGraph srlGraph) {
        this.srlGraph = srlGraph;
    }
    public void setHasPred(boolean hasPred) {
        this.hasPred = hasPred;
    }
        
}
