package edu.jhu.hltcoe.util.vector;

import java.util.Arrays;
import java.util.Iterator;

import edu.jhu.hltcoe.util.Sort;
import edu.jhu.hltcoe.util.Utilities;

public class SortedLongDoubleMap implements Iterable<LongDoubleEntry> {

	protected long[] indices;
	protected double[] values;
	protected int used; // TODO: size
	
	public SortedLongDoubleMap() {
		this.used = 0;
		this.indices= new long[0];
		this.values = new double[0];	
	}

	public SortedLongDoubleMap(long[] index, double[] data) {
		if (!Sort.isSortedAscAndUnique(index)) {
			throw new IllegalStateException("Indices are not sorted ascending");
		}
		if (!Sort.isSortedAsc(data)) {
			throw new IllegalStateException("Values are not sorted ascending");
		}
		
		this.used = index.length;
		this.indices = index;
		this.values = data;
	}

	public SortedLongDoubleMap(SortedLongDoubleMap other) {
		this.used = other.used;
		this.indices = Utilities.copyOf(other.indices);
		this.values = Utilities.copyOf(other.values);
	}

	public void clear() {
		this.used = 0;
	}
	
	public boolean contains(long idx) {
		return Arrays.binarySearch(indices, 0, used, idx) >= 0;
	}
	
	public double get(long idx) {
		int i = Arrays.binarySearch(indices, 0, used, idx);
		if (i < 0) {
			throw new IllegalArgumentException("This map does not contain the key: " + idx);
		}
		return values[i];
	}
	
	public double getWithDefault(long idx, double defaultVal) {
		int i = Arrays.binarySearch(indices, 0, used, idx);
		if (i < 0) {
			return defaultVal;
		}
		return values[i];
	}
	
	public void remove(long idx) {
		int i = Arrays.binarySearch(indices, 0, used, idx);
		if (i < 0) {
			throw new IllegalArgumentException("This map does not contain the key: " + idx);
		}		
		// Shift the values over.
		System.arraycopy(indices, i+1, indices, i, used - i - 1);
		System.arraycopy(values, i+1, values, i, used - i - 1);
		used--;
	}
	
	public void put(long idx, double val) {
		int i = Arrays.binarySearch(indices, 0, used, idx);
		if (i >= 0) {
			// Just update the value.
			values[i] = val;
			return;
		} 
		int insertionPoint = -(i + 1);
		indices = insert(indices, insertionPoint, idx);
		values = insert(values, insertionPoint, val);
		used++;
	}
	
	private final long[] insert(long[] array, int i, long val) {
		if (used >= array.length) {
			// Increase the capacity of the array.
			array = cern.colt.Arrays.ensureCapacity(array, used+1);
		}
		if (i < used) {
			// Shift the values over.
			System.arraycopy(array, i, array, i+1, used - i);
		}
		// Insert the new index into the array.
		array[i] = val;
		return array;
	}
	
	private final double[] insert(double[] array, int i, double val) {
		if (used >= array.length) {
			// Increase the capacity of the array.
			array = cern.colt.Arrays.ensureCapacity(array, used+1);
		}
		if (i < used) {
			// Shift the values over.
			System.arraycopy(array, i, array, i+1, used - i);
		}
		// Insert the new index into the array.
		array[i] = val;
		return array;
	}

	public class LongDoubleEntryImpl implements LongDoubleEntry {
		private int i;
		public LongDoubleEntryImpl(int i) {
			this.i = i;
		}
		public long index() {
			return indices[i];
		}
		public double get() {
			return values[i];
		}
	}

	public class LongDoubleIterator implements Iterator<LongDoubleEntry> {

		private int i = 0;
		
		@Override
		public boolean hasNext() {
			return i < used;
		}

		@Override
		public LongDoubleEntry next() {
			return new LongDoubleEntryImpl(i);
		}

		@Override
		public void remove() {
			throw new RuntimeException("operation not supported");
		}
		
	}

	@Override
	public Iterator<LongDoubleEntry> iterator() {
		return new LongDoubleIterator();
	}


	public int size() {
		return used;
	}

	public int getUsed() {
		return used;
	}	

    /**
     * Returns the indices.
     */
    public long[] getIndices() {
        if (used == indices.length)
            return indices;

        long[] tmpIndices = new long[used];
        for (int i = 0; i < used; i++) {
        	tmpIndices[i] = indices[i];
        }
        return tmpIndices;
    }
    
    /**
     * Returns the values.
     */
    public double[] getValues() {
        if (used == values.length)
            return values;

        double[] tmpValues = new double[used];
        for (int i = 0; i < used; i++) {
        	tmpValues[i] = values[i];
        }
        return tmpValues;
    }
		
}
