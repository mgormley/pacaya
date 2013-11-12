package edu.jhu.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.util.Alphabet;

public class DepTreebank implements Iterable<DepTree> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(DepTreebank.class);

    private SentenceCollection sentences = null;
    private Alphabet<Label> alphabet;
    private ArrayList<DepTree> trees;
            
    public DepTreebank(Alphabet<Label> alphabet) {
        this.alphabet = alphabet;
        this.trees = new ArrayList<DepTree>();
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
                int idx = alphabet.lookupIndex(node.getLabel());
                if (idx == -1) {
                    throw new RuntimeException("Unknown label: " + node.getLabel());
                }
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
