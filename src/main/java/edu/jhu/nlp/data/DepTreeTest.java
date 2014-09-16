package edu.jhu.nlp.data;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.nlp.data.DepTree.Dir;
import edu.jhu.prim.tuple.Pair;

public class DepTreeTest {

    @Test
    public void testIsProjective() {
        assertTrue(DepTree.checkIsProjective(new int[]{ 1, -1, 1 }));
        assertTrue(DepTree.checkIsProjective(new int[]{ -1, -1, -1 }));
        
        assertFalse(DepTree.checkIsProjective(new int[]{ 2, -1, 1, 1 }));
        assertFalse(DepTree.checkIsProjective(new int[]{ 2, -1, 3, 1 }));
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
}
