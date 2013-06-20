package edu.jhu.util.collections;

import cern.colt.list.IntArrayList;

public class PIntArrayList extends IntArrayList {

    public void add(int[] values) {
        for (int element : values) {
            this.add(element);
        }
    }

    public int[] toNativeArray() {
        this.trimToSize();
        return elements();
    }
    
}
