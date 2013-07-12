package edu.jhu.prim.map;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

public class IntDoubleSortedMapTest {

	@Test
	public void testOrderedUsage() {
		IntDoubleMap map = new IntDoubleSortedMap();
		map.put(1, toDouble(11));
		map.put(2, toDouble(22));
		map.put(3, toDouble(33));
		
		assertEquals(11, toInt(map.get(1)));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(33, toInt(map.get(3)));
	}
	
	@Test
	public void testNormalUsage() {
		IntDoubleMap map = new IntDoubleSortedMap();
		map.put(2, toDouble(22));
		map.put(1, toDouble(11));
		map.put(3, toDouble(33));
		map.put(-1, toDouble(-11));
		map.put(8, toDouble(88));
		map.put(6, toDouble(66));

		assertEquals(33, toInt(map.get(3)));		
		assertEquals(11, toInt(map.get(1)));
		assertEquals(-11, toInt(map.get(-1)));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(88, toInt(map.get(8)));
		assertEquals(66, toInt(map.get(6)));
		
		// Clear the map.
		map.clear();
		
		map.put(3, toDouble(33));
		map.put(2, toDouble(22));
		map.put(1, toDouble(11));
		
		assertEquals(22, toInt(map.get(2)));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(33, toInt(map.get(3)));
	}


	@Test
	public void testRemove() {
		// First element.
		IntDoubleMap map = new IntDoubleSortedMap();
		map.put(2, toDouble(22));
		map.put(1, toDouble(11));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(11, toInt(map.get(1)));
		
		map.remove(1);
		assertEquals(false, map.contains(1));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(1, map.size());
		
		// Middle element.
		map = new IntDoubleSortedMap();
		map.put(2, toDouble(22));
		map.put(3, toDouble(33));
		map.put(1, toDouble(11));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(33, toInt(map.get(3)));		
		
		map.remove(2);
		assertEquals(false, map.contains(2));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(33, toInt(map.get(3)));		
		assertEquals(2, map.size());
		
		// Last element.
		map = new IntDoubleSortedMap();
		map.put(2, toDouble(22));
		map.put(3, toDouble(33));
		map.put(1, toDouble(11));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(33, toInt(map.get(3)));		
		
		map.remove(3);
		assertEquals(false, map.contains(3));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(22, toInt(map.get(2)));		
		assertEquals(2, map.size());
	}

	@Test
	public void testBadGets() {
		IntDoubleMap map = new IntDoubleSortedMap();

		try {
			map.get(2);
		} catch(Exception e) {
			// pass
		}
		map.put(3, toDouble(33));
		try {
			map.get(-3);
		} catch(Exception e) {
			// pass
		}
	}

    @Test
    public void testIterator() {
        IntDoubleSortedMap map = new IntDoubleSortedMap();
        map.put(2, toDouble(22));
        map.put(1, toDouble(11));
        
        IntDoubleEntry cur;
        Iterator<IntDoubleEntry> iter = map.iterator();
        assertEquals(true, iter.hasNext()); 
        assertEquals(true, iter.hasNext()); 
        cur = iter.next();
        assertEquals(1, cur.index()); 
        assertEquals(11, toInt(cur.get())); 
        assertEquals(true, iter.hasNext()); 
        cur = iter.next();
        assertEquals(2, cur.index()); 
        assertEquals(22, toInt(cur.get())); 
        assertEquals(false, iter.hasNext());
    }
    
    private int toInt(double d) {
        return (int)d;
    }

    private double[] toDoubles(int... b) {
        double[] a = new double[b.length];
        for (int i=0; i<b.length; i++) {
            a[i] = b[i];
        }
        return a;
    }

    private double toDouble(int i) {
        return i;
    }
    
}
