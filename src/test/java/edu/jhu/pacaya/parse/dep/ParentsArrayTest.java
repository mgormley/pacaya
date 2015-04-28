package edu.jhu.pacaya.parse.dep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.pacaya.parse.dep.ParentsArray;
import edu.jhu.pacaya.parse.dep.ParentsArray.Dir;
import edu.jhu.prim.tuple.Pair;

public class ParentsArrayTest {

    @Test
    public void testIsProjective() {
        assertTrue(ParentsArray.isProjective(new int[]{ 1, -1, 1 }));
        assertTrue(ParentsArray.isProjective(new int[]{ -1, -1, -1 }));
        
        assertFalse(ParentsArray.isProjective(new int[]{ 2, -1, 1, 1 }));
        assertFalse(ParentsArray.isProjective(new int[]{ 2, -1, 3, 1 }));
    }
    
    @Test
    public void testIsConnected() {
        assertTrue(ParentsArray.isConnectedAndAcyclic(new int[]{ 1, -1, 1 }));
        assertTrue(ParentsArray.isConnectedAndAcyclic(new int[]{ -1, -1, -1 }));       
        assertTrue(ParentsArray.isConnectedAndAcyclic(new int[]{ 2, -1, 1, 1 }));
        assertTrue(ParentsArray.isConnectedAndAcyclic(new int[]{ 2, -1, 3, 1 }));
        
        // This is not counted as connected since there is no connection to the wall node.
        assertFalse(ParentsArray.isConnectedAndAcyclic(new int[]{ 1, 2, 0 }));
        
        assertFalse(ParentsArray.isConnectedAndAcyclic(new int[]{ 1, 0, -1 }));
        assertFalse(ParentsArray.isConnectedAndAcyclic(new int[]{ 3, 4, 5, 6 }));
        assertFalse(ParentsArray.isConnectedAndAcyclic(new int[]{ 2, -1, 0, 1 }));
    }
    
    @Test
    public void testIsAcyclic() {
        assertTrue(ParentsArray.isAcyclic(new int[]{ 1, -1, 1 }));
        assertTrue(ParentsArray.isAcyclic(new int[]{ -1, -1, -1 }));       
        assertTrue(ParentsArray.isAcyclic(new int[]{ 2, -1, 1, 1 }));
        assertTrue(ParentsArray.isAcyclic(new int[]{ 2, -1, 3, 1 }));
        
        // This is not counted as connected since there is no connection to the wall node.
        assertFalse(ParentsArray.isAcyclic(new int[]{ 1, 2, 0 }));
        
        assertFalse(ParentsArray.isAcyclic(new int[]{ 1, 0, -1 }));
        //assertFalse(ParentsArray.isAcyclic(new int[]{ 3, 4, 5, 6 }));
        assertFalse(ParentsArray.isAcyclic(new int[]{ 2, -1, 0, 1 }));
        
    }
    
    @Test
    public void testGetDependencyPath1() {
        int[] parents = new int[]{ 1, -1, 1 };
        List<Pair<Integer, ParentsArray.Dir>> path = ParentsArray.getDependencyPath(0, 2, parents);
        
        List<Pair<Integer, ParentsArray.Dir>> goldPath = new ArrayList<Pair<Integer, ParentsArray.Dir>>();
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(0,ParentsArray.Dir.UP));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(1,ParentsArray.Dir.DOWN));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(2,ParentsArray.Dir.NONE));
        assertEquals(path, goldPath);
    }

    @Test
    public void testGetDependencyPath2() {
        int[] parents = new int[]{ 2, 0, -1, 2, 3 };
        List<Pair<Integer, ParentsArray.Dir>> path = ParentsArray.getDependencyPath(1, 4, parents);
        
        List<Pair<Integer, ParentsArray.Dir>> goldPath = new ArrayList<Pair<Integer, ParentsArray.Dir>>();
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(1,ParentsArray.Dir.UP));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(0,ParentsArray.Dir.UP));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(2,ParentsArray.Dir.DOWN));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(3,ParentsArray.Dir.DOWN));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(4,ParentsArray.Dir.NONE));
        assertEquals(path, goldPath);
    }

    @Test
    public void testGetDependencyPath3() {
        int[] parents = new int[]{ 2, 0, -1, 2, 3 };
        List<Pair<Integer, ParentsArray.Dir>> path = ParentsArray.getDependencyPath(1, 2, parents);
        
        List<Pair<Integer, ParentsArray.Dir>> goldPath = new ArrayList<Pair<Integer, ParentsArray.Dir>>();
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(1,ParentsArray.Dir.UP));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(0,ParentsArray.Dir.UP));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(2,ParentsArray.Dir.NONE));
        assertEquals(path, goldPath);
    }

    @Test
    public void testGetDependencyPath4() {
        int[] parents = new int[]{ 2, 0, -1, 2, 3 };
        List<Pair<Integer, ParentsArray.Dir>> path = ParentsArray.getDependencyPath(2, 4, parents);
        
        List<Pair<Integer, ParentsArray.Dir>> goldPath = new ArrayList<Pair<Integer, ParentsArray.Dir>>();
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(2,ParentsArray.Dir.DOWN));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(3,ParentsArray.Dir.DOWN));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(4,ParentsArray.Dir.NONE));
        assertEquals(path, goldPath);
    }
    

    @Test
    public void testGetDependencyPath5() {
        int[] parents = new int[]{ 2, 0, -1, 2, 3 };
        List<Pair<Integer, ParentsArray.Dir>> path = ParentsArray.getDependencyPath(-1, 4, parents);
        
        List<Pair<Integer, ParentsArray.Dir>> goldPath = new ArrayList<Pair<Integer, ParentsArray.Dir>>();
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(-1,ParentsArray.Dir.DOWN));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(2,ParentsArray.Dir.DOWN));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(3,ParentsArray.Dir.DOWN));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(4,ParentsArray.Dir.NONE));
        assertEquals(path, goldPath);
    }

    @Test
    public void testGetDependencyPath6() {
        int[] parents = new int[]{ 2, 0, -1, 2, 3 };
        List<Pair<Integer, ParentsArray.Dir>> path = ParentsArray.getDependencyPath(1, -1, parents);
        
        List<Pair<Integer, ParentsArray.Dir>> goldPath = new ArrayList<Pair<Integer, ParentsArray.Dir>>();
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(1,ParentsArray.Dir.UP));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(0,ParentsArray.Dir.UP));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(2,ParentsArray.Dir.UP));
        goldPath.add(new Pair<Integer,ParentsArray.Dir>(-1,ParentsArray.Dir.NONE));
        assertEquals(path, goldPath);
    }
    

    // Check that we return null if there is no path.
    @Test
    public void testGetDependencyPath7() {
        int[] parents = new int[]{ -2, 0, -1, 2, 3};
        assertNull(ParentsArray.getDependencyPath(1, 2, parents));
        assertNull(ParentsArray.getDependencyPath(1, 3, parents));
        assertNull(ParentsArray.getDependencyPath(1, 4, parents));
        assertNull(ParentsArray.getDependencyPath(0, 2, parents));
        assertNull(ParentsArray.getDependencyPath(2, 1, parents));
        assertNull(ParentsArray.getDependencyPath(3, 1, parents));
        assertNull(ParentsArray.getDependencyPath(4, 1, parents));
    }
    
    // Check that we return null if there is a cycle.
    @Test
    public void testGetDependencyPath8() {
        int[] parents = new int[]{ 1, 2, 3, 0, -1};
        // Two within the cycle.
        assertNull(ParentsArray.getDependencyPath(1, 2, parents));
        // Start in the cycle.
        assertNull(ParentsArray.getDependencyPath(1, 4, parents));
        // End in the cycle.
        assertNull(ParentsArray.getDependencyPath(4, 1, parents));
    }
    
}
