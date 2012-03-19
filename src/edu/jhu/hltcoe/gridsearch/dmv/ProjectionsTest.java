package edu.jhu.hltcoe.gridsearch.dmv;

import ilog.concert.IloException;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

import edu.jhu.hltcoe.math.Vectors;


public class ProjectionsTest {

    @Test
    public void testProjsplx() {
        testParams(new double[]{0.1, 1.7, 0.3});
        testParams(new double[]{0.2, 0.1, 0.3});
        testParams(new double[]{14.2, 10.1, 17.3});
    }

    private void testParams(double[] y) {
        double[] x = Projections.getProjectedParams(y);        
        System.out.println(Arrays.toString(x));
        Assert.assertEquals(1.0, Vectors.sum(x), 1e-13);
    }
    
    @Test
    public void testCplexProjection() throws Exception {
        testParams2(new double[][]{{0.1, 1.7, 0.3}, {0.2, 0.2, 0.2}, {0.8, 0.8, 0.8}});
        testParams2(new double[][]{{0.2, 0.1, 0.3}, {0.2, 0.2, 0.2}, {0.8, 0.8, 0.8}});
        testParams2(new double[][]{{14.2, 10.1, 17.3}, {0.1, 0.2, 0.3}, {0.8, 0.7, 0.6}});
    }
    
    public void testParams2(double[][] all) throws Exception {
        testParams2(all[0], all[1], all[2]);
    }

    private void testParams2(double[] y, double[] lbs, double[] ubs) throws Exception {
        double[] x = Projections.getProjectedParams(y, lbs, ubs);        
        System.out.println(Arrays.toString(x));
        Assert.assertEquals(1.0, Vectors.sum(x), 1e-13);
        
        for (int m=0; m<y.length; m++) {
            Assert.assertTrue(lbs[m] <= x[m]);
            Assert.assertTrue(x[m] <= ubs[m]);
        }
    }
    
}
