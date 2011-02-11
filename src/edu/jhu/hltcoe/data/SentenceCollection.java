package edu.jhu.hltcoe.data;

import java.util.ArrayList;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.Treebank;

public class SentenceCollection extends ArrayList<Sentence> {

    public SentenceCollection(Treebank treebank) {
        super();
        for (Tree tree : treebank) {
            Sentence sentence = new Sentence();
            for (Tree node : tree.getLeaves()) {
                //TODO:
            }
            add(sentence);
        }
    }

}
