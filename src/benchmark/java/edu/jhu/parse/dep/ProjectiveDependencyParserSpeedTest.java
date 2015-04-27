package edu.jhu.parse.dep;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.arrays.Multinomials;
import edu.jhu.prim.util.Timer;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.JUnitUtils;

public class ProjectiveDependencyParserSpeedTest {
    
    /**
     * Output:
     * SEED=123456789101112
     * Total time: 231.0
     * Sentences per second: 432.90043290043286
     * Total time: 183.0
     * Sentences per second: 546.448087431694
     * Total time: 195.0
     * Sentences per second: 512.8205128205128
     * 
     * After adding Inside/Outside:
     * Total time: 317.0
     * Sentences per second: 315.45741324921136
     * Tokens per second: 8955.223880597014
     * 
     * After increasing to 1000 trials:
     * Total time: 916.0
     * Sentences per second: 1091.703056768559
     * Tokens per second: 32751.09170305677
     */
    @Test
    public void testParseSpeed() {
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
            int[] parents = new int[n];
            ProjectiveDependencyParser.parseSingleRoot(root, child, parents);
            //ProjectiveDependencyParser.insideOutsideAlgorithm(root, child);
        }
        timer.stop();
        System.out.println("Total time: " + timer.totMs());
        int numSents = trials;
        int numTokens = n * numSents;
        System.out.println("Sentences per second: " + numSents / timer.totSec());
        System.out.println("Tokens per second: " + numTokens / timer.totSec());
    }

    /**
     * Output:
     * SEED=123456789101112
     * 100 trials: Tokens per second: 5338.078291814946
     * 1000 trials: Tokens per second: 11406.84410646388
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
            ProjectiveDependencyParser.insideOutsideSingleRoot(root, child);
        }
        timer.stop();
        System.out.println("Total time: " + timer.totMs());
        int numSents = trials;
        int numTokens = n * numSents;
        System.out.println("Sentences per second: " + numSents / timer.totSec());
        System.out.println("Tokens per second: " + numTokens / timer.totSec());
        FastMath.useLogAddTable = false;
    }
    
}
