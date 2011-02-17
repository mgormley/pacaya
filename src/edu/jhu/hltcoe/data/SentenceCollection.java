package edu.jhu.hltcoe.data;

import java.util.ArrayList;

public class SentenceCollection extends ArrayList<Sentence> {

    public SentenceCollection() {
        super();
    }
    
    public SentenceCollection(DepTreebank treebank) {
        super();
        for (DepTree tree : treebank) {
            Sentence sentence = new Sentence(tree);
            add(sentence);   
        }
    }

}
