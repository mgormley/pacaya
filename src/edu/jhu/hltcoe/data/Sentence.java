package edu.jhu.hltcoe.data;

import java.util.ArrayList;
import java.util.Iterator;

import edu.jhu.hltcoe.data.conll.CoNLLXSentence;
import edu.jhu.hltcoe.util.Alphabet;
import gnu.trove.TIntArrayList;


public class Sentence implements Iterable<Label> {

    private static final long serialVersionUID = 1L;
    private ArrayList<Label> labels;
    private TIntArrayList labelIds;
    private Alphabet<Label> alphabet;
    
    protected Sentence(Alphabet<Label> alphabet) {
        labels = new ArrayList<Label>();
        labelIds = new TIntArrayList();
        this.alphabet = alphabet; 
    }
    
    public Sentence(Alphabet<Label> alphabet, DepTree tree) {
        this(alphabet);
        for (DepTreeNode node : tree.getNodes()) {
            if (!node.isWall()) {
                add(node.getLabel());
            }
        }
    }
    
    public Sentence(CoNLLXSentence sent) {
        
    }

    protected boolean add(Label label) {
        labelIds.add(alphabet.lookupIndex(label));
        return labels.add(label);
    }
    
    public Label get(int i) {
        return labels.get(i);
    }
    
    public int size() {
        return labels.size();
    }

    @Override
    public Iterator<Label> iterator() {
        return labels.iterator();
    }
    
    public int[] getLabelIds() {
        return labelIds.toNativeArray();
    }

    public Alphabet<Label> getAlphabet() {
        return alphabet;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Label label : this) {
            sb.append(label);
            sb.append(" ");
        }
        return sb.toString();
    }

}
