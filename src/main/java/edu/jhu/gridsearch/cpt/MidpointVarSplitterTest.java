package edu.jhu.gridsearch.cpt;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.prim.util.math.FastMath;


public class MidpointVarSplitterTest {

    private static class MockCptBounds extends CptBounds {

        public MockCptBounds(IndexedCpt icpt) {
            super(icpt);
        }

        public void reverseApply(CptBoundsDeltaList deltas) {
            applyDeltaList(CptBoundsDeltaList.getReverse(deltas));
        }

        public void forwardApply(CptBoundsDeltaList deltas) {
            applyDeltaList(deltas);
        }
        
        public void applyDeltaList(CptBoundsDeltaList deltas) {
            for (CptBoundsDelta delta : deltas) {
                applyDelta(delta);
            }
        }
                
        public void applyDelta(CptBoundsDelta delta) {
            Type type = delta.getType();
            int c = delta.getC();
            int m = delta.getM();

            double origLb = this.getLb(type, c, m);
            double origUb = this.getUb(type, c, m);
            double newLb = origLb;
            double newUb = origUb;

            if (delta.getLu() == Lu.LOWER) {
                newLb = origLb + delta.getDelta();
            } else if (delta.getLu() == Lu.UPPER) {
                newUb = origUb + delta.getDelta();
            } else {
                throw new IllegalStateException();
            }

            assert newLb <= newUb + 1e-8 : String.format("l,u = %f, %f", newLb, newUb);
            this.set(type, c, m, newLb, newUb);
        }
    }
    
    public static class MockIndexedCpt implements IndexedCpt {
        private int numConds;
        private int numParams;
        public MockIndexedCpt(int numConds, int numParams) {
            this.numConds = numConds;
            this.numParams = numParams;
        }
        @Override
        public int getNumConds() {
            return numConds;
        }
        @Override
        public int getNumParams(int c) {
            return numParams;
        }
        @Override
        public int[][] getTotMaxFreqCm() {
            int[][] totMaxFreqCms = new int[numConds][numParams];
            for (int i=0; i<totMaxFreqCms.length; i++) {
                Arrays.fill(totMaxFreqCms[i], 1);
            }
            return totMaxFreqCms;
        }
        @Override
        public String getName(int c, int m) {
            throw new RuntimeException("This method not be called");
        }
        @Override        
        public int getNumNonZeroUnsupMaxFreqCms() {
            throw new RuntimeException("This method not be called");
        }
        @Override        
        public int getNumTotalParams() {
            throw new RuntimeException("This method not be called");
        }
        @Override        
        public int getTotUnsupervisedMaxFreqCm(int c, int m) {
            throw new RuntimeException("This method not be called");
        }
        @Override
        public int[][] getTotSupervisedFreqCm() {
            // All zeros.
            int[][] totMaxFreqCms = new int[numConds][numParams];
            for (int i=0; i<totMaxFreqCms.length; i++) {
                Arrays.fill(totMaxFreqCms[i], 0);
            }
            return totMaxFreqCms;
        }
        @Override
        public int[][] getTotUnsupervisedMaxFreqCm() {
            throw new RuntimeException("This method not be called");
        }        
    }        

    @Test
    public void testSplitAtMidpointWithTwoParams() {
        MockCptBounds bounds = new MockCptBounds(new MockIndexedCpt(1, 2));
        System.out.println(bounds);
        
        List<CptBoundsDeltaList> deltas = splitAtProb(bounds, 0, 0, 0.4);
        assertEquals(2, deltas.size());
        
        CptBoundsDeltaList uDeltas = deltas.get(0);
        CptBoundsDeltaList lDeltas = deltas.get(1);
        assertEquals(Lu.LOWER, lDeltas.getPrimary().getLu());
        assertEquals(Lu.UPPER, uDeltas.getPrimary().getLu());
        
        for (CptBoundsDelta d : lDeltas) {
            System.out.println(d);
        }
        System.out.println();
        for (CptBoundsDelta d : uDeltas) {
            System.out.println(d);
        }
        
        assertEquals(2, lDeltas.size());
        bounds.forwardApply(lDeltas);
        System.out.println(bounds);
        assertEquals(FastMath.exp(bounds.getLb(Type.PARAM, 0, 0)), 0.4, 1e-9);
        assertEquals(FastMath.exp(bounds.getUb(Type.PARAM, 0, 1)), 0.6, 1e-9);
        bounds.reverseApply(lDeltas);
        
        assertEquals(2, uDeltas.size());
        bounds.forwardApply(uDeltas);
        assertEquals(FastMath.exp(bounds.getUb(Type.PARAM, 0, 0)), 0.4, 1e-9);
        assertEquals(FastMath.exp(bounds.getLb(Type.PARAM, 0, 1)), 0.6, 1e-9);
    }
    
    @Test
    public void testSplitAtMidpointWithFourParams() {
        MockCptBounds bounds = new MockCptBounds(new MockIndexedCpt(2, 4));
        System.out.println(bounds);
        
        List<CptBoundsDeltaList> deltas = splitAtProb(bounds, 0, 0, 0.4);
        assertEquals(2, deltas.size());
        
        CptBoundsDeltaList uDeltas = deltas.get(0);
        CptBoundsDeltaList lDeltas = deltas.get(1);
        assertEquals(Lu.LOWER, lDeltas.getPrimary().getLu());
        assertEquals(Lu.UPPER, uDeltas.getPrimary().getLu());
        
        for (CptBoundsDelta d : lDeltas) {
            System.out.println(d);
        }
        System.out.println();
        for (CptBoundsDelta d : uDeltas) {
            System.out.println(d);
        }
        
        // Test lower bound side
        assertEquals(4, lDeltas.size());
        bounds.forwardApply(lDeltas);
        System.out.println(bounds);
        assertEquals(FastMath.exp(bounds.getLb(Type.PARAM, 0, 0)), 0.4, 1e-9);
        assertEquals(FastMath.exp(bounds.getUb(Type.PARAM, 0, 1)), 0.6, 1e-9);
        assertEquals(FastMath.exp(bounds.getUb(Type.PARAM, 0, 2)), 0.6, 1e-9);
        assertEquals(FastMath.exp(bounds.getUb(Type.PARAM, 0, 3)), 0.6, 1e-9);
        bounds.reverseApply(lDeltas);
        
        // Test upper bound side
        assertEquals(1, uDeltas.size());
        bounds.forwardApply(uDeltas);
        assertEquals(FastMath.exp(bounds.getUb(Type.PARAM, 0, 0)), 0.4, 1e-9);
        bounds.reverseApply(uDeltas);
    }
        
    @Test
    public void testSplitAtMidpointWithInitialLowerBounds() {
        MockCptBounds bounds = new MockCptBounds(new MockIndexedCpt(2, 4));
        setProbBounds(bounds, 0, 1, 0.11, 0.9);
        setProbBounds(bounds, 0, 2, 0.12, 0.9);
        setProbBounds(bounds, 0, 3, 0.13, 0.9);
        System.out.println(bounds);
        
        List<CptBoundsDeltaList> deltas = splitAtProb(bounds, 0, 0, 0.4);
        assertEquals(2, deltas.size());

        CptBoundsDeltaList uDeltas = deltas.get(0);
        CptBoundsDeltaList lDeltas = deltas.get(1);
        assertEquals(Lu.LOWER, lDeltas.getPrimary().getLu());
        assertEquals(Lu.UPPER, uDeltas.getPrimary().getLu());
        
        for (CptBoundsDelta d : lDeltas) {
            System.out.println(d);
        }
        System.out.println();
        for (CptBoundsDelta d : uDeltas) {
            System.out.println(d);
        }
        
        assertEquals(4, lDeltas.size());
        bounds.forwardApply(lDeltas);
        System.out.println(bounds);
        assertEquals(FastMath.exp(bounds.getLb(Type.PARAM, 0, 0)), 0.4, 1e-9);
        assertEquals(FastMath.exp(bounds.getUb(Type.PARAM, 0, 1)), 1 - 0.4 - 0.12 - 0.13, 1e-9);
        assertEquals(FastMath.exp(bounds.getUb(Type.PARAM, 0, 2)), 1 - 0.4 - 0.11 - 0.13, 1e-9);
        assertEquals(FastMath.exp(bounds.getUb(Type.PARAM, 0, 3)), 1 - 0.4 - 0.11 - 0.12, 1e-9);
        bounds.reverseApply(lDeltas);
        
        assertEquals(1, uDeltas.size());
        bounds.forwardApply(uDeltas);
        assertEquals(FastMath.exp(bounds.getUb(Type.PARAM, 0, 0)), 0.4, 1e-9);
    }
    
    @Test
    public void testSplitAtMidpointWithInitialUpperBounds() {
        MockCptBounds bounds = new MockCptBounds(new MockIndexedCpt(2, 4));
        setProbBounds(bounds, 0, 1, 0.11, 0.15);
        setProbBounds(bounds, 0, 2, 0.12, 0.20);
        setProbBounds(bounds, 0, 3, 0.13, 0.3);
        System.out.println(bounds);
        
        List<CptBoundsDeltaList> deltas = splitAtProb(bounds, 0, 0, 0.4);
        assertEquals(2, deltas.size());

        CptBoundsDeltaList uDeltas = deltas.get(0);
        CptBoundsDeltaList lDeltas = deltas.get(1);
        assertEquals(Lu.LOWER, lDeltas.getPrimary().getLu());
        assertEquals(Lu.UPPER, uDeltas.getPrimary().getLu());
        
        for (CptBoundsDelta d : lDeltas) {
            System.out.println(d);
        }
        System.out.println();
        for (CptBoundsDelta d : uDeltas) {
            System.out.println(d);
        }
        
        assertEquals(1, lDeltas.size());
        bounds.forwardApply(lDeltas);
        System.out.println(bounds);
        assertEquals(FastMath.exp(bounds.getLb(Type.PARAM, 0, 0)), 0.4, 1e-9);
        bounds.reverseApply(lDeltas);
        
        assertEquals(3, uDeltas.size());
        bounds.forwardApply(uDeltas);
        assertEquals(FastMath.exp(bounds.getUb(Type.PARAM, 0, 0)), 0.4, 1e-9);
        assertEquals(FastMath.exp(bounds.getLb(Type.PARAM, 0, 1)), 0.11, 1e-9); // This bound remains untouched.
        assertEquals(FastMath.exp(bounds.getLb(Type.PARAM, 0, 2)), 0.15, 1e-9);
        assertEquals(FastMath.exp(bounds.getLb(Type.PARAM, 0, 3)), 0.25, 1e-9);
    }

    private List<CptBoundsDeltaList> splitAtProb(CptBounds bounds, int c, int m, double prob) {
        return MidpointVarSplitter.splitAtMidPoint(bounds, c, m, safeLog(prob));
    }

    private void setProbBounds(CptBounds bounds, int c, int m, double newLbProb, double newUbProb) {
        bounds.set(Type.PARAM, c, m, safeLog(newLbProb), safeLog(newUbProb));
    }

    private double safeLog(double newLbProb) {
        double newLb = FastMath.log(newLbProb);
        if (newLb <= CptBounds.DEFAULT_LOWER_BOUND) {
            return CptBounds.DEFAULT_LOWER_BOUND;
        }
        return newLb;
    }
    
}
