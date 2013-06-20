package edu.jhu.util.vector;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

public class SortedIntIntMapTest {

	@Test
	public void testOrderedUsage() {
		SortedIntIntMap map = new SortedIntIntMap();
		map.put(1, 11);
		map.put(2, 22);
		map.put(3, 33);
		
		assertEquals(11, map.get(1));
		assertEquals(22, map.get(2));
		assertEquals(33, map.get(3));
	}
	
	@Test
	public void testNormalUsage() {
		SortedIntIntMap map = new SortedIntIntMap();
		map.put(2, 22);
		map.put(1, 11);
		map.put(3, 33);
		map.put(-1, -11);
		map.put(8, 88);
		map.put(6, 66);

		assertEquals(33, map.get(3));		
		assertEquals(11, map.get(1));
		assertEquals(-11, map.get(-1));
		assertEquals(22, map.get(2));
		assertEquals(88, map.get(8));
		assertEquals(66, map.get(6));
		
		// Clear the map.
		map.clear();
		
		map.put(3, 33);
		map.put(2, 22);
		map.put(1, 11);
		
		assertEquals(22, map.get(2));
		assertEquals(11, map.get(1));
		assertEquals(33, map.get(3));
	}


	@Test
	public void testRemove() {
		// First element.
		SortedIntIntMap map = new SortedIntIntMap();
		map.put(2, 22);
		map.put(1, 11);
		assertEquals(22, map.get(2));
		assertEquals(11, map.get(1));
		
		map.remove(1);
		assertEquals(false, map.contains(1));
		assertEquals(22, map.get(2));
		assertEquals(1, map.size());
		
		// Middle element.
		map = new SortedIntIntMap();
		map.put(2, 22);
		map.put(3, 33);
		map.put(1, 11);
		assertEquals(22, map.get(2));
		assertEquals(11, map.get(1));
		assertEquals(33, map.get(3));		
		
		map.remove(2);
		assertEquals(false, map.contains(2));
		assertEquals(11, map.get(1));
		assertEquals(33, map.get(3));		
		assertEquals(2, map.size());
		
		// Last element.
		map = new SortedIntIntMap();
		map.put(2, 22);
		map.put(3, 33);
		map.put(1, 11);
		assertEquals(22, map.get(2));
		assertEquals(11, map.get(1));
		assertEquals(33, map.get(3));		
		
		map.remove(3);
		assertEquals(false, map.contains(3));
		assertEquals(11, map.get(1));
		assertEquals(22, map.get(2));		
		assertEquals(2, map.size());
	}

	@Test
	public void testBadGets() {
		SortedIntIntMap map = new SortedIntIntMap();

		try {
			map.get(2);
		} catch(Exception e) {
			// pass
		}
		map.put(3, 33);
		try {
			map.get(-3);
		} catch(Exception e) {
			// pass
		}
	}

    @Test
    public void testIterator() {
        SortedIntIntMap map = new SortedIntIntMap();
        map.put(2, 22);
        map.put(1, 11);
        
        IntIntEntry cur;
        Iterator<IntIntEntry> iter = map.iterator();
        assertEquals(true, iter.hasNext()); 
        assertEquals(true, iter.hasNext()); 
        cur = iter.next();
        assertEquals(1, cur.index()); 
        assertEquals(11, cur.get()); 
        assertEquals(true, iter.hasNext()); 
        cur = iter.next();
        assertEquals(2, cur.index()); 
        assertEquals(22, cur.get()); 
        assertEquals(false, iter.hasNext());
    }
}
