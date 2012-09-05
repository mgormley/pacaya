package edu.jhu.hltcoe.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree.HeadFinderException;
import edu.stanford.nlp.ling.CategoryWordTag;
import edu.stanford.nlp.trees.DiskTreebank;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.Treebank;

public class DepTreebank extends ArrayList<DepTree> {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(DepTreebank.class);

    private SentenceCollection sentences = null;
    private int maxSentenceLength;
    private int maxNumSentences;
    private TreeFilter filter = null;

    public DepTreebank() {
        this(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public DepTreebank(final int maxSentenceLength, final int maxNumSentences) {
        this(maxSentenceLength, maxNumSentences, null);
    }
    
    public DepTreebank(final int maxSentenceLength, final int maxNumSentences, TreeFilter filter) {
        this.maxSentenceLength = maxSentenceLength;
        this.maxNumSentences = maxNumSentences;
        this.filter = filter;
    }
    
    public void setTreeFilter(TreeFilter filter) {
        this.filter = filter;
    }
    
    public void loadPath(String trainPath) {
        Treebank stanfordTreebank = new DiskTreebank();
        CategoryWordTag.suppressTerminalDetails = true;
        stanfordTreebank.loadPath(trainPath);
        for (Tree stanfordTree : stanfordTreebank) {
            try {
                if (this.size() >= maxNumSentences) {
                    break;
                }
                DepTree tree = new DepTree(stanfordTree);
                int len = tree.getNumTokens();
                if (len <= maxSentenceLength) {
                    if (filter == null || filter.accept(tree)) {
                        this.add(tree);
                    }
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

    public int getNumTokens() {
        int numWords = 0;
        for (DepTree tree : this) {
            numWords += tree.getNumTokens();
        }
        return numWords;
    }
    
    public Set<Label> getTypes() {
        Set<Label> types = new HashSet<Label>();
        for (DepTree tree : this) {
            for (DepTreeNode node : tree) {
                types.add(node.getLabel());
            }
        }
        types.remove(WallDepTreeNode.WALL_LABEL);
        return types;
    }
    
    public int getNumTypes() {
        return getTypes().size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (DepTree tree : this) {
            sb.append(tree);
            sb.append("\n");
        }
        return sb.toString();
    }
}
