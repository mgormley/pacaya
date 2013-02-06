package edu.jhu.hltcoe.gridsearch.cpt;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.gridsearch.cpt.Projections.ProjectionsPrm;
import edu.jhu.hltcoe.gridsearch.dmv.BasicDmvProjector;
import edu.jhu.hltcoe.gridsearch.dmv.IndexedDmvModel;
import edu.jhu.hltcoe.gridsearch.dmv.IndexedDmvModelTest;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.JUnitUtils;
import edu.jhu.hltcoe.util.Utilities;


public class ProjectionsTest {

    private Projections projections;

    @Before
    public void setUp() {
        projections = new Projections(new ProjectionsPrm());
    }
    
    @Test
    public void testNormalizedProjection() {
        validateNormalizedProjection(new double[]{0.1, 1.7, 0.3});
        validateNormalizedProjection(new double[]{0.2, 0.1, 0.3});
        validateNormalizedProjection(new double[]{14.2, 10.1, 17.3});        
    }
    
    private void validateNormalizedProjection(double[] params) {
        double[] x = Projections.getNormalizedProjection(params, 0.1);        
        System.out.println(Arrays.toString(x));
        Assert.assertEquals(1.0, Vectors.sum(x), 1e-13);
    }    
    
    @Test
    public void testUnboundedProjection() {
        validateUnboundedProjection(new double[]{0.1, 1.7, 0.3});
        validateUnboundedProjection(new double[]{0.2, 0.1, 0.3});
        validateUnboundedProjection(new double[]{14.2, 10.1, 17.3});
    }

    private void validateUnboundedProjection(double[] params) {
        double[] x = Projections.getUnboundedProjection(params);        
        System.out.println(Arrays.toString(x));
        Assert.assertEquals(1.0, Vectors.sum(x), 1e-13);
    }
    
    @Test
    public void testBoundedProjection() throws Exception {
        validateBoundedProjection(new double[][]{{0.1, 1.7, 0.3}, {0.2, 0.2, 0.2}, {0.8, 0.8, 0.8}});
        validateBoundedProjection(new double[][]{{0.2, 0.1, 0.3}, {0.2, 0.2, 0.2}, {0.8, 0.8, 0.8}});
        validateBoundedProjection(new double[][]{{14.2, 10.1, 17.3}, {0.1, 0.2, 0.3}, {0.8, 0.7, 0.6}});
        
        // Test that equal upper/lower bounds work
        validateBoundedProjection(new double[][]{{1.0, 1.0, 1.0}, {0.5, 0.5, 0.0}, {0.5, 0.5, 0.0}});
    }
    
    public void validateBoundedProjection(double[][] all) throws Exception {
        validateBoundedProjection(all[0], all[1], all[2]);
    }

    private void validateBoundedProjection(double[] params, double[] lbs, double[] ubs) throws Exception {
        double[] x = projections.getBoundedProjection(params, lbs, ubs);        
        System.out.println(Arrays.toString(x));
        Assert.assertEquals(1.0, Vectors.sum(x), 1e-13);
        
        for (int m=0; m<params.length; m++) {
            Assert.assertTrue(lbs[m] <= x[m]);
            Assert.assertTrue(x[m] <= ubs[m]);
        }
    }
    
    @Test
    public void testLogBoundedProjection() throws Exception {
        validateLogBoundedProjection(new double[][]{{0.1, 1.7, 0.3}, {0.2, 0.2, 0.2}, {0.8, 0.8, 0.8}});
        validateLogBoundedProjection(new double[][]{{0.2, 0.1, 0.3}, {0.2, 0.2, 0.2}, {0.8, 0.8, 0.8}});
        validateLogBoundedProjection(new double[][]{{14.2, 10.1, 17.3}, {0.1, 0.2, 0.3}, {0.8, 0.7, 0.6}});
        
        // Test that equal upper/lower bounds work
        validateLogBoundedProjection(new double[][]{{1.0, 1.0, 1.0}, {0.5, 0.5, 0.0}, {0.5, 0.5, 0.0}});
    }

    public void validateLogBoundedProjection(double[][] all) throws Exception {
        validateLogBoundedProjection(all[0], all[1], all[2]);
    }

    private void validateLogBoundedProjection(double[] params, double[] lbs, double[] ubs) throws Exception {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V N Adj");
        sentences.addSentenceFromString("N N V Adj");
        IndexedDmvModel idm = IndexedDmvModelTest.getIdm(sentences);
        CptBounds logBounds = new CptBounds(idm);
        Assert.assertEquals(lbs.length, ubs.length);
        int c = 0;
        for (int m=0; m<lbs.length; m++) {
            logBounds.set(Type.PARAM, c, m, Utilities.log(lbs[m]), Utilities.log(ubs[m]));
        }
        
        double[] x = projections.getBoundedProjection(logBounds, c, params);
        System.out.println(Arrays.toString(x));
        Assert.assertEquals(1.0, Vectors.sum(x), 1e-13);
        
        for (int m=0; m<params.length; m++) {
            Assert.assertTrue(lbs[m] <= x[m]);
            Assert.assertTrue(x[m] <= ubs[m]);
        }
    }

    @Test
    public void testLogBoundedProjectionWithRealValues() throws Exception {
        double[] params = new double[] { 0.9067065073041163, 3.3200531208499307E-4, 3.3200531208499307E-4,
                3.3200531208499307E-4, 3.3200531208499307E-4, 3.3200531208499307E-4, 3.3200531208499307E-4,
                3.3200531208499307E-4, 3.3200531208499307E-4, 3.3200531208499307E-4, 0.08997343957503312,
                3.3200531208499307E-4 };

        double[] lbs = new double[] { 1.000000000000001E-12, 1.000000000000001E-12, 1.000000000000001E-12,
                1.000000000000001E-12, 1.000000000000001E-12, 1.000000000000001E-12, 1.000000000000001E-12,
                1.000000000000001E-12, 1.000000000000001E-12, 1.000000000000001E-12, 1.000000000000001E-12,
                1.000000000000001E-12 };
        
        double[] ubs = new double[] {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

        // Part 1
        validateBoundedProjection(params, lbs, ubs);
        
        // Part 2
        SentenceCollection sentences = new SentenceCollection();
        StringBuilder fakeSentence = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            fakeSentence.append(String.valueOf(i));
            if (i < params.length - 1) {
                fakeSentence.append(" ");
            }
        }
        sentences.addSentenceFromString(fakeSentence.toString());
        IndexedDmvModel idm = IndexedDmvModelTest.getIdm(sentences);
        CptBounds logBounds = new CptBounds(idm);
        
        int c = 0;
        double[] x = projections.getBoundedProjection(logBounds, c, params);
        System.out.println(Arrays.toString(x));
        Assert.assertEquals(1.0, Vectors.sum(x), 1e-13);
        
        for (int m=0; m<params.length; m++) {
            Assert.assertTrue(Utilities.exp(logBounds.getLb(Type.PARAM, c, m)) <= x[m]);
            Assert.assertTrue(x[m] <= Utilities.exp(logBounds.getUb(Type.PARAM, c, m)));
        }
    }
    
    @Test
    public void testGetProjectiveParse() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate hat");
        double[] fracRoot = new double[]{0.3, 0.6, 0.1};
        double[][] fracChild = new double[][]{{0.0, 0.3, 0.25},{0.5, 0.0, 0.5}, {0.25, 0.1, 0.0}};
        DepTree t = BasicDmvProjector.getProjectiveParse(sentences.get(0), fracRoot, fracChild);
        System.out.println(t);
        JUnitUtils.assertArrayEquals(new int[]{1,-1,1}, t.getParents());
    }
    
}
