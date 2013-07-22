package edu.jhu.data.conll;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.jhu.data.Lemma;
import edu.jhu.data.Tag;
import edu.jhu.data.Word;

/**
 * One sentence from a CoNLL-2009 formatted file.
 */
public class CoNLL09Sentence implements Iterable<CoNLL09Token> {

    private ArrayList<CoNLL09Token> tokens;
    
    public CoNLL09Sentence(List<CoNLL09Token> tokens) {
        this.tokens = new ArrayList<CoNLL09Token>(tokens);
    }

//    public CoNLL09Sentence(Sentence sent, int[] heads) {
//        tokens = new ArrayList<CoNLL09Token>();
//        for (int i=0; i<sent.size(); i++) {
//            Label label = sent.get(i);
//            TaggedWord tw = (TaggedWord) label;
//            tokens.add(new CoNLL09Token(i+1, tw.getWord(), tw.getWord(), tw.getTag(), tw.getTag(), null, heads[i], "NO_LABEL", null, null));
//        }
//    }

    /** Deep copy constructor. */
    public CoNLL09Sentence(CoNLL09Sentence sent) {
        tokens = new ArrayList<CoNLL09Token>(sent.tokens.size());
        for (CoNLL09Token tok : sent) {
            tokens.add(new CoNLL09Token(tok));
        }
    }

    public static CoNLL09Sentence getInstanceFromTokenStrings(ArrayList<String> sentLines) {
        List<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();
        for (String line : sentLines) {
            tokens.add(new CoNLL09Token(line));
        }
        return new CoNLL09Sentence(tokens);
    }
    
    public CoNLL09Token get(int i) {
        return tokens.get(i);
    }

    public int size() {
        return tokens.size();
    }

    @Override
    public Iterator<CoNLL09Token> iterator() {
        return tokens.iterator();
    }

    /**
     * Returns the head value for each token. The wall has index 0.
     * 
     * @return
     */
    public int[] getHeads() {
        int[] heads = new int[size()];
        for (int i = 0; i < heads.length; i++) {
            heads[i] = tokens.get(i).getHead();
        }
        return heads;
    }

    /**
     * Returns my internal reprensentation of the parent index for each token.
     * The wall has index -1.
     * 
     * @return
     */
    public int[] getParents() {
        int[] parents = new int[size()];
        for (int i = 0; i < parents.length; i++) {
            parents[i] = tokens.get(i).getHead() - 1;
        }
        return parents;
    }

    public List<Word> getWords() {
        ArrayList<Word> words = new ArrayList<Word>(size());
        for (int i=0; i<size(); i++) {
            words.add(new Word(tokens.get(i).getForm()));            
        }
        return words;
    }
    
    public List<Lemma> getLemmas() {
        ArrayList<Lemma> words = new ArrayList<Lemma>(size());
        for (int i=0; i<size(); i++) {
            words.add(new Lemma(tokens.get(i).getLemma()));            
        }
        return words;
    }
    
    public List<Tag> getPosTags() {
        ArrayList<Tag> words = new ArrayList<Tag>(size());
        for (int i=0; i<size(); i++) {
            words.add(new Tag(tokens.get(i).getPos()));            
        }
        return words;
    }
    
    public List<Tag> getPredictedPosTags() {
        ArrayList<Tag> words = new ArrayList<Tag>(size());
        for (int i=0; i<size(); i++) {
            words.add(new Tag(tokens.get(i).getPpos()));            
        }
        return words;
    }
    
    public void setPheadsFromParents(int[] parents) {
        for (int i = 0; i < parents.length; i++) {
            tokens.get(i).setPhead(parents[i] + 1);
        }
    }

    public SrlGraph getSrlGraph() {
        return new SrlGraph(this);
    }

}
