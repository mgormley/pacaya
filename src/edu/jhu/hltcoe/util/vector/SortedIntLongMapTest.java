package edu.jhu.hltcoe.util.vector;

import static org.junit.Assert.*;

import org.junit.Test;

public class SortedIntLongMapTest {

	@Test
	public void testOrderedUsage() {
		SortedIntLongMap map = new SortedIntLongMap();
		map.put(1, 11);
		map.put(2, 22);
		map.put(3, 33);
		
		assertEquals(11, map.get(1));
		assertEquals(22, map.get(2));
		assertEquals(33, map.get(3));
	}
	
	@Test
	public void testNormalUsage() {
		SortedIntLongMap map = new SortedIntLongMap();
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
		SortedIntLongMap map = new SortedIntLongMap();
		map.put(2, 22);
		map.put(1, 11);
		assertEquals(22, map.get(2));
		assertEquals(11, map.get(1));
		
		map.remove(1);
		assertEquals(false, map.contains(1));
		assertEquals(22, map.get(2));
		assertEquals(1, map.size());
		
		// Middle element.
		map = new SortedIntLongMap();
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
		map = new SortedIntLongMap();
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
		SortedIntLongMap map = new SortedIntLongMap();

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
}
