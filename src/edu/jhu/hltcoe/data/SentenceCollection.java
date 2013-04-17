package edu.jhu.hltcoe.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.jhu.hltcoe.util.Alphabet;

public class SentenceCollection implements Iterable<Sentence> {

    private static final long serialVersionUID = 1L;
    private Alphabet<Label> alphabet;
    private ArrayList<Sentence> sents;
    private int numTokens;
    
    /**
     * Visible for testing only.
     */
    public SentenceCollection(Alphabet<Label> alphabet) {
        this.alphabet = alphabet;
        this.sents = new ArrayList<Sentence>();
    }
    
    /**
     * For testing only.
     */
    public SentenceCollection() {
        this(new Alphabet<Label>());
    }
    
    public SentenceCollection(Sentence sentence) {
        this(sentence.getAlphabet());
        add(sentence);
    }
    
    SentenceCollection(DepTreebank treebank) {
        this(treebank.getAlphabet());
        for (DepTree tree : treebank) {
            Sentence sentence = new Sentence(alphabet, tree);
            add(sentence);   
        }
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

    private void add(Sentence sentence) {
        addSentenceToAlphabet(sentence);
        sents.add(sentence);
        numTokens += sentence.size();
    }
    
    private void addSentenceToAlphabet(Sentence sentence) {
        for (Label l : sentence) {
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
     * Vocabulary of the sentences including WALL_LABEL
     */
    public Set<Label> getVocab() {
        Set<Label> vocab = new HashSet<Label>();
        for (Sentence sent : this) {
            for (Label label : sent) {
                vocab.add(label);
            }
        }
        // Special case for Wall
        vocab.add(WallDepTreeNode.WALL_LABEL);
        return vocab;
    }

    public Alphabet<Label> getLabelAlphabet() {
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
        
        public StringSentence(Alphabet<Label> alphabet, String string) {
            super(alphabet);
            String[] splits = string.split("\\s");
            for (String tok : splits) {
                String[] tw = tok.split("/");
                if (tw.length == 1) {
                    this.add(new Word(tw[0]));
                } else if (tw.length == 2) {
                    this.add(new TaggedWord(tw[0], tw[1]));
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
