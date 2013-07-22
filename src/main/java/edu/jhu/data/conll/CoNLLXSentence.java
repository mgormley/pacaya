package edu.jhu.data.conll;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.data.TaggedWord;

/**
 * One sentence from a CoNLL-X formatted file.
 */
public class CoNLLXSentence implements Iterable<CoNLLXToken> {

    private ArrayList<CoNLLXToken> tokens;
        
    public CoNLLXSentence(ArrayList<String> sentLines) {
        tokens = new ArrayList<CoNLLXToken>();
        for (String line : sentLines) {
            tokens.add(new CoNLLXToken(line));
        }
    }

    public CoNLLXSentence(Sentence sent, int[] heads) {
        tokens = new ArrayList<CoNLLXToken>();
        for (int i=0; i<sent.size(); i++) {
            Label label = sent.get(i);
            TaggedWord tw = (TaggedWord) label;
            tokens.add(new CoNLLXToken(i+1, tw.getWord(), tw.getWord(), tw.getTag(), tw.getTag(), null, heads[i], "NO_LABEL", null, null));
        }
    }

    /** Deep copy constructor. */
    public CoNLLXSentence(CoNLLXSentence sent) {
        tokens = new ArrayList<CoNLLXToken>(sent.tokens.size());
        for (CoNLLXToken tok : sent) {
            tokens.add(new CoNLLXToken(tok));
        }
    }    
    
    public CoNLLXToken get(int i) {
        return tokens.get(i);
    }
    
    public int size() {
        return tokens.size();
    }
    
    @Override
    public Iterator<CoNLLXToken> iterator() {
        return tokens.iterator();
    }

    /**
     * Returns the head value for each token. The wall has index 0.
     * @return
     */
    public int[] getHeads() {
        int[] heads = new int[size()];
        for (int i=0; i<heads.length; i++) {
            heads[i] = tokens.get(i).getHead();
        }
        return heads;
    }
    
    /**
     * Returns my internal reprensentation of the parent index for each token. The wall has index -1.
     * @return
     */
    public int[] getParents() {
        int[] parents = new int[size()];
        for (int i=0; i<parents.length; i++) {
            parents[i] = tokens.get(i).getHead() - 1;
        }
        return parents;
    }

    public void setHeadsFromParents(int[] parents) {
        for (int i=0; i<parents.length; i++) {
            tokens.get(i).setHead(parents[i] + 1);
        }
    }
}