package edu.jhu.hltcoe.data;

import java.util.ArrayList;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.Treebank;

public class DepTreebank extends ArrayList<DepTree> {

    private SentenceCollection sentences = null;
    
    public DepTreebank() {
        // TODO Auto-generated constructor stub
    }
    
    public DepTreebank(Treebank treebank) {
        for (Tree tree : treebank) {
            add(new DepTree(tree));
        }
    }

    public SentenceCollection getSentences() {
        if (sentences == null) {
            sentences = new SentenceCollection(this);
        }
        return sentences;
    }
    
}
