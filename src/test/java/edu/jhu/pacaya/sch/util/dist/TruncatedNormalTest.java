package edu.jhu.pacaya.sch.util.dist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.junit.Test;

import edu.jhu.pacaya.sch.util.TestUtils;

public class TruncatedNormalTest {

    private static double tol = 1E-8;

    @Test
    public void testStandard() {
        assertTrue(TestUtils.checkThrows(() -> new TruncatedNormal(null,  0.0,  1.0,  1E-8, 0.0), IllegalArgumentException.class));
        double mu = 0.0;
        for (double sigma : Arrays.asList(1.0, 0.5, 3.0)) {
            NormalDistribution sn = new NormalDistribution(mu, sigma);
            TruncatedNormal tr = new TruncatedNormal(null, mu, sigma, Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY);
            assertTrue(tr.isSupportConnected());
            assertFalse(tr.isSupportLowerBoundInclusive());
            assertFalse(tr.isSupportUpperBoundInclusive());
            assertEquals(Double.NEGATIVE_INFINITY, tr.getSupportLowerBound(), tol);
            assertEquals(Double.POSITIVE_INFINITY, tr.getSupportUpperBound(), tol);
            for (double x : Arrays.asList(0.1, 0.5, 2.3, 0.0, -2.8, -10.0, -50.0, 50.0)) {
                assertEquals(sn.density(x), tr.density(x), tol);
                assertEquals(sn.cumulativeProbability(x), tr.cumulativeProbability(x), tol);
                assertEquals(sn.density(x), TruncatedNormal.densityNonTrunc(x, mu, sigma), tol);
                assertEquals(sn.cumulativeProbability(x), TruncatedNormal.cumulativeNonTrunc(x, mu, sigma), tol);
                for (double y : Arrays.asList(x - 1.5, x - 0.5, x, x + 0.5, x + 1.5)) {
                    if (y < x) continue;
                    assertEquals(sn.probability(x, y), tr.probability(x, y), tol);
                    assertEquals(sn.probability(x, y), TruncatedNormal.probabilityNonTrunc(x, y, mu, sigma), tol);
                }
            }
        }
    }

    @Test
    public void testOneSidedStandard() {
        double mu = 0.0;
        for (double sigma : Arrays.asList(1.0, 0.5, 3.0)) {
            NormalDistribution sn = new NormalDistribution(mu, sigma);
            {
                TruncatedNormal tr = new TruncatedNormal(null, mu, sigma, 0.0, Double.POSITIVE_INFINITY);
                assertEquals(0.0, tr.density(Double.NEGATIVE_INFINITY), tol);
                assertEquals(0.0, tr.density(-0.1), tol);
                assertEquals(0.0, tr.density(-1E-9), tol);
                assertEquals(0.0, tr.cumulativeProbability(Double.NEGATIVE_INFINITY), tol);
                assertEquals(0.0, tr.cumulativeProbability(-0.1), tol);
                assertEquals(0.0, tr.cumulativeProbability(-1E-9), tol);
                for (double x : Arrays.asList(0.1, 0.5, 2.3, 0.0, 50.0)) {
                    assertEquals(2 * sn.density(x), tr.density(x), tol);
                    assertEquals(2 * sn.cumulativeProbability(x) - 1.0, tr.cumulativeProbability(x), tol);
                    for (double y : Arrays.asList(x, x + 0.5, x + 1.5)) {
                        if (y < x) continue;
                        assertEquals(2 * sn.probability(x, y), tr.probability(x, y), tol);
                        assertEquals(tr.probability(x, y), TruncatedNormal.probabilityTruncZero(x, y, mu, sigma), tol);
                    }
                }
            }
            {
                TruncatedNormal tr = new TruncatedNormal(null, 0.0, sigma, Double.NEGATIVE_INFINITY, 0.0);
                assertEquals(0.0, tr.density(Double.POSITIVE_INFINITY), tol);
                assertEquals(0.0, tr.density(0.1), tol);
                assertEquals(0.0, tr.density(1E-9), tol);
                assertEquals(1.0, tr.cumulativeProbability(Double.POSITIVE_INFINITY), tol);
                assertEquals(1.0, tr.cumulativeProbability(0.1), tol);
                assertEquals(1.0, tr.cumulativeProbability(1E-9), tol);
                for (double x : Arrays.asList(0.0, -2.8, -10.0, -50.0)) {
                    assertEquals(2 * sn.density(x), tr.density(x), tol);
                    assertEquals(2 * sn.cumulativeProbability(x), tr.cumulativeProbability(x), tol);
                    for (double y : Arrays.asList(x, x - 0.5, x - 1.5)) {
                        if (y < x) continue;
                        assertEquals(2 * sn.probability(x, y), tr.probability(x, y), tol);
                    }
                }
            }
        }
    }

    @Test
    public void testNonStandard() {
        for (double mu : Arrays.asList(-5.0, -3.0, 0.0, 3.0, 5.0)) {
            for (double sigma : Arrays.asList(1.0, 0.9, 3.0)) {
                NormalDistribution sn = new NormalDistribution(mu, sigma);
                {
                    TruncatedNormal tr = new TruncatedNormal(null, mu, sigma, Double.NEGATIVE_INFINITY,
                            Double.POSITIVE_INFINITY);
                    TruncatedNormal trZeroTrunc = new TruncatedNormal(null, mu, sigma, 0, Double.POSITIVE_INFINITY);
                    assertEquals(0.0, trZeroTrunc.getSupportLowerBound(), tol);
                    assertEquals(Double.POSITIVE_INFINITY, trZeroTrunc.getSupportUpperBound(), tol);
                    assertEquals(mu, tr.getNumericalMean(), tol);
                    assertEquals(sigma*sigma, tr.getNumericalVariance(), tol);
                    assertEquals(mu, tr.getNormalMu(), tol);
                    assertEquals(sigma, tr.getNormalSigma(), tol);
                    for (double x : Arrays.asList(0.1, 0.5, 2.3, 0.0, -2.8, -10.0, -50.0, 50.0)) {
                        assertEquals(sn.density(x), tr.density(x), tol);
                        assertEquals(sn.cumulativeProbability(x), tr.cumulativeProbability(x), tol);
                        assertEquals(new TruncatedNormal(null, mu, sigma, x, Double.POSITIVE_INFINITY).getNumericalMean(), TruncatedNormal.meanTruncLower(mu, sigma, x), 1E-5);
                        for (double y : Arrays.asList(x - 1.5, x - 0.5, x, x + 0.5, x + 1.5)) {
                            if (y < x) continue;
                            assertEquals(sn.probability(x, y), tr.probability(x, y), tol);
                            assertEquals(sn.probability(x, y), TruncatedNormal.probabilityNonTrunc(x, y, mu, sigma), tol);
                            assertEquals(trZeroTrunc.probability(x, y),
                                    TruncatedNormal.probabilityTruncZero(x, y, mu, sigma), tol);
                        }
                    }
                }
            }
        }

    }

}
