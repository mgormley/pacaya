package edu.jhu.hltcoe.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bidirectional mapping of Objects to ints.
 * 
 * @author mgormley
 *
 */
public class Alphabet<T> {

	private ArrayList<T> idxObjMap;
	private Map<T, Integer> objIdxMap;
	
	public Alphabet() {
		idxObjMap = new ArrayList<T>();
		objIdxMap = new HashMap<T, Integer>();
	}
	
	public Alphabet(Alphabet<T> other) {
		idxObjMap = new ArrayList<T>(other.idxObjMap);
		objIdxMap = new HashMap<T, Integer>(other.objIdxMap);
	}

	public int lookupIndex(T object) {
		return lookupIndex(object, true);
	}

	public int lookupIndex(T object, boolean addIfMissing) {
		Integer index = objIdxMap.get(object);
		if (index == null) {
			// Add this new object to the alphabet.
			index = idxObjMap.size();
			idxObjMap.add(object);
			objIdxMap.put(object, index);
		}
		return index;
	}
	
	public T lookupObject(int index) {
		return idxObjMap.get(index);
	}

	@Override
	public Object clone() {
		return new Alphabet<T>(this);
	}

	public int size() {
		return idxObjMap.size();
	}

	public void startGrowth() {
		// TODO Auto-generated method stub
		
	}

	public void stopGrowth() {
		// TODO Auto-generated method stub
		
	}

	public List<T> getObjects() {
		return Collections.unmodifiableList(idxObjMap);
	}
	
}
