package edu.jhu.pacaya.util;

import edu.jhu.prim.list.IntArrayList;

/**
 * Bidirectional mapping of Objects to ints. This version keeps track of the
 * number of times the index was looked up for each object.
 * 
 * @author mgormley
 * @author mmitchell
 * 
 */
public class CountingAlphabet<T> extends Alphabet<T> {

    private static final long serialVersionUID = 1L;
    private IntArrayList idxCountMap;
	
	public CountingAlphabet() {
	    super();
	    idxCountMap = new IntArrayList();
	}
	
	public CountingAlphabet(CountingAlphabet<T> other) {
	    super(other);
	    idxCountMap = new IntArrayList(other.idxCountMap);
	}

	@Override
    public int lookupIndex(T object, boolean addIfMissing) {
	    int index = super.lookupIndex(object, addIfMissing);
	    if (index != MISSING_OBJECT_INDEX) {
	        while (index+1 > idxCountMap.size()) {
	            idxCountMap.add(0);
	        }
	        // Increment.
	        idxCountMap.set(index, idxCountMap.get(index) + 1);
	    }
	    return index;
	}
	
	public int lookupObjectCount(int index) {
	    return idxCountMap.get(index);
	}
	
}
