package edu.jhu.prim.matrix.infinite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;


public class ColumnCountIteratorTest {

	@Test
	public void testEmptyColumnVector() {
	    int[] columnCounts = new int[]{0,0,0};
	    ColumnCountIterator iterator = new ColumnCountIterator(columnCounts);
	    for (Integer k : iterator) {
	        fail();
	    }
	}
	
	@Test
    public void testOtherColumnVector() {
        int[] columnCounts = new int[]{0,1,1,0,0,1,1,1,0,0};
        List<Integer> expectedIndices = Arrays.asList(1,2,5,6,7); 
        
        testVector(columnCounts, expectedIndices);
    }
	
	@Test
    public void testAnotherColumnVector() {
        int[] columnCounts = new int[]{1,1,0,1};
        List<Integer> expectedIndices = Arrays.asList(0,1,3); 
        
        testVector(columnCounts, expectedIndices);
    }

    private void testVector(int[] columnCounts, List<Integer> expectedIndices) {
        ColumnCountIterator iterator = new ColumnCountIterator(columnCounts);
        
        List<Integer> actualIndices = new ArrayList<Integer>();
        for (Integer k : iterator) {
            actualIndices.add(k);
        }
        
        assertEquals(expectedIndices, actualIndices);
    }
	
}
