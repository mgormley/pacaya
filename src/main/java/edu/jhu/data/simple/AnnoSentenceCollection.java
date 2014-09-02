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

public class AnnoSentenceCollection extends ArrayList<AnnoSentence> {

    private static final long serialVersionUID = -6867088670574667680L;

    /** Stores the source sentences (e.g. the Communication object for Concrete). */
    private Object sourceSents;

    public AnnoSentenceCollection() {
        super();
    }
    
    public AnnoSentenceCollection(Collection<AnnoSentence> list) {
        super(list);
        if (list instanceof AnnoSentenceCollection) {
            this.sourceSents = ((AnnoSentenceCollection) list).sourceSents;
        }
    }

    public static AnnoSentenceCollection getSingleton(AnnoSentence sent) {
        AnnoSentenceCollection col = new  AnnoSentenceCollection();
        col.add(sent);
        return col;
    }

    public SentenceCollection getWordsAsSentenceCollection(Alphabet<String> alphabet) {
        SentenceCollection sents = new SentenceCollection(alphabet);
        for (AnnoSentence sent : this) {
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
        for (AnnoSentence sent : this) {
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
        for (AnnoSentence sent : this) {
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
        for (AnnoSentence sent : this) {
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
        for (AnnoSentence sent : this) {
            numTokens += sent.size();
        }
        return numTokens;
    }

    public AnnoSentenceCollection subList(int start, int end) {
        return new AnnoSentenceCollection(super.subList(start, end));
    }

    /**
     * Gets a deep copy of these sentences with some annotation layers removed.
     * @param removeAts The annotation layers to remove.
     * @return The filtered deep copy.
     */
    public AnnoSentenceCollection getWithAtsRemoved(List<AT> removeAts) {
        AnnoSentenceCollection newSents = new AnnoSentenceCollection();
        newSents.sourceSents = this.sourceSents;
        for (AnnoSentence sent : this) {
            AnnoSentence newSent = new AnnoSentence(sent);
            newSent.removeAts(removeAts);
            newSents.add(newSent);
        }
        return newSents;
    }

    /** Gets the length of the longest sentence in this collection. */
    public int getMaxLength() {
        int maxLen = Integer.MIN_VALUE;
        for (AnnoSentence sent : this) {
            if (sent.size() > maxLen) {
                maxLen = sent.size();
            }
        }
        return maxLen;
    }
    
    public static void copyShallow(AnnoSentenceCollection srcSents, AnnoSentenceCollection destSents, AT at) {
        for (int i=0; i<srcSents.size(); i++) {
            AnnoSentence src = srcSents.get(i);
            AnnoSentence dest = destSents.get(i);
            AnnoSentence.copyShallow(src, dest, at);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i=0; i<size(); i++) {
            sb.append(get(i));
            if (i != size()-1) {
                sb.append(",\n");
            }
        }
        sb.append("]");
        return sb.toString();        
    }
    
    public Object getSourceSents() {
        return sourceSents;
    }

    public void setSourceSents(Object sourceSents) {
        this.sourceSents = sourceSents;
    }
    
}
