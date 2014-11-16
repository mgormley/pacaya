package edu.jhu.nlp.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.nlp.data.DepTree.Dir;
import edu.jhu.prim.tuple.Pair;

public class DepTreeTest {

    @Test
    public void testIsProjective() {
        assertTrue(DepTree.isProjective(new int[]{ 1, -1, 1 }));
        assertTrue(DepTree.isProjective(new int[]{ -1, -1, -1 }));
        
        assertFalse(DepTree.isProjective(new int[]{ 2, -1, 1, 1 }));
        assertFalse(DepTree.isProjective(new int[]{ 2, -1, 3, 1 }));
    }
    
    @Test
    public void testIsConnected() {
        assertTrue(DepTree.isConnectedAndAcyclic(new int[]{ 1, -1, 1 }));
        assertTrue(DepTree.isConnectedAndAcyclic(new int[]{ -1, -1, -1 }));       
        assertTrue(DepTree.isConnectedAndAcyclic(new int[]{ 2, -1, 1, 1 }));
        assertTrue(DepTree.isConnectedAndAcyclic(new int[]{ 2, -1, 3, 1 }));
        
        // This is not counted as connected since there is no connection to the wall node.
        assertFalse(DepTree.isConnectedAndAcyclic(new int[]{ 1, 2, 0 }));
        
        assertFalse(DepTree.isConnectedAndAcyclic(new int[]{ 1, 0, -1 }));
        assertFalse(DepTree.isConnectedAndAcyclic(new int[]{ 3, 4, 5, 6 }));
        assertFalse(DepTree.isConnectedAndAcyclic(new int[]{ 2, -1, 0, 1 }));
    }
    
    @Test
    public void testIsAcyclic() {
        assertTrue(DepTree.isAcyclic(new int[]{ 1, -1, 1 }));
        assertTrue(DepTree.isAcyclic(new int[]{ -1, -1, -1 }));       
        assertTrue(DepTree.isAcyclic(new int[]{ 2, -1, 1, 1 }));
        assertTrue(DepTree.isAcyclic(new int[]{ 2, -1, 3, 1 }));
        
        // This is not counted as connected since there is no connection to the wall node.
        assertFalse(DepTree.isAcyclic(new int[]{ 1, 2, 0 }));
        
        assertFalse(DepTree.isAcyclic(new int[]{ 1, 0, -1 }));
        //assertFalse(DepTree.isAcyclic(new int[]{ 3, 4, 5, 6 }));
        assertFalse(DepTree.isAcyclic(new int[]{ 2, -1, 0, 1 }));
        
    }
    
    @Test
    public void testGetDependencyPath1() {
        int[] parents = new int[]{ 1, -1, 1 };
        List<Pair<Integer, Dir>> path = DepTree.getDependencyPath(0, 2, parents);
        
        List<Pair<Integer, Dir>> goldPath = new ArrayList<Pair<Integer, Dir>>();
        goldPath.add(new Pair<Integer,Dir>(0,Dir.UP));
        goldPath.add(new Pair<Integer,Dir>(1,Dir.DOWN));
        goldPath.add(new Pair<Integer,Dir>(2,Dir.NONE));
        assertEquals(path, goldPath);
    }

    @Test
    public void testGetDependencyPath2() {
        int[] parents = new int[]{ 2, 0, -1, 2, 3 };
        List<Pair<Integer, Dir>> path = DepTree.getDependencyPath(1, 4, parents);
        
        List<Pair<Integer, Dir>> goldPath = new ArrayList<Pair<Integer, Dir>>();
        goldPath.add(new Pair<Integer,Dir>(1,Dir.UP));
        goldPath.add(new Pair<Integer,Dir>(0,Dir.UP));
        goldPath.add(new Pair<Integer,Dir>(2,Dir.DOWN));
        goldPath.add(new Pair<Integer,Dir>(3,Dir.DOWN));
        goldPath.add(new Pair<Integer,Dir>(4,Dir.NONE));
        assertEquals(path, goldPath);
    }

    @Test
    public void testGetDependencyPath3() {
        int[] parents = new int[]{ 2, 0, -1, 2, 3 };
        List<Pair<Integer, Dir>> path = DepTree.getDependencyPath(1, 2, parents);
        
        List<Pair<Integer, Dir>> goldPath = new ArrayList<Pair<Integer, Dir>>();
        goldPath.add(new Pair<Integer,Dir>(1,Dir.UP));
        goldPath.add(new Pair<Integer,Dir>(0,Dir.UP));
        goldPath.add(new Pair<Integer,Dir>(2,Dir.NONE));
        assertEquals(path, goldPath);
    }

    @Test
    public void testGetDependencyPath4() {
        int[] parents = new int[]{ 2, 0, -1, 2, 3 };
        List<Pair<Integer, Dir>> path = DepTree.getDependencyPath(2, 4, parents);
        
        List<Pair<Integer, Dir>> goldPath = new ArrayList<Pair<Integer, Dir>>();
        goldPath.add(new Pair<Integer,Dir>(2,Dir.DOWN));
        goldPath.add(new Pair<Integer,Dir>(3,Dir.DOWN));
        goldPath.add(new Pair<Integer,Dir>(4,Dir.NONE));
        assertEquals(path, goldPath);
    }
    

    @Test
    public void testGetDependencyPath5() {
        int[] parents = new int[]{ 2, 0, -1, 2, 3 };
        List<Pair<Integer, Dir>> path = DepTree.getDependencyPath(-1, 4, parents);
        
        List<Pair<Integer, Dir>> goldPath = new ArrayList<Pair<Integer, Dir>>();
        goldPath.add(new Pair<Integer,Dir>(-1,Dir.DOWN));
        goldPath.add(new Pair<Integer,Dir>(2,Dir.DOWN));
        goldPath.add(new Pair<Integer,Dir>(3,Dir.DOWN));
        goldPath.add(new Pair<Integer,Dir>(4,Dir.NONE));
        assertEquals(path, goldPath);
    }

    @Test
    public void testGetDependencyPath6() {
        int[] parents = new int[]{ 2, 0, -1, 2, 3 };
        List<Pair<Integer, Dir>> path = DepTree.getDependencyPath(1, -1, parents);
        
        List<Pair<Integer, Dir>> goldPath = new ArrayList<Pair<Integer, Dir>>();
        goldPath.add(new Pair<Integer,Dir>(1,Dir.UP));
        goldPath.add(new Pair<Integer,Dir>(0,Dir.UP));
        goldPath.add(new Pair<Integer,Dir>(2,Dir.UP));
        goldPath.add(new Pair<Integer,Dir>(-1,Dir.NONE));
        assertEquals(path, goldPath);
    }
    

    // Check that we return null if there is no path.
    @Test
    public void testGetDependencyPath7() {
        int[] parents = new int[]{ -2, 0, -1, 2, 3};
        assertNull(DepTree.getDependencyPath(1, 2, parents));
        assertNull(DepTree.getDependencyPath(1, 3, parents));
        assertNull(DepTree.getDependencyPath(1, 4, parents));
        assertNull(DepTree.getDependencyPath(0, 2, parents));
        assertNull(DepTree.getDependencyPath(2, 1, parents));
        assertNull(DepTree.getDependencyPath(3, 1, parents));
        assertNull(DepTree.getDependencyPath(4, 1, parents));
    }
    
    // Check that we return null if there is a cycle.
    @Test
    public void testGetDependencyPath8() {
        int[] parents = new int[]{ 1, 2, 3, 0, -1};
        // Two within the cycle.
        assertNull(DepTree.getDependencyPath(1, 2, parents));
        // Start in the cycle.
        assertNull(DepTree.getDependencyPath(1, 4, parents));
        // End in the cycle.
        assertNull(DepTree.getDependencyPath(4, 1, parents));
    }
    
}
