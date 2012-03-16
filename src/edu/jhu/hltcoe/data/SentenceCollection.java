package edu.jhu.hltcoe.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import util.Alphabet;

public class SentenceCollection extends ArrayList<Sentence> {

    private static final long serialVersionUID = 1L;
    private Alphabet<Label> alphabet;

    public SentenceCollection() {
        super();
        alphabet = new Alphabet<Label>();
    }
    
    SentenceCollection(DepTreebank treebank) {
        this();
        for (DepTree tree : treebank) {
            Sentence sentence = new Sentence(alphabet, tree);
            add(sentence);   
        }
    }
    
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

}
