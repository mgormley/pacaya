package edu.jhu.hltcoe.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree.HeadFinderException;
import edu.jhu.hltcoe.data.conll.CoNLL09DepTree;
import edu.jhu.hltcoe.data.conll.CoNLL09FileReader;
import edu.jhu.hltcoe.data.conll.CoNLL09Sentence;
import edu.jhu.hltcoe.data.conll.CoNLLXDepTree;
import edu.jhu.hltcoe.data.conll.CoNLLXDirReader;
import edu.jhu.hltcoe.data.conll.CoNLLXSentence;
import edu.jhu.hltcoe.util.Alphabet;
import edu.stanford.nlp.ling.CategoryWordTag;
import edu.stanford.nlp.trees.DiskTreebank;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.Treebank;

public class DepTreebank implements Iterable<DepTree> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(DepTreebank.class);

    private SentenceCollection sentences = null;
    private int maxSentenceLength;
    private int maxNumSentences;
    private TreeFilter filter = null;
    private Alphabet<Label> alphabet;
    private ArrayList<DepTree> trees;
        
    public DepTreebank(Alphabet<Label> alphabet) {
        this(Integer.MAX_VALUE, Integer.MAX_VALUE, alphabet);
    }

    public DepTreebank(final int maxSentenceLength, final int maxNumSentences, Alphabet<Label> alphabet) {
        this(maxSentenceLength, maxNumSentences, null, alphabet);
    }
    
    private DepTreebank(final int maxSentenceLength, final int maxNumSentences, TreeFilter filter, Alphabet<Label> alphabet) {
        this.maxSentenceLength = maxSentenceLength;
        this.maxNumSentences = maxNumSentences;
        this.filter = filter;
        this.alphabet = alphabet;
        this.trees = new ArrayList<DepTree>();
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
    public void loadPtbPath(String trainPath) {
        Treebank stanfordTreebank = new DiskTreebank();
        CategoryWordTag.suppressTerminalDetails = true;
        stanfordTreebank.loadPath(trainPath);
        for (Tree stanfordTree : stanfordTreebank) {
            try {
                if (this.size() >= maxNumSentences) {
                    break;
                }
                DepTree tree = new PtbDepTree(stanfordTree);
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

    /**
     * Read constituency trees from the Penn Treebank and use the Collins head
     * finding rules to extract dependency trees from them.
     * 
     * @param trainPath
     */
    public void loadCoNLLXPath(String trainPath) {
        CoNLLXDirReader reader = new CoNLLXDirReader(trainPath);
        for (CoNLLXSentence sent : reader) {
            try {
                if (this.size() >= maxNumSentences) {
                    break;
                }
                DepTree tree = new CoNLLXDepTree(sent, alphabet);
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
    
    public void loadCoNLL09Path(String trainPath) throws IOException {
        CoNLL09FileReader reader = new CoNLL09FileReader(new File(trainPath));
        for (CoNLL09Sentence sent : reader) {
            try {
                if (this.size() >= maxNumSentences) {
                    break;
                }
                DepTree tree = new CoNLL09DepTree(sent, alphabet);
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
    
    public void add(DepTree tree) {
        addTreeToAlphabet(tree);
        trees.add(tree);
    }

    private void addTreeToAlphabet(DepTree tree) {
        for (DepTreeNode node : tree) {
            if (node.getLabel() != WallDepTreeNode.WALL_LABEL) {
                alphabet.lookupIndex(node.getLabel());
            }
        }
    }
    
    public DepTree get(int i) {
        return trees.get(i);
    }
    
    public int size() {
        return trees.size();
    }

    public Alphabet<Label> getAlphabet() {
        return alphabet;
    }

    @Override
    public Iterator<DepTree> iterator() {
        return trees.iterator();
    }
    
}
