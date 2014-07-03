package edu.jhu.data.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.Sentence;
import edu.jhu.data.SentenceCollection;
import edu.jhu.featurize.TemplateLanguage.AT;
import edu.jhu.util.Alphabet;

public class SimpleAnnoSentenceCollection extends ArrayList<SimpleAnnoSentence> {

    private static final long serialVersionUID = -6867088670574667680L;

    public SimpleAnnoSentenceCollection() {
        super();
    }
    
    public SimpleAnnoSentenceCollection(Collection<SimpleAnnoSentence> list) {
        super(list);
    }

    public static SimpleAnnoSentenceCollection getSingleton(SimpleAnnoSentence sent) {
        SimpleAnnoSentenceCollection col = new  SimpleAnnoSentenceCollection();
        col.add(sent);
        return col;
    }

    public SentenceCollection getWordsAsSentenceCollection(Alphabet<String> alphabet) {
        SentenceCollection sents = new SentenceCollection(alphabet);
        for (SimpleAnnoSentence sent : this) {
            List<String> labels = new ArrayList<String>();
            for (String w : sent.getWords()) {
                labels.add(w);
            }
            sents.add(new Sentence(alphabet, labels));
        }
        return sents;
    }
    
    public SentenceCollection getLemmasAsSentenceCollection(Alphabet<String> alphabet) {
        SentenceCollection sents = new SentenceCollection(alphabet);
        for (SimpleAnnoSentence sent : this) {
            List<String> labels = new ArrayList<String>();
            for (String l : sent.getLemmas()) {
                labels.add(l);
            }
            sents.add(new Sentence(alphabet, labels));
        }
        return sents;
    }
    
    public SentenceCollection getPosTagsAsSentenceCollection(Alphabet<String> alphabet) {
        SentenceCollection sents = new SentenceCollection(alphabet);
        for (SimpleAnnoSentence sent : this) {
            List<String> labels = new ArrayList<String>();
            for (String t : sent.getPosTags()) {
                labels.add(t);
            }
            sents.add(new Sentence(alphabet, labels));
        }
        return sents;
    }
    
    public DepTreebank getPosTagsAndParentsAsDepTreebank(Alphabet<String> alphabet) {
        DepTreebank trees = new DepTreebank(alphabet);
        for (SimpleAnnoSentence sent : this) {
            List<String> labels = new ArrayList<String>();
            for (String t : sent.getPosTags()) {
                labels.add(t);
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

    public SimpleAnnoSentenceCollection subList(int start, int end) {
        return new SimpleAnnoSentenceCollection(super.subList(start, end));
    }

    /**
     * Gets a deep copy of these sentences with some annotation layers removed.
     * @param removeAts The annotation layers to remove.
     * @return The filtered deep copy.
     */
    public SimpleAnnoSentenceCollection getWithAtsRemoved(List<AT> removeAts) {
        SimpleAnnoSentenceCollection newSents = new SimpleAnnoSentenceCollection();
        for (SimpleAnnoSentence sent : this) {
            SimpleAnnoSentence newSent = new SimpleAnnoSentence(sent);
            newSent.removeAts(removeAts);
            newSents.add(newSent);
        }
        return newSents;
    }

    /** Gets the length of the longest sentence in this collection. */
    public int getMaxLength() {
        int maxLen = Integer.MIN_VALUE;
        for (SimpleAnnoSentence sent : this) {
            if (sent.size() > maxLen) {
                maxLen = sent.size();
            }
        }
        return maxLen;
    }
    
}
