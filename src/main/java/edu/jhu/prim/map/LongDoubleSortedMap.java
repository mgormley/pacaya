package edu.jhu.prim.map;

import java.util.Arrays;
import java.util.Iterator;

import edu.jhu.prim.list.DoubleArrayList;
import edu.jhu.prim.list.LongArrayList;
import edu.jhu.util.Pair;
import edu.jhu.util.Sort;
import edu.jhu.util.Utilities;

/**
 * A primitives map from longs to doubles. The map is stored by keeping a sorted
 * array of indices, and an array of their corresponding values. This is useful
 * when an extremely compact representation of the map is needed.
 * 
 * @author mgormley
 */
public class LongDoubleSortedMap implements LongDoubleMap {

	protected long[] indices;
	protected double[] values;
	protected int used; // TODO: size
	
	public LongDoubleSortedMap() {
		this.used = 0;
		this.indices= new long[0];
		this.values = new double[0];	
	}

	public LongDoubleSortedMap(long[] index, double[] data) {
		if (!Sort.isSortedAscAndUnique(index)) {
			throw new IllegalStateException("Indices are not sorted ascending");
		}
		
		this.used = index.length;
		this.indices = index;
		this.values = data;
	}

	public LongDoubleSortedMap(LongDoubleSortedMap other) {
		this.used = other.used;
		this.indices = Utilities.copyOf(other.indices);
		this.values = Utilities.copyOf(other.values);
	}
	
    //	// TODO: we need to break up Sort into SortLongDouble, SortIntDouble before adding this constructor.
    //	public SortedLongDoubleMap(PLongDoubleHashMap other) {
    //        Pair<long[], double[]> ivs = other.getIndicesAndValues();
    //        this.indices = ivs.get1();
    //        this.values = ivs.get2();
    //        this.used = indices.length;
    //        Sort.sortIndexAsc(indices, values);
    //	}

	/* (non-Javadoc)
     * @see edu.jhu.util.vector.LongDoubleMap#clear()
     */
	@Override
    public void clear() {
		this.used = 0;
	}
	
	// TODO: rename to containsKey.
	/* (non-Javadoc)
     * @see edu.jhu.util.vector.LongDoubleMap#contains(long)
     */
	@Override
    public boolean contains(long idx) {
		return Arrays.binarySearch(indices, 0, used, idx) >= 0;
	}
	
	/* (non-Javadoc)
     * @see edu.jhu.util.vector.LongDoubleMap#get(long)
     */
	@Override
    public double get(long idx) {
		int i = Arrays.binarySearch(indices, 0, used, idx);
		if (i < 0) {
			throw new IllegalArgumentException("This map does not contain the key: " + idx);
		}
		return values[i];
	}
	
	/* (non-Javadoc)
     * @see edu.jhu.util.vector.LongDoubleMap#getWithDefault(long, double)
     */
	@Override
    public double getWithDefault(long idx, double defaultVal) {
		int i = Arrays.binarySearch(indices, 0, used, idx);
		if (i < 0) {
			return defaultVal;
		}
		return values[i];
	}
	
	/* (non-Javadoc)
     * @see edu.jhu.util.vector.LongDoubleMap#remove(long)
     */
	@Override
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
	
	/* (non-Javadoc)
     * @see edu.jhu.util.vector.LongDoubleMap#put(long, double)
     */
	@Override
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
			array = LongArrayList.ensureCapacity(array, used+1);
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
			array = DoubleArrayList.ensureCapacity(array, used+1);
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

    /**
     * This iterator is fast in the case of for(Entry e : vector) { }, however a
     * given entry should not be used after the following call to next().
     */
    public class LongDoubleIterator implements Iterator<LongDoubleEntry> {

        // The current entry.
        private LongDoubleEntryImpl entry = new LongDoubleEntryImpl(-1);

        @Override
        public boolean hasNext() {
            return entry.i + 1 < used;
        }

        @Override
        public LongDoubleEntry next() {
            entry.i++;
            return entry;
        }

        @Override
        public void remove() {
            throw new RuntimeException("operation not supported");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.jhu.util.vector.LongDoubleMap#iterator()
     */
	@Override
	public Iterator<LongDoubleEntry> iterator() {
		return new LongDoubleIterator();
	}


	/* (non-Javadoc)
     * @see edu.jhu.util.vector.LongDoubleMap#size()
     */
	@Override
    public int size() {
		return used;
	}

	public int getUsed() {
		return used;
	}	

    /* (non-Javadoc)
     * @see edu.jhu.util.vector.LongDoubleMap#getIndices()
     */
    @Override
    public long[] getIndices() {
        if (used == indices.length)
            return indices;

        long[] tmpIndices = new long[used];
        for (int i = 0; i < used; i++) {
        	tmpIndices[i] = indices[i];
        }
        return tmpIndices;
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.util.vector.LongDoubleMap#getValues()
     */
    @Override
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
