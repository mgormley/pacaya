package edu.jhu.hltcoe.data;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree.HeadFinderException;
import edu.stanford.nlp.ling.CategoryWordTag;
import edu.stanford.nlp.trees.DiskTreebank;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeVisitor;
import edu.stanford.nlp.trees.Treebank;

public class DepTreebank extends ArrayList<DepTree> {

    private static Logger log = Logger.getLogger(DepTreebank.class);

    private SentenceCollection sentences = null;
    private int maxSentenceLength;
    private int maxNumSentences;

    public DepTreebank() {
        this(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public DepTreebank(final int maxSentenceLength, final int maxNumSentences) {
        this.maxSentenceLength = maxSentenceLength;
        this.maxNumSentences = maxNumSentences;
    }

    public void loadPath(String trainPath) {
        Treebank stanfordTreebank = new DiskTreebank();
        CategoryWordTag.suppressTerminalDetails = true;
        stanfordTreebank.loadPath(trainPath);
        for (Tree stanfordTree : stanfordTreebank) {
            try {
                DepTree tree = new DepTree(stanfordTree);
                if (this.size() >= maxNumSentences) {
                    break;
                }
                int len = tree.getNumWords();
                if (len <= maxSentenceLength) {
                    this.add(tree);
                }
            } catch (HeadFinderException e) {
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
