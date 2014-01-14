package edu.jhu.data.deptree;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.TreeFilter;
import edu.jhu.data.conll.CoNLL09DepTree;
import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLLXDepTree;
import edu.jhu.data.conll.CoNLLXDirReader;
import edu.jhu.data.conll.CoNLLXSentence;
import edu.jhu.data.deptree.PtbDepTree.HeadFinderException;
import edu.stanford.nlp.ling.CategoryWordTag;
import edu.stanford.nlp.trees.DiskTreebank;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.Treebank;

public class DepTreebankLoader {

    private static final Logger log = Logger.getLogger(DepTreebankLoader.class);

    private int maxSentenceLength;
    private int maxNumSentences;
    private TreeFilter filter = null;
        
    public DepTreebankLoader() {
        this(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public DepTreebankLoader(final int maxSentenceLength, final int maxNumSentences) {
        this(maxSentenceLength, maxNumSentences, null);
    }
    
    private DepTreebankLoader(final int maxSentenceLength, final int maxNumSentences, TreeFilter filter) {
        this.maxSentenceLength = maxSentenceLength;
        this.maxNumSentences = maxNumSentences;
        this.filter = filter;
    }
    
    public void setTreeFilter(TreeFilter filter) {
        this.filter = filter;
    }
    
    /**
     * Read constituency trees from the Penn Treebank and use the Collins head
     * finding rules to extract dependency trees from them.
     * 
     * @param trainPath
     */
    // TODO: move to 
    public void loadPtbPath(DepTreebank trees, File trainPath) {
        Treebank stanfordTreebank = new DiskTreebank();
        CategoryWordTag.suppressTerminalDetails = true;
        stanfordTreebank.loadPath(trainPath);
        for (Tree stanfordTree : stanfordTreebank) {
            try {
                if (trees.size() >= maxNumSentences) {
                    break;
                }
                DepTree tree = new PtbDepTree(stanfordTree);
                int len = tree.getNumTokens();
                if (len <= maxSentenceLength) {
                    if (filter == null || filter.accept(tree)) {
                        trees.add(tree);
                    }
                }
            } catch (HeadFinderException e) {
                log.warn("Skipping tree due to HeadFinderException: " + e.getMessage());
            }
        }
    }

    /**
     * Read CoNLL-X dependency trees.
     * 
     * @param trainPath
     */
    public void loadCoNLLXPath(DepTreebank trees, File trainPath) {
        CoNLLXDirReader reader = new CoNLLXDirReader(trainPath);
        for (CoNLLXSentence sent : reader) {
            if (trees.size() >= maxNumSentences) {
                break;
            }
            DepTree tree = new CoNLLXDepTree(sent, trees.getAlphabet());
            int len = tree.getNumTokens();
            if (len <= maxSentenceLength) {
                if (filter == null || filter.accept(tree)) {
                    trees.add(tree);
                }
            }
        }
    }
    
    public void loadCoNLL09Path(DepTreebank trees, File trainPath) throws IOException {
        CoNLL09FileReader reader = new CoNLL09FileReader(trainPath);
        for (CoNLL09Sentence sent : reader) {
            if (trees.size() >= maxNumSentences) {
                break;
            }
            DepTree tree = new CoNLL09DepTree(sent, trees.getAlphabet());
            int len = tree.getNumTokens();
            if (len <= maxSentenceLength) {
                if (filter == null || filter.accept(tree)) {
                    trees.add(tree);
                }
            }
        }
    }
            
}
