package edu.jhu.data.concrete;

import java.util.List;

/**
 * Simple representation of a single sentence with many annotations.
 * 
 * This representation only uses strings, without Label objects or Alphabet objects.
 * 
 * @author mgormley
 */
public class SimpleAnnoSentence {

    private List<String> words;
    private List<String> lemmas;
    private List<String> posTags;
    /** Internal representation of a dependency parse. */
    private int[] parents;
    
    // TODO: add constituency parse as NaryTree<String>
    
    public SimpleAnnoSentence() {
        
    }

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
