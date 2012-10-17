package edu.jhu.hltcoe.gridsearch.cpt;

import java.util.ArrayList;
import java.util.Iterator;

public class CptBoundsDeltaList implements Iterable<CptBoundsDelta> {

    private ArrayList<CptBoundsDelta> deltas;
    private int primaryIndex;
    
    public CptBoundsDeltaList(CptBoundsDelta primaryDelta) {
        this();
        add(primaryDelta);
        // Store the index of the primary delta for later lookups.
        this.primaryIndex = 0;
    }

    private CptBoundsDeltaList() {
        this.deltas = new ArrayList<CptBoundsDelta>();
        this.primaryIndex = -1;
    }

    public void add(CptBoundsDelta lDelta) {
        deltas.add(lDelta);
    }

    public CptBoundsDelta get(int i) {
        return deltas.get(i);
    }

    public int size() {
        return deltas.size();
    }

    @Override
    public Iterator<CptBoundsDelta> iterator() {
        return deltas.iterator();
    }

    /**
     * Gets the primary delta which corresponds to the variable on which
     * we branched.
     */
    public CptBoundsDelta getPrimary() {
        return get(primaryIndex);
    }

    public static CptBoundsDeltaList getReverse(CptBoundsDeltaList deltas) {
        CptBoundsDeltaList reverse = new CptBoundsDeltaList();
        for (int i = deltas.size() - 1; i >= 0; i--) {
            reverse.add(CptBoundsDelta.getReverse(deltas.get(i)));
        }
        reverse.primaryIndex = deltas.size() - 1 - deltas.primaryIndex;  
        return reverse;
    }

    @Override
    public String toString() {
        return "CptBoundsDeltaList [deltas=" + deltas + ", primaryIndex=" + primaryIndex + "]";
    }
    
}
