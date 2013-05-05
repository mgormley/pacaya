package edu.jhu.hltcoe.util.vector;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

public class SortedLongDoubleMapTest {

	@Test
	public void testOrderedUsage() {
		SortedLongDoubleMap map = new SortedLongDoubleMap();
		map.put(1, 11.);
		map.put(2, 22.);
		map.put(3, 33.);
		
		assertEquals(11., map.get(1), 1e-13);
		assertEquals(22., map.get(2), 1e-13);
		assertEquals(33., map.get(3), 1e-13);
	}
	
	@Test
	public void testNormalUsage() {
		SortedLongDoubleMap map = new SortedLongDoubleMap();
		map.put(2, 22.);
		map.put(1, 11.);
		map.put(3, 33.);
		map.put(-1, -11.);
		map.put(8, 88.);
		map.put(6, 66.);

		assertEquals(33., map.get(3), 1e-13);		
		assertEquals(11., map.get(1), 1e-13);
		assertEquals(-11., map.get(-1), 1e-13);
		assertEquals(22., map.get(2), 1e-13);
		assertEquals(88., map.get(8), 1e-13);
		assertEquals(66., map.get(6), 1e-13);
		
		// Clear the map.
		map.clear();
		
		map.put(3, 33.);
		map.put(2, 22.);
		map.put(1, 11.);
		
		assertEquals(22., map.get(2), 1e-13);
		assertEquals(11., map.get(1), 1e-13);
		assertEquals(33., map.get(3), 1e-13);
	}


	@Test
	public void testRemove() {
		// First element.
		SortedLongDoubleMap map = new SortedLongDoubleMap();
		map.put(2, 22.);
		map.put(1, 11.);
		assertEquals(22., map.get(2), 1e-13);
		assertEquals(11., map.get(1), 1e-13);
		
		map.remove(1);
		assertEquals(false, map.contains(1));
		assertEquals(22., map.get(2), 1e-13);
		assertEquals(1, map.size());
		
		// Middle element.
		map = new SortedLongDoubleMap();
		map.put(2, 22.);
		map.put(3, 33.);
		map.put(1, 11.);
		assertEquals(22., map.get(2), 1e-13);
		assertEquals(11., map.get(1), 1e-13);
		assertEquals(33., map.get(3), 1e-13);		
		
		map.remove(2);
		assertEquals(false, map.contains(2));
		assertEquals(11., map.get(1), 1e-13);
		assertEquals(33., map.get(3), 1e-13);		
		assertEquals(2, map.size());
		
		// Last element.
		map = new SortedLongDoubleMap();
		map.put(2, 22.);
		map.put(3, 33.);
		map.put(1, 11.);
		assertEquals(22., map.get(2), 1e-13);
		assertEquals(11., map.get(1), 1e-13);
		assertEquals(33., map.get(3), 1e-13);		
		
		map.remove(3);
		assertEquals(false, map.contains(3));
		assertEquals(11., map.get(1), 1e-13);
		assertEquals(22., map.get(2), 1e-13);		
		assertEquals(2, map.size());
	}

	@Test
	public void testBadGets() {
		SortedLongDoubleMap map = new SortedLongDoubleMap();

		try {
			map.get(2);
		} catch(Exception e) {
			// pass
		}
		map.put(3, 33.);
		try {
			map.get(-3);
		} catch(Exception e) {
			// pass
		}
	}

    @Test
    public void testIterator() {
        SortedLongDoubleMap map = new SortedLongDoubleMap();
        map.put(2, 22.);
        map.put(1, 11.);
        
        LongDoubleEntry cur;
        Iterator<LongDoubleEntry> iter = map.iterator();
        assertEquals(true, iter.hasNext()); 
        assertEquals(true, iter.hasNext()); 
        cur = iter.next();
        assertEquals(1, cur.index()); 
        assertEquals(11, cur.get(), 1e-13); 
        assertEquals(true, iter.hasNext()); 
        cur = iter.next();
        assertEquals(2, cur.index()); 
        assertEquals(22, cur.get(), 1e-13); 
        assertEquals(false, iter.hasNext());
    }
    
}
