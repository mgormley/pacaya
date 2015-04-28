package edu.jhu.pacaya.autodiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.pacaya.autodiff.Toposort.Deps;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.collections.Sets;

public class ToposortTest {

    @Test
    public void testTopoSortNodes() {
        // valid sorts:
        List<String> valid = Arrays.asList(new String[] { 
                "[7, 5, 3, 11, 8, 2, 9, 10, ROOT]",
                "[3, 5, 7, 8, 11, 2, 9, 10, ROOT]",
                "[3, 7, 8, 5, 11, 10, 2, 9, ROOT]",
                "[5, 7, 3, 8, 11, 10, 9, 2, ROOT]",
                "[7, 5, 11, 3, 10, 8, 9, 2, ROOT]",
                "[7, 5, 11, 2, 3, 8, 9, 10, ROOT]",
                "[3, 7, 8, 5, 11, 10, 9, 2, ROOT]",
                "[5, 3, 7, 8, 11, 10, 2, 9, ROOT]",
                "[5, 7, 11, 2, 3, 10, 8, 9, ROOT]",
                "[3, 5, 7, 8, 11, 9, 10, 2, ROOT]",
                "[5, 3, 7, 8, 11, 10, 9, 2, ROOT]",
                "[7, 3, 8, 5, 11, 10, 9, 2, ROOT]",
        });
        
        for (int i : Lists.getList(1, 10, 12)) {
            Deps<String> deps = new ShuffledSimpleGraph(i);
            List<String> sort = Toposort.toposort("ROOT", deps);            
            if(valid.contains(sort.toString())) {
                System.out.println("i:" + i);
            }
            System.out.println(sort);
            assertTrue(valid.contains(sort.toString()));
        }
    }
    
    @Test
    public void testFindCycle() {
        Deps<String> deps = new SimpleGraph() {
            public List<String> getDeps(String x) {
                if (x == "5") {
                    return Lists.getList("7", "2");
                }
                return super.getDeps(x);
            }
        };
        try {
            List<String> sort = Toposort.toposort("ROOT", deps);
            System.out.println(sort);
            fail();
        } catch (IllegalStateException e) {
            // pass
        }
    }
    
    @Test
    public void testTopoSortWithInputs2() {
        // valid sorts:
        List<String> valid = Arrays.asList(new String[] { 
                "[3, 2, 1, ROOT]",
        });
        
        Deps<String> deps = new DiamondGraph();
        List<String> sort = Toposort.toposort(Lists.getList("4"), "ROOT", deps);
        System.out.println(sort);
        assertTrue(valid.contains(sort.toString()));
    }
    
    @Test
    public void testTopoSortWithInputs() {
        // valid sorts:
        List<String> valid = Arrays.asList(new String[] { 
                "[11, 2, 8, 9, 10, ROOT]",
                "[11, 2, 10, 8, 9, ROOT]",
                "[8, 11, 9, 2, 10, ROOT]",
                "[11, 8, 9, 10, 2, ROOT]",
        });
        
        for (int i : Lists.getList(0, 1, 2)) {
            // Note: in current implementation all orders are equivalent.
            Deps<String> deps = new ShuffledSimpleGraph(i*3);
            List<String> sort = Toposort.toposort(Lists.getList("7", "5", "3"), "ROOT", deps);            
            if(valid.contains(sort.toString())) {
                System.out.println("i:" + i);
            }
            System.out.println(sort);
            assertTrue(valid.contains(sort.toString()));
        }
    }
    
    @Test
    public void testTopoSortWithBadInputs() {
        SimpleGraph deps = new SimpleGraph();
        // Invalid leaf set.
        try {
            Toposort.toposort(Lists.getList("11", "8"), "ROOT", deps);         
            fail();
        } catch (IllegalStateException e) {
            //pass
        }
        // Multiple copies.
        try {
            Toposort.toposort(Lists.getList("11", "8", "11"), "ROOT", deps);         
            fail();
        } catch (IllegalStateException e) {
            //pass
        }
    }
    
    @Test
    public void testCheckIsValidLeafSet() {
        SimpleGraph deps = new SimpleGraph();
        // root = ROOT
        Toposort.checkIsValidLeafSet(new HashSet<String>(Lists.getList("11", "8", "3")), "ROOT", deps);
        Toposort.checkIsValidLeafSet(new HashSet<String>(Lists.getList("7", "5", "3")), "ROOT", deps);
        Toposort.checkIsValidLeafSet(new HashSet<String>(Lists.getList("11", "9", "10")), "ROOT", deps);
        Toposort.checkIsValidLeafSet(new HashSet<String>(Lists.getList("ROOT")), "ROOT", deps);
        
        // root = 9
        Toposort.checkIsValidLeafSet(new HashSet<String>(Lists.getList("7", "5", "3")), "9", deps);
        Toposort.checkIsValidLeafSet(new HashSet<String>(Lists.getList("11", "8")), "9", deps);
        
        try {
            Toposort.checkIsValidLeafSet(new HashSet<String>(Lists.getList("11", "8")), "ROOT", deps);
            fail();
        } catch (IllegalStateException e) {
            //pass
        }

        try {
            Toposort.checkIsValidLeafSet(new HashSet<String>(Lists.getList("5", "3")), "ROOT", deps);
            fail();
        } catch (IllegalStateException e) {
            //pass
        }

        try {
            Toposort.checkIsValidLeafSet(new HashSet<String>(Lists.getList("2", "8", "5")), "ROOT", deps);
            fail();
        } catch (IllegalStateException e) {
            //pass
        }
        
        // ROOT is not a descendent of 9.
        try {
            Toposort.checkIsValidLeafSet(new HashSet<String>(Lists.getList("11", "8", "ROOT")), "9", deps);
            fail();
        } catch (IllegalStateException e) {
            //pass
        }
    }
    
    @Test
    public void testDfs() {
        HashSet<String> visited = new HashSet<String>();
        HashSet<String> leaves = Toposort.dfs("ROOT", visited, new SimpleGraph());
        System.out.println(leaves);
        System.out.println(visited);
        
        String[] v = visited.toArray(new String[]{});
        Arrays.sort(v);
        Assert.assertArrayEquals(new String[]{ "10", "11", "2", "3", "5", "7", "8", "9", "ROOT"}, v);
        
        String[] l = leaves.toArray(new String[]{});
        Arrays.sort(l);
        Assert.assertArrayEquals(new String[]{"3", "5", "7"}, l);
    }
    
    @Test
    public void testGetImmediateParents() {
        {
            SimpleGraph deps = new SimpleGraph();
            HashSet<String> inputs = new HashSet<String>(Lists.getList("7", "5", "3"));
            Set<String> parents = Toposort.getImmediateParents(inputs, "ROOT", deps);
            assertEquals(Sets.getSet("11", "8", "10"), parents);
        }
        {
            SimpleGraph deps = new SimpleGraph();
            HashSet<String> inputs = new HashSet<String>();
            Set<String> parents = Toposort.getImmediateParents(inputs, "ROOT", deps);
            assertEquals(Sets.getSet("7", "5", "3"), parents);
        }
    }
    
    private static class ShuffledSimpleGraph extends SimpleGraph implements Deps<String> {
        
        private int i;
        
        private ShuffledSimpleGraph(int i) {
            this.i = i;
        }
        
        @Override
        public List<String> getDeps(String x) {
            List<String> deps = super.getDeps(x);
            Collections.shuffle(deps, new Random(i));
            return deps;
        }
        
    }
    

    private static class SimpleGraph implements Deps<String> {
        
        @Override
        public List<String> getDeps(String x) {
            switch(x) {
            case "ROOT": return Lists.getList("2", "9", "10");
            case "2": return Lists.getList("11");
            case "9": return Lists.getList("11", "8");
            case "10": return Lists.getList("11", "3");
            case "11": return Lists.getList("7", "5");
            case "8": return Lists.getList("7", "3");
            case "7": return Lists.getList();
            case "5": return Lists.getList();
            case "3": return Lists.getList();
            default: throw new IllegalArgumentException("Unknown node: " + x);
            }
        }
        
    }
    

    private static class DiamondGraph implements Deps<String> {
        
        @Override
        public List<String> getDeps(String x) {
            switch(x) {
            case "ROOT": return Lists.getList("1");
            case "1": return Lists.getList("2", "3");
            case "2": return Lists.getList("3", "4");
            case "3": return Lists.getList("4");
            case "4": return Lists.getList();
            default: throw new IllegalArgumentException("Unknown node: " + x);
            }
        }
        
    }

}
