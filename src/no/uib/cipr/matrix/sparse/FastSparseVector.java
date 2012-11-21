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

package no.uib.cipr.matrix.sparse;

import no.uib.cipr.matrix.Vector;

/**
 * Faster Sparse vector
 */
public class FastSparseVector extends SparseVector {

    boolean norm2Cached = false;
    double norm2Value;
    
    public FastSparseVector() {
        super(Integer.MAX_VALUE);
    }
    
    public FastSparseVector(int size) {
        super(size);
    }

    public FastSparseVector(Vector vector) {
        super(vector);
    }
    
    public FastSparseVector(int size, int[] index, double[] data) {
        super(size, index, data);
    }

    @Override
    public double dot(Vector y) {
        if (y instanceof FastSparseVector) {
            checkSize(y);
            FastSparseVector other = ((FastSparseVector) y);
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
     * Computes the Hadamard product (or entry-wise product) of this vector with another.
     */
    public SparseVector hadamardProd(FastSparseVector other) {
        FastSparseVector ip = new FastSparseVector(Math.max(other.size(), this.size()));
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
        for (int i=0; i<used; i++) {
            sb.append(index[i]);
            sb.append(":");
            sb.append(data[i]);
            if (i+1<used) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

}
