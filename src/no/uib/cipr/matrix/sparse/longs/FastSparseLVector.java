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

import edu.jhu.hltcoe.util.LongSort;
import edu.jhu.hltcoe.util.Utilities;

/**
 * Faster Sparse vector
 */
public class FastSparseLVector extends SparseLVector {

    boolean norm2Cached = false;
    double norm2Value;

    public FastSparseLVector() {
        super(Long.MAX_VALUE);
    }

    public FastSparseLVector(long size) {
        super(size);
    }

    public FastSparseLVector(LVector vector) {
        super(vector);
    }

    public FastSparseLVector(long size, long[] index, double[] data) {
        super(size, LongSort.sortIndexAsc(index, data), data);
    }

    public FastSparseLVector(long[] index, double[] data) {
        this(Long.MAX_VALUE, index, data);
    }

    public FastSparseLVector(double[] denseData) {
        super(Long.MAX_VALUE, LongSort.getIndexArray(denseData), denseData);
    }

    /**
     * @return A new vector without zeros OR the same vector if it has none.
     */
    public static SparseLVector getWithNoZeroValues(FastSparseLVector row, double zeroThreshold) {
        long[] origIndex = row.getIndex();
        double[] origData = row.getData();
        
        // Count and keep track of nonzeros.
        int numNonZeros = 0;
        boolean[] isNonZero = new boolean[row.getUsed()];
        for (int i = 0; i < row.getUsed(); i++) {
            double absVal = Math.abs(origData[i]);
            if (absVal < -zeroThreshold || zeroThreshold < absVal) {
                isNonZero[i] = true;
                numNonZeros++;
            } else {
                isNonZero[i] = false;
            }
        }
        int numZeros = row.getUsed() - numNonZeros;
        
        if (numZeros > 0) {
            // Create the new vector without the zeros.
            long[] newIndex = new long[numNonZeros];
            double[] newData = new double[numNonZeros];

            int newIdx = 0;
            for (int i = 0; i < row.getUsed(); i++) {
                if (isNonZero[i]) {
                    newIndex[newIdx] = origIndex[i];
                    newData[newIdx] = origData[i];
                    newIdx++;
                }
            }
            return new FastSparseLVector(newIndex, newData);
        } else {
            return row;
        }
    }

    @Override
    public double dot(LVector y) {
        if (y instanceof FastSparseLVector) {
            checkSize(y);
            FastSparseLVector other = ((FastSparseLVector) y);
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

    /**
     * TODO: Consider removing this since it's specific to the SCTM usage.
     */
    @Override
    protected double norm2() {
        if (!norm2Cached) {
            norm2Value = super.norm2();
            norm2Cached = true;
        }
        return norm2Value;
    }

    /**
     * Computes the Hadamard product (or entry-wise product) of this vector with
     * another.
     */
    public FastSparseLVector hadamardProd(FastSparseLVector other) {
        FastSparseLVector ip = new FastSparseLVector(Math.max(other.size(), this.size()));
        int oc = 0;
        for (int c = 0; c < used; c++) {
            while (oc < other.used) {
                if (other.index[oc] < index[c]) {
                    oc++;
                } else if (index[c] == other.index[oc]) {
                    ip.set(index[c], data[c] * other.data[oc]);
                    break;
                } else {
                    break;
                }
            }
        }
        return ip;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < used; i++) {
            sb.append(index[i]);
            sb.append(":");
            sb.append(data[i]);
            if (i + 1 < used) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Returns true if the input vector is equal to this one.
     */
    public boolean equals(FastSparseLVector other, double delta) {
        if (other.size() != this.size()) {
            return false;
        }
        // This is slow, but correct.
        for (LVectorEntry ve : this) {
            if (!Utilities.equals(ve.get(), other.get(ve.index()), delta)) {
                return false;
            }
        }
        for (LVectorEntry ve : other) {
            if (!Utilities.equals(ve.get(), this.get(ve.index()), delta)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the data.
     */
    @Override
    public double[] getData() {
        if (used == data.length)
            return data;

        double[] values = new double[used];
        for (int i = 0; i < used; i++) {
            values[i] = data[i];
        }
        return values;
    }

}
