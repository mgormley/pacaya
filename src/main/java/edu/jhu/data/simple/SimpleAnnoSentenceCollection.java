package edu.jhu.data.simple;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.Label;
import edu.jhu.data.Lemma;
import edu.jhu.data.Sentence;
import edu.jhu.data.SentenceCollection;
import edu.jhu.data.Tag;
import edu.jhu.data.Word;
import edu.jhu.util.Alphabet;

public class SimpleAnnoSentenceCollection extends ArrayList<SimpleAnnoSentence> {

    private static final long serialVersionUID = -6867088670574667680L;

    public static SimpleAnnoSentenceCollection getSingleton(SimpleAnnoSentence sent) {
        SimpleAnnoSentenceCollection col = new  SimpleAnnoSentenceCollection();
        col.add(sent);
        return col;
    }

    public SentenceCollection getWordsAsSentenceCollection(Alphabet<Label> alphabet) {
        SentenceCollection sents = new SentenceCollection(alphabet);
        for (SimpleAnnoSentence sent : this) {
            List<Label> labels = new ArrayList<Label>();
            for (String w : sent.getWords()) {
                labels.add(new Word(w));
            }
            sents.add(new Sentence(alphabet, labels));
        }
        return sents;
    }
    
    public SentenceCollection getLemmasAsSentenceCollection(Alphabet<Label> alphabet) {
        SentenceCollection sents = new SentenceCollection(alphabet);
        for (SimpleAnnoSentence sent : this) {
            List<Label> labels = new ArrayList<Label>();
            for (String l : sent.getLemmas()) {
                labels.add(new Lemma(l));
            }
            sents.add(new Sentence(alphabet, labels));
        }
        return sents;
    }
    
    public SentenceCollection getPosTagsAsSentenceCollection(Alphabet<Label> alphabet) {
        SentenceCollection sents = new SentenceCollection(alphabet);
        for (SimpleAnnoSentence sent : this) {
            List<Label> labels = new ArrayList<Label>();
            for (String t : sent.getPosTags()) {
                labels.add(new Tag(t));
            }
            sents.add(new Sentence(alphabet, labels));
        }
        return sents;
    }
    
    public DepTreebank getPosTagsAndParentsAsDepTreebank(Alphabet<Label> alphabet) {
        DepTreebank trees = new DepTreebank(alphabet);
        for (SimpleAnnoSentence sent : this) {
            List<Label> labels = new ArrayList<Label>();
            for (String t : sent.getPosTags()) {
                labels.add(new Tag(t));
            }
            Sentence sentence = new Sentence(alphabet, labels);
            boolean isProjective = DepTree.checkIsProjective(sent.getParents());
            trees.add(new DepTree(sentence, sent.getParents(), isProjective));
        }
        return trees; 
    }
    
    public int getNumTokens() {
        int numTokens = 0;
        for (SimpleAnnoSentence sent : this) {
            numTokens += sent.size();
        }
        return numTokens;
    }
    
}
