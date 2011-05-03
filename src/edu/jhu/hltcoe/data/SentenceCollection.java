package edu.jhu.hltcoe.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SentenceCollection extends ArrayList<Sentence> {

    public SentenceCollection() {
        super();
    }
    
    SentenceCollection(DepTreebank treebank) {
        super();
        for (DepTree tree : treebank) {
            Sentence sentence = new Sentence(tree);
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

}
