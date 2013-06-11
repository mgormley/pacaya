package edu.jhu.hltcoe.data;

import edu.jhu.hltcoe.data.conll.CoNLL09Sentence;
import edu.jhu.hltcoe.data.conll.CoNLL09Token;
import edu.jhu.hltcoe.data.conll.CoNLLXSentence;
import edu.jhu.hltcoe.data.conll.CoNLLXToken;
import edu.jhu.hltcoe.util.Alphabet;
import gnu.trove.TIntArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class Sentence implements Iterable<Label> {

    private static final long serialVersionUID = 1L;
    private ArrayList<Label> labels;
    private TIntArrayList labelIds;     // TODO: cache an int[] version of this.
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
    
    public Sentence(CoNLLXSentence sent, Alphabet<Label> alphabet) {
        this(alphabet);
        for (CoNLLXToken token : sent) {
            add(new TaggedWord(token.getForm(), token.getPosTag()));
        }
    }

    public Sentence(CoNLL09Sentence sent, Alphabet<Label> alphabet) {
        this(alphabet);
        for (CoNLL09Token token : sent) {
            add(new TaggedWord(token.getForm(), token.getPos()));
        }
    }

    public Sentence(Alphabet<Label> alphabet, Iterable<Label> labels) {
        this(alphabet);
        for (Label l : labels) {
            add(l);
        }
    }
    
    public Sentence(Alphabet<Label> alphabet, int[] labelIds) {
        this(alphabet);
        for (int labelId : labelIds) {
            add(labelId);
        }
    }
    
    protected boolean add(Label label) {
        labelIds.add(alphabet.lookupIndex(label));
        return labels.add(label);
    }
    
    protected boolean add(int labelId) {
        labelIds.add(labelId);
        return labels.add(alphabet.lookupObject(labelId));
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
    
    public List<Label> getLabels() {
        return Collections.unmodifiableList(labels);
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
