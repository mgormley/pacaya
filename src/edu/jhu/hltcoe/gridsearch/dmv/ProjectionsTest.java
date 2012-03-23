package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.JUnitUtils;


public class ProjectionsTest {

    private Projections projections;

    @Before
    public void setUp() {
        projections = new Projections();
    }
    
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
        double[] x = projections.getProjectedParams(y, lbs, ubs);        
        System.out.println(Arrays.toString(x));
        Assert.assertEquals(1.0, Vectors.sum(x), 1e-13);
        
        for (int m=0; m<y.length; m++) {
            Assert.assertTrue(lbs[m] <= x[m]);
            Assert.assertTrue(x[m] <= ubs[m]);
        }
    }
    
    @Test
    public void testProjectiveParse() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate hat");
        double[] fracRoot = new double[]{0.3, 0.6, 0.1};
        double[][] fracChild = new double[][]{{0.0, 0.3, 0.25},{0.5, 0.0, 0.5}, {0.25, 0.1, 0.0}};
        DepTree t = Projections.getProjectiveParse(sentences.get(0), fracRoot, fracChild);
        System.out.println(t);
        JUnitUtils.assertArrayEquals(new int[]{1,-1,1}, t.getParents());
    }
    
    
}
