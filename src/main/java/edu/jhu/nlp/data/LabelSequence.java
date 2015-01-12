package edu.jhu.nlp.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.jhu.prim.list.IntArrayList;
import edu.jhu.util.Alphabet;


public class LabelSequence<X> implements Iterable<X>, Serializable {

    private static final long serialVersionUID = 1L;
    private ArrayList<X> labels;
    private IntArrayList labelIds;     // TODO: cache an int[] version of this.
    private Alphabet<X> alphabet;
    
    protected LabelSequence(Alphabet<X> alphabet) {
        labels = new ArrayList<X>();
        labelIds = new IntArrayList();
        this.alphabet = alphabet; 
    }

    public LabelSequence(Alphabet<X> alphabet, Iterable<X> labels) {
        this(alphabet);
        for (X l : labels) {
            add(l);
        }
    }
    
    public LabelSequence(Alphabet<X> alphabet, int[] labelIds) {
        this(alphabet);
        for (int labelId : labelIds) {
            add(labelId);
        }
    }
    
    protected boolean add(X label) {
        int idx = alphabet.lookupIndex(label);
        if (idx == -1) {
            throw new RuntimeException("Unknown label: " + label);
        }
        labelIds.add(idx);
        return labels.add(label);
    }
    
    protected boolean add(int labelId) {
        labelIds.add(labelId);
        return labels.add(alphabet.lookupObject(labelId));
    }
    
    public X get(int i) {
        return labels.get(i);
    }
    
    public int size() {
        return labels.size();
    }

    @Override
    public Iterator<X> iterator() {
        return labels.iterator();
    }
    
    public int[] getLabelIds() {
        return labelIds.toNativeArray();
    }

    public Alphabet<X> getAlphabet() {
        return alphabet;
    }
    
    public List<X> getLabels() {
        return Collections.unmodifiableList(labels);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (X label : this) {
            sb.append(label);
            sb.append(" ");
        }
        return sb.toString();
    }

}
