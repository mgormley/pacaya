package edu.jhu.hltcoe.util.vector;

import static org.junit.Assert.*;

import org.junit.Test;

public class SortedIntDoubleMapTest {

	@Test
	public void testOrderedUsage() {
		SortedIntDoubleMap map = new SortedIntDoubleMap();
		map.put(1, 11.);
		map.put(2, 22.);
		map.put(3, 33.);
		
		assertEquals(11., map.get(1), 1e-13);
		assertEquals(22., map.get(2), 1e-13);
		assertEquals(33., map.get(3), 1e-13);
	}
	
	@Test
	public void testNormalUsage() {
		SortedIntDoubleMap map = new SortedIntDoubleMap();
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
		SortedIntDoubleMap map = new SortedIntDoubleMap();
		map.put(2, 22.);
		map.put(1, 11.);
		assertEquals(22., map.get(2), 1e-13);
		assertEquals(11., map.get(1), 1e-13);
		
		map.remove(1);
		assertEquals(false, map.contains(1));
		assertEquals(22., map.get(2), 1e-13);
		assertEquals(1, map.size());
		
		// Middle element.
		map = new SortedIntDoubleMap();
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
		map = new SortedIntDoubleMap();
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
		SortedIntDoubleMap map = new SortedIntDoubleMap();

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
}
