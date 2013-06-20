package edu.jhu.util.collections;

import cern.colt.list.DoubleArrayList;

public class PDoubleArrayList extends DoubleArrayList {

    public void add(double[] values) {
        for (double element : values) {
            this.add(element);
        }
    }

    public double[] toNativeArray() {
        this.trimToSize();
        return elements();
    }

}
