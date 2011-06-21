package edu.jhu.hltcoe.data;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree.HeadFinderException;
import edu.jhu.hltcoe.parse.IlpViterbiParser;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.Treebank;

public class DepTreebank extends ArrayList<DepTree> {

    private static Logger log = Logger.getLogger(DepTreebank.class);

    private SentenceCollection sentences = null;
    
    public DepTreebank() {
        // TODO Auto-generated constructor stub
    }
    
    public DepTreebank(Treebank treebank) {
        for (Tree tree : treebank) {
            try {
                add(new DepTree(tree));
            } catch(HeadFinderException e) {
                log.warn("Skipping tree due to HeadFinderException: " + e.getMessage());
            }
        }
    }

    public SentenceCollection getSentences() {
        if (sentences == null) {
            sentences = new SentenceCollection(this);
        }
        return sentences;
    }
    
    public int getNumWords() {
        int numWords = 0;
        for (DepTree tree : this) {
            numWords += tree.getNumWords();
        }
        return numWords;
    }
    
}
