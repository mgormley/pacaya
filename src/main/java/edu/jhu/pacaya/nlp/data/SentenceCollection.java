package edu.jhu.pacaya.nlp.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.jhu.pacaya.util.Alphabet;

public class SentenceCollection implements Iterable<Sentence> {

    private static final long serialVersionUID = 1L;
    private Alphabet<String> alphabet;
    private ArrayList<Sentence> sents;
    private int numTokens;
    
    /**
     * Visible for testing only.
     */
    public SentenceCollection(Alphabet<String> alphabet) {
        this.alphabet = alphabet;
        this.sents = new ArrayList<Sentence>();
    }
    
    /**
     * For testing only.
     */
    public SentenceCollection() {
        this(new Alphabet<String>());
    }
    
    public SentenceCollection(Sentence sentence) {
        this(sentence.getAlphabet());
        add(sentence);
    }
    
    public SentenceCollection(SentenceCollection sentences1, SentenceCollection sentences2) {
        this(sentences1.alphabet);
        if (sentences1.alphabet != sentences2.alphabet) {
            throw new IllegalStateException("Alphabets must be the same");
        }
        for (Sentence sentence : sentences1) {
            add(sentence);
        }
        for (Sentence sentence : sentences2) {
            add(sentence);
        }
    }

    // TODO: should this be private?
    public void add(Sentence sentence) {
        if (sentence.getAlphabet() != alphabet) {
            throw new IllegalArgumentException("Alphabets do not match.");
        }
        addSentenceToAlphabet(sentence);
        sents.add(sentence);
        numTokens += sentence.size();
    }
    
    private void addSentenceToAlphabet(Sentence sentence) {
        for (String l : sentence) {
            alphabet.lookupIndex(l);
        }
    }

    public Sentence get(int i) {
        return sents.get(i);
    }
    
    public int size() {
        return sents.size();
    }

    /**
     * Vocabulary of the sentences.
     */
    public Set<String> getVocab() {
        Set<String> vocab = new HashSet<String>();
        for (Sentence sent : this) {
            for (String label : sent) {
                vocab.add(label);
            }
        }
        return vocab;
    }

    public Alphabet<String> getLabelAlphabet() {
        return alphabet;
    }

    /**
     * For testing only.
     */
    public void addSentenceFromString(String string) {
        add(new StringSentence(alphabet, string));
    }

    /**
     * For testing only.
     */
    private static class StringSentence extends Sentence {

        private static final long serialVersionUID = 1L;
        
        public StringSentence(Alphabet<String> alphabet, String string) {
            super(alphabet);
            String[] splits = string.split("\\s");
            for (String tok : splits) {
                String[] tw = tok.split("/");
                if (tw.length == 1) {
                    this.add(tw[0]);
                } else if (tw.length == 2) {
                    // Note: here we use just the tag.
                    // String word = tw[0];
                    String tag = tw[1];
                    this.add(tag);
                } else {
                    throw new IllegalStateException("At most we should only have a tag and a word");
                }
            }
        }

    }

    public int getNumTokens() {
        return numTokens;
    }

    @Override
    public Iterator<Sentence> iterator() {
        return sents.iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Sentence sent : this) {
            sb.append(sent);
            sb.append("\n");
        }
        return sb.toString();
    }
    
}
