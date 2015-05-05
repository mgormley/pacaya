package edu.jhu.pacaya.parse.dep;

import org.junit.Test;

import edu.jhu.prim.arrays.Multinomials;
import edu.jhu.prim.util.Timer;
import edu.jhu.prim.util.math.FastMath;

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
     * Output: (round 2)
     * sum: 2001819.430589211
     * Total time: 5551.0
     * Sentences per second: 1801.477211313277
     * Tokens per second: 54044.316339398305  <<<< 
     */
    @Test
    public void testParseSpeed2() {
        FastMath.useLogAddTable = true;

        int rounds = 2;
        int trials = 10000;
        int n = 30;
        
        double[] root = new double[n];
        double[][] child = new double[n][n];
        for (int i=0; i<n; i++) {
            root[i] = Math.log(i+1);
            for (int j=0; j<n; j++) {
                child[i][j] = Math.log(i*n + j+1);
            }
        }
        
        for (int round=0; round<rounds; round++) {
            Timer timer = new Timer();
            timer.start();
            double sum = 0.0;
            for (int t=0; t<trials; t++) {
                int[] parents = new int[n];
                sum += ProjectiveDependencyParser.parseSingleRoot(root, child, parents);
            }
            System.out.println("sum: " + sum);
            timer.stop();
            System.out.println("Total time: " + timer.totMs());
            int numSents = trials;
            int numTokens = n * numSents;
            System.out.println("Sentences per second: " + numSents / timer.totSec());
            System.out.println("Tokens per second: " + numTokens / timer.totSec());
        }
        FastMath.useLogAddTable = false;
    }
    
    /**
     * Output:
     * SEED=123456789101112
     * 100 trials: Tokens per second: 5338.078291814946
     * 1000 trials: Tokens per second: 11406.84410646388
     * 
     * Case 1: useLogAddTable=false
     * sum: 229809.96390368755
     * Total time: 9836.0
     * Sentences per second: 101.667344448963
     * Tokens per second: 3050.0203334688895
     *
     * Case 2: useLogAddTable=true
     * sum: 229809.96390396365
     * Total time: 8432.0
     * Sentences per second: 118.59582542694497
     * Tokens per second: 3557.874762808349
     * 
     * Case 3: useLogAddTable=true, w/ LogAddTable in place of SmoothedLogAddTable.
     * sum: 229794.61076560934
     * Total time: 2317.0
     * Sentences per second: 431.5925766076823
     * Tokens per second: 12947.777298230469
     */
    @Test
    public void testInsideOutsideSpeed() {
        FastMath.useLogAddTable = false;

        int rounds = 2;
        int trials = 1000;
        int n = 30;
        
        double[] root = new double[n];
        double[][] child = new double[n][n];
        for (int i=0; i<n; i++) {
            root[i] = Math.log(i+1);
            for (int j=0; j<n; j++) {
                child[i][j] = Math.log(i*n + j+1);
            }
        }
        
        for (int round=0; round<rounds; round++) {
            Timer timer = new Timer();
            timer.start();
            double sum = 0.0;
            for (int t=0; t<trials; t++) {
                DepIoChart chart = ProjectiveDependencyParser.insideOutsideSingleRoot(root, child);
                sum += chart.getLogPartitionFunction();
            }
            System.out.println("sum: " + sum);
            timer.stop();
            System.out.println("Total time: " + timer.totMs());
            int numSents = trials;
            int numTokens = n * numSents;
            System.out.println("Sentences per second: " + numSents / timer.totSec());
            System.out.println("Tokens per second: " + numTokens / timer.totSec());
        }
        FastMath.useLogAddTable = false;
    }
    
    public static void main(String[] args) {
        (new ProjectiveDependencyParserSpeedTest()).testInsideOutsideSpeed();
    }
    
}
