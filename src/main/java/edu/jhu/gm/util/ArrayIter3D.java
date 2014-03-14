package edu.jhu.gm.util;

import java.io.Serializable;
import java.util.Iterator;

public class ArrayIter3D implements Serializable {

    private static final long serialVersionUID = 1L;
    public int i;
    public int j;
    public int k;
    int[][][] indices;
    
    public ArrayIter3D(int[][][] indices) {
        this.indices = indices;
        reset();
    }

    private void reset() {
        i=0;
        j=0;
        k=-1;
    }
    
    private boolean iOkay() {
        return i < indices.length;
    }
    
    private boolean jOkay() {
        return iOkay() && j < indices[i].length;
    }
    
    private boolean kOkay() {
        return jOkay() && k < indices[i][j].length; 
    }
    
    public boolean next() {
        k++;
        while (!kOkay()) {
            k=0;
            j++;
            while (!jOkay()) {
                j=0;
                i++;
                if (!iOkay()) {
                    // Don't reset t=0, so that we recognize when we're done.                    
                    return false;
                }
            }
        }
        return true;
    }
    
}
