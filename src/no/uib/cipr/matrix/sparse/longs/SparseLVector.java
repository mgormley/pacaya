/*
 * Copyright (C) 2003-2006 Bjørn-Ove Heimsund
 * 
 * This file is part of MTJ.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package no.uib.cipr.matrix.sparse.longs;

import java.util.Iterator;

/**
 * Sparse vector
 */
public class SparseLVector extends AbstractLVector implements ILSparseVector {

    /**
     * Data
     */
    double[] data;

    /**
     * Indices to data
     */
    long[] index;

    /**
     * How much has been used
     */
    int used;

    /**
     * Constructor for SparseVector.
     * 
     * @param size
     *            Size of the vector
     * @param nz
     *            Initial number of non-zeros
     */
    public SparseLVector(long size, int nz) {
        super(size);
        data = new double[nz];
        index = new long[nz];
    }

    /**
     * Constructor for SparseVector, and copies the contents from the supplied
     * vector.
     * 
     * @param x
     *            Vector to copy from
     * @param deep
     *            True if a deep copy is to be made. If the copy is shallow,
     *            <code>x</code> must be a <code>SparseVector</code>
     */
    public SparseLVector(LVector x, boolean deep) {
        super(x);

        if (deep) {
            int nz = Matrices.cardinality(x);
            data = new double[nz];
            index = new long[nz];
            set(x);
        } else {
            SparseLVector xs = (SparseLVector) x;
            data = xs.getData();
            index = xs.getIndex();
            used = xs.getUsed();
        }
    }

    /**
     * Constructor for SparseVector, and copies the contents from the supplied
     * vector. Zero initial pre-allocation
     * 
     * @param x
     *            Vector to copy from. A deep copy is made
     */
    public SparseLVector(LVector x) {
        this(x, true);
    }

    /**
     * Constructor for SparseVector. Zero initial pre-allocation
     * 
     * @param size
     *            Size of the vector
     */
    public SparseLVector(long size) {
        this(size, 0);
    }

    /**
     * Constructor for SparseVector
     * 
     * @param size
     *            Size of the vector
     * @param index
     *            Indices of the vector
     * @param data
     *            Entries of the vector
     * @param deep
     *            True for a deep copy. For shallow copies, the given indices
     *            will be used internally
     */
    public SparseLVector(long size, long[] index, double[] data, boolean deep) {
        super(size);

        if (index.length != data.length)
            throw new IllegalArgumentException("index.length != data.length");

        if (deep) {
            used = index.length;
            this.index = index.clone();
            this.data = data.clone();
        } else {
            this.index = index;
            this.data = data;
            used = index.length;
        }
    }

    /**
     * Constructor for SparseVector
     * 
     * @param size
     *            Size of the vector
     * @param index
     *            The vector indices are copies from this array
     * @param data
     *            The vector entries are copies from this array
     */
    public SparseLVector(long size, long[] index, double[] data) {
        this(size, index, data, true);
    }

    @Override
    public void set(long index, double value) {
        check(index);

        // TODO: should we check against zero when setting zeros?
        
        int i = getIndex(index);
        data[i] = value;
    }

    @Override
    public void add(long index, double value) {
        check(index);

        int i = getIndex(index);
        data[i] += value;
    }

    @Override
    public double get(long index) {
        check(index);

        int in = Arrays.binarySearch(this.index, index, 0, used);
        if (in >= 0)
            return data[in];
        return 0;
    }

    /**
     * Tries to find the index. If it is not found, a reallocation is done, and
     * a new index is returned.
     */
    private int getIndex(long ind) {

        // Try to find column index
        int i = Arrays.binarySearchGreater(index, ind, 0, used);

        // Found
        if (i < used && index[i] == ind)
            return i;

        long[] newIndex = index;
        double[] newData = data;

        // Check available memory
        if (++used > data.length) {

            // If zero-length, use new length of 1, else double the bandwidth
            int newLength = data.length != 0 ? data.length << 1 : 1;

            // Copy existing data into new arrays
            newIndex = new long[newLength];
            newData = new double[newLength];
            System.arraycopy(index, 0, newIndex, 0, i);
            System.arraycopy(data, 0, newData, 0, i);
        }

        // All ok, make room for insertion
        System.arraycopy(index, i, newIndex, i + 1, used - i - 1);
        System.arraycopy(data, i, newData, i + 1, used - i - 1);

        // Put in new structure
        newIndex[i] = ind;
        newData[i] = 0.;

        // Update pointers
        index = newIndex;
        data = newData;

        // Return insertion index
        return i;
    }

    @Override
    public SparseLVector copy() {
        return new SparseLVector(this);
    }

    @Override
    public SparseLVector zero() {
        java.util.Arrays.fill(data, 0);
		used = 0;
        return this;
    }

    @Override
    public SparseLVector scale(double alpha) {
        // Quick return if possible
        if (alpha == 0)
            return zero();
        else if (alpha == 1)
            return this;

        for (int i = 0; i < used; ++i)
            data[i] *= alpha;

        return this;
    }

    /** 
     * My implementation:
     * 
     */
    @Override
    public double dot(LVector y) {
        if (y instanceof SparseLVector) {
            checkSize(y);
            SparseLVector other = ((SparseLVector) y);
            double ret = 0;
            int oc = 0;
            for (int c = 0; c < used; c++) {
                while (oc < other.used) {
                    if (other.index[oc] < index[c]) {
                        oc++;
                    } else if (index[c] == other.index[oc]) {
                        ret += data[c] * other.data[oc];
                        break;
                    } else {
                        break;
                    }
                }
            }
            return ret;
        } else {
            return super.dot(y);
        }
    }

    @Override
    protected double norm1() {
        double sum = 0;
        for (int i = 0; i < used; ++i)
            sum += Math.abs(data[i]);
        return sum;
    }

    @Override
    protected double norm2() {
        double norm = 0;
        for (int i = 0; i < used; ++i)
            norm += data[i] * data[i];
        return Math.sqrt(norm);
    }

    @Override
    protected double norm2_robust() {
        double scale = 0, ssq = 1;
        for (int i = 0; i < used; ++i) {
            if (data[i] != 0) {
                double absxi = Math.abs(data[i]);
                if (scale < absxi) {
                    ssq = 1 + ssq * Math.pow(scale / absxi, 2);
                    scale = absxi;
                } else
                    ssq = ssq + Math.pow(absxi / scale, 2);
            }
        }
        return scale * Math.sqrt(ssq);
    }

    @Override
    protected double normInf() {
        double max = 0;
        for (int i = 0; i < used; ++i)
            max = Math.max(Math.abs(data[i]), max);
        return max;
    }

    /**
     * Returns the internal data
     */
    public double[] getData() {
        return data;
    }

    /**
     * Returns the indices
     */
    public long[] getIndex() {
    	if (used == index.length)
    		return index;
    	
    	// could run compact, or return subarray
    	// compact();
    	long[] indices = new long[used];
    	for (int i = 0 ; i < used; i++) {
    		indices[i] = index[i];
    	}
    	return indices;
    }

    /**
     * Number of entries used in the sparse structure
     */
    public int getUsed() {
        return used;
    }

    /**
     * Compacts the vector
     */
    public void compact() {
		int nz = Matrices.cardinality(this); // catches zero entries

        if (nz < data.length) {
            long[] newIndex = new long[nz];
            double[] newData = new double[nz];

            // Copy only non-zero entries
            for (int i = 0, j = 0; i < data.length; ++i)
                if (data[i] != 0.) {
                    newIndex[j] = index[i];
                    newData[j] = data[i];
                    j++;
                }

            data = newData;
            index = newIndex;
            used = data.length;
        }
    }

    @Override
    public Iterator<LVectorEntry> iterator() {
        return new SparseLVectorIterator();
    }

    @Override
    public LVector set(LVector y) {
        if (!(y instanceof SparseLVector))
            return super.set(y);

        checkSize(y);

        SparseLVector yc = (SparseLVector) y;

        if (yc.index.length != index.length) {
            data = new double[yc.data.length];
            index = new long[yc.data.length];
        }

        System.arraycopy(yc.data, 0, data, 0, data.length);
        System.arraycopy(yc.index, 0, index, 0, index.length);
        used = yc.used;

        return this;
    }

    /**
     * Iterator over a sparse vector
     */
    private class SparseLVectorIterator implements Iterator<LVectorEntry> {

        private int cursor;

        private final SparseLVectorEntry entry = new SparseLVectorEntry();

        public boolean hasNext() {
            return cursor < used;
        }

        public LVectorEntry next() {
            entry.update(cursor);

            cursor++;

            return entry;
        }

        public void remove() {
            entry.set(0);
        }

    }

    /**
     * Entry of a sparse vector
     */
    private class SparseLVectorEntry implements LVectorEntry {

        private int cursor;

        public void update(int cursor) {
            this.cursor = cursor;
        }

        public long index() {
            return index[cursor];
        }

        public double get() {
            return data[cursor];
        }

        public void set(double value) {
            data[cursor] = value;
        }

    }

}
