package edu.jhu.hypergraph.depparse;

import org.junit.Test;

import edu.jhu.prim.arrays.Multinomials;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Timer;

public class HyperDepParserSpeedTest {


    /**
     * Output:
     * SEED=123456789101112
     * 100 trials: Tokens per second: 2700.2700270027003
     * 1000 trials: Tokens per second: 5395.6834532374105
     * 
     * After switching to setTailNodes() with Hypernode[] storage.
     * 100 trials: Tokens per second: 2929.6875
     * 1000 trials: Tokens per second: 6651.884700665189
     * 1000 trials (with LogPosNegSemiring): Tokens per second: 4687.5
     */
    @Test
    public void testInsideOutsideSpeed() {
        FastMath.useLogAddTable = true;

        int trials = 1000;
        int n = 30;

        // Just create one tree.
        double[] root = Multinomials.randomMultinomial(n);
        double[][] child = new double[n][];
        for (int i=0; i<n; i++) {
            child[i] =  Multinomials.randomMultinomial(n);
        }
        
        Timer timer = new Timer();
        timer.start();
        for (int t=0; t<trials; t++) {
            HyperDepParser.insideOutsideAlgorithm(root, child);
        }
        timer.stop();
        System.out.println("Total time: " + timer.totMs());
        int numSents = trials;
        int numTokens = n * numSents;
        System.out.println("Sentences per second: " + numSents / timer.totSec());
        System.out.println("Tokens per second: " + numTokens / timer.totSec());
        FastMath.useLogAddTable = false;
    }

    public static void main(String[] args) {
        (new HyperDepParserSpeedTest()).testInsideOutsideSpeed();
    }
}
