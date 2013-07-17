package edu.jhu.data.concrete;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.data.Span;

/**
 * Simple representation of a single sentence with many annotations.
 * 
 * This representation only uses strings, without Label objects or Alphabet objects.
 * 
 * @author mgormley
 */
public class SimpleAnnoSentence {

    // TODO: maybe change this to something like underscore?
    private static final String SPAN_STR_SEP = " ";
    
    private List<String> words;
    private List<String> lemmas;
    private List<String> posTags;
    /**
     * Internal representation of a dependency parse: parents[i] gives the index
     * of the parent of the word at index i. The Wall node has index -1. If a
     * word has no parent, it has index -2 (e.g. if punctuation was not marked
     * with a head).
     */
    private int[] parents;
    
    // TODO: add constituency parse as NaryTree<String>
    
    public SimpleAnnoSentence() {
        
    }


    /** Gets the i'th word as a String. */
    public String getWord(int i) {
        return words.get(i);
    }
    
    /** Gets the i'th POS tag as a String. */
    public String getPosTag(int i) {
        return posTags.get(i);
    }
    
    /** Gets the i'th lemma as a String. */
    public String getLemma(int i) {
        return lemmas.get(i);
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

    public int[] getParents() {
        return parents;
    }

    public void setParents(int[] parents) {
        this.parents = parents;
    }
    
}
