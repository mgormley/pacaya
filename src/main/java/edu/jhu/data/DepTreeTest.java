package edu.jhu.data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.DepTree.Dir;
import edu.jhu.prim.tuple.Pair;

public class DepTreeTest {

    @Test
    public void testGetDependencyPath1() {
        int[] parents = new int[]{ 1, -1, 1 };
        List<Pair<Integer, Dir>> path = DepTree.getDependencyPath(0, 2, parents);
        
        List<Pair<Integer, Dir>> goldPath = new ArrayList<Pair<Integer, Dir>>();
        goldPath.add(new Pair<Integer,Dir>(0,Dir.UP));
        goldPath.add(new Pair<Integer,Dir>(1,Dir.DOWN));
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
        assertEquals(path, goldPath);
    }

    @Test
    public void testGetDependencyPath3() {
        int[] parents = new int[]{ 2, 0, -1, 2, 3 };
        List<Pair<Integer, Dir>> path = DepTree.getDependencyPath(1, 2, parents);
        
        List<Pair<Integer, Dir>> goldPath = new ArrayList<Pair<Integer, Dir>>();
        goldPath.add(new Pair<Integer,Dir>(1,Dir.UP));
        goldPath.add(new Pair<Integer,Dir>(0,Dir.UP));
        assertEquals(path, goldPath);
    }

    @Test
    public void testGetDependencyPath4() {
        int[] parents = new int[]{ 2, 0, -1, 2, 3 };
        List<Pair<Integer, Dir>> path = DepTree.getDependencyPath(2, 4, parents);
        
        List<Pair<Integer, Dir>> goldPath = new ArrayList<Pair<Integer, Dir>>();
        goldPath.add(new Pair<Integer,Dir>(2,Dir.DOWN));
        goldPath.add(new Pair<Integer,Dir>(3,Dir.DOWN));
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
        assertEquals(path, goldPath);
    }
}
