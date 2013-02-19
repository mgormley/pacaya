package edu.jhu.hltcoe.util.cplex;

import ilog.cplex.IloCplex.BasisStatus;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Tests how efficient storing a BasisStatus array is vs. storing booleans.
 * 
 * The results suggest that a BasisStatus[] is equivalent to an array of shorts.
 * 
 * @author mgormley
 *
 */
public class BasisStatusTest {

    private int numVars = 10000;
    
    @Test
    public void testNumBasisStatuses() {
        List<BasisStatus[]> list = new ArrayList<BasisStatus[]>();
        int i=0;
        try {
        while (true) {
            // TODO: set the value of each entry.
            list.add(new BasisStatus[numVars]);
            i++;
        }
        } catch (OutOfMemoryError e) {
            // Clean up.
            list = null;
            System.gc();
        }
        System.out.println(i);
    }
    
    @Test
    public void testNumBools1d() {
        List<boolean[]> list = new ArrayList<boolean[]>();
        int i=0;
        try {
        while (true) {
            list.add(new boolean[numVars*2]);
            i++;
        }
        } catch (OutOfMemoryError e) {
            // Clean up.
            list = null;
            System.gc();
        }
        System.out.println(i);
    }
    

    @Test
    public void testNumBools2d() {
        List<boolean[][]> list = new ArrayList<boolean[][]>();
        int i=0;
        try {
        while (true) {
            list.add(new boolean[numVars][2]);
            i++;
        }
        } catch (OutOfMemoryError e) {
            // Clean up.
            list = null;
            System.gc();
        }
        System.out.println(i);
    }
    

    @Test
    public void testNumShorts() {
        List<short[]> list = new ArrayList<short[]>();
        int i=0;
        try {
        while (true) {
            list.add(new short[numVars*2]);
            i++;
        }
        } catch (OutOfMemoryError e) {
            // Clean up.
            list = null;
            System.gc();
        }
        System.out.println(i);
    }
    
}
