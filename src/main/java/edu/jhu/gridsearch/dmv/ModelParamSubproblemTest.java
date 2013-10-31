package edu.jhu.gridsearch.dmv;

import junit.framework.Assert;


import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.jhu.data.SentenceCollection;
import edu.jhu.gridsearch.cpt.CptBounds;
import edu.jhu.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.util.Pair;
import edu.jhu.util.Prng;

public class ModelParamSubproblemTest {

    @BeforeClass
    public static void classSetUp() {
        // Logger.getRootLogger().setLevel(Level.TRACE);
    }

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }

    @Test
    public void testCorrectnessJOptimizeProb() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("Det N"); 
        sentences.addSentenceFromString("Adj N");
        IndexedDmvModel idm = IndexedDmvModelTest.getIdm(sentences);
        
        ModelParamSubproblem mps = new ModelParamSubproblem();
        CptBounds bounds = new CptBounds(idm);

        // [[-0.0073286529568420125, -0.007328652956842013,
        // -0.007328652956842013], [-0.0, -0.0, -0.0], [-0.0, -0.0, -0.0],
        // [-0.003664326478421006, -0.0, -0.0036643264784210062], [-0.0, -0.0,
        // -0.0], [-0.0, -0.0, -0.0], [-0.0, -0.0, -0.0], [-1.0, -0.0], [-0.0,
        // -0.0], [-0.0, -0.0], [-0.0, -0.0], [-0.0, -0.0], [-0.0, -0.0], [-2.0,
        // -0.0], [-0.0, -0.0], [-1.0, -0.0], [-0.0, -0.0],
        // [-4.3368086899420177E-19, -0.0], [-0.0, -0.0]]

        double[][] weights = {{-0.0073286529568420125, -0.007328652956842013, -0.007328652956842013}, {-0.0, -0.0, -0.0}, {-0.0, -0.0, -0.0}, {-0.003664326478421006, -0.0, -0.0036643264784210062}, {-0.0, -0.0, -0.0}, {-0.0, -0.0, -0.0}, {-0.0, -0.0, -0.0}, {-1.0, -0.0}, {-0.0, -0.0}, {-0.0, -0.0}, {-0.0, -0.0}, {-0.0, -0.0}, {-0.0, -0.0}, {-2.0, -0.0}, {-0.0, -0.0}, {-1.0, -0.0}, {-0.0, -0.0}, {-4.3368086899420177E-19, -0.0}, {-0.0, -0.0}};

        
//        double[][] weights = new double[][] { { -0.0073286529568420125, -0.007328652956842013, -0.007328652956842013 },
//                { -0.003664326478421006, -0.0, -0.0036643264784210062 }, { -0.0, -0.0, -0.0 }, { -1.0, 0.0 },
//                { -2.0, 0.0 }, { -1.0, 0.0 }, {-4.3368086899420177E-19, -0.0} };
        Pair<double[][], Double> mPair = mps.solveModelParamSubproblemJOptimizeProb(weights, bounds);
        double convexGammaPrice = 0.506242970847732;
        
        double[][] logProbs = mPair.get1();
        double mReducedCost = mPair.get2() - convexGammaPrice;

        System.out.println("weights:   " + DoubleArrays.deepToString(weights));
        System.out.println("logProbs:  " + DoubleArrays.deepToString(logProbs));
        System.out.println("redcost: " + mReducedCost);
        Assert.assertEquals(-0.477009091116349, mReducedCost, 1e-13);

        double[][] betterLogProbs = {{-3.1354938439690474, -0.09097181363050562, -3.1354938439690474}, {-1.0986122886681096, -1.0986122886681096, -1.0986122886681096}, {-27.631021115928547, -1.999955756561757E-12, -27.631021115928547}, {-0.6931471805609453, -27.631021115928547, -0.6931471805609453}, {-1.0986122886681096, -1.0986122886681096, -1.0986122886681096}, {-1.0986122886681096, -1.0986122886681096, -1.0986122886681096}, {-27.631021115928547, -1.999955756561757E-12, -27.631021115928547}, {-9.999778782803785E-13, -27.631021115928547}, {-0.6931471805599453, -0.6931471805599453}, {-0.08701139335573368, -2.4849064697608743}, {-9.999778782803785E-13, -27.631021115928547}, {-3.091041820054021, -0.04652004579225024}, {-9.999778782803785E-13, -27.631021115928547}, {-9.999778782803785E-13, -27.631021115928547}, {-0.6931471805599453, -0.6931471805599453}, {-9.999778782803785E-13, -27.631021115928547}, {-0.6931471805599453, -0.6931471805599453}, {-0.08701139335573368, -2.4849064697608743}, {-9.999778782803785E-13, -27.631021115928547}};
        double betterRedCost = ModelParamSubproblem.getReducedCost(weights, betterLogProbs) - convexGammaPrice; // -0.4596183775352521;
        System.out.println("bLogProbs: " + DoubleArrays.deepToString(betterLogProbs));
        System.out.println("betterredcost: " + betterRedCost);
    }

    @Test
    public void testCorrectnessJOptimizeLogProb() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("Det N"); 
        sentences.addSentenceFromString("Adj N");
        IndexedDmvModel idm = IndexedDmvModelTest.getIdm(sentences);
        
        ModelParamSubproblem mps = new ModelParamSubproblem();
        CptBounds bounds = new CptBounds(idm);

        // [[-0.0073286529568420125, -0.007328652956842013,
        // -0.007328652956842013], [-0.0, -0.0, -0.0], [-0.0, -0.0, -0.0],
        // [-0.003664326478421006, -0.0, -0.0036643264784210062], [-0.0, -0.0,
        // -0.0], [-0.0, -0.0, -0.0], [-0.0, -0.0, -0.0], [-1.0, -0.0], [-0.0,
        // -0.0], [-0.0, -0.0], [-0.0, -0.0], [-0.0, -0.0], [-0.0, -0.0], [-2.0,
        // -0.0], [-0.0, -0.0], [-1.0, -0.0], [-0.0, -0.0],
        // [-4.3368086899420177E-19, -0.0], [-0.0, -0.0]]

        double[][] weights = {{-0.0073286529568420125, -0.007328652956842013, -0.007328652956842013}, {-0.0, -0.0, -0.0}, {-0.0, -0.0, -0.0}, {-0.003664326478421006, -0.0, -0.0036643264784210062}, {-0.0, -0.0, -0.0}, {-0.0, -0.0, -0.0}, {-0.0, -0.0, -0.0}, {-1.0, -0.0}, {-0.0, -0.0}, {-0.0, -0.0}, {-0.0, -0.0}, {-0.0, -0.0}, {-0.0, -0.0}, {-2.0, -0.0}, {-0.0, -0.0}, {-1.0, -0.0}, {-0.0, -0.0}, {-4.3368086899420177E-19, -0.0}, {-0.0, -0.0}};

        
//        double[][] weights = new double[][] { { -0.0073286529568420125, -0.007328652956842013, -0.007328652956842013 },
//                { -0.003664326478421006, -0.0, -0.0036643264784210062 }, { -0.0, -0.0, -0.0 }, { -1.0, 0.0 },
//                { -2.0, 0.0 }, { -1.0, 0.0 }, {-4.3368086899420177E-19, -0.0} };
        Pair<double[][], Double> mPair = mps.solveModelParamSubproblemJOptimizeLogProb(weights, bounds);
        double convexGammaPrice = 0.506242970847732;
        
        double[][] logProbs = mPair.get1();
        double mReducedCost = mPair.get2() - convexGammaPrice;
        
        System.out.println("weights:   " + DoubleArrays.deepToString(weights));
        System.out.println("logProbs:  " + DoubleArrays.deepToString(logProbs));
        System.out.println("redcost: " + mReducedCost);
        Assert.assertEquals(-0.47700907394014513, mReducedCost, 1e-13);

        double[][] betterLogProbs = {{-3.1354938439690474, -0.09097181363050562, -3.1354938439690474}, {-1.0986122886681096, -1.0986122886681096, -1.0986122886681096}, {-27.631021115928547, -1.999955756561757E-12, -27.631021115928547}, {-0.6931471805609453, -27.631021115928547, -0.6931471805609453}, {-1.0986122886681096, -1.0986122886681096, -1.0986122886681096}, {-1.0986122886681096, -1.0986122886681096, -1.0986122886681096}, {-27.631021115928547, -1.999955756561757E-12, -27.631021115928547}, {-9.999778782803785E-13, -27.631021115928547}, {-0.6931471805599453, -0.6931471805599453}, {-0.08701139335573368, -2.4849064697608743}, {-9.999778782803785E-13, -27.631021115928547}, {-3.091041820054021, -0.04652004579225024}, {-9.999778782803785E-13, -27.631021115928547}, {-9.999778782803785E-13, -27.631021115928547}, {-0.6931471805599453, -0.6931471805599453}, {-9.999778782803785E-13, -27.631021115928547}, {-0.6931471805599453, -0.6931471805599453}, {-0.08701139335573368, -2.4849064697608743}, {-9.999778782803785E-13, -27.631021115928547}};
        double betterRedCost = ModelParamSubproblem.getReducedCost(weights, betterLogProbs) - convexGammaPrice; // -0.4596183775352521;
        System.out.println("bLogProbs: " + DoubleArrays.deepToString(betterLogProbs));
        System.out.println("betterredcost: " + betterRedCost);
    }
    
}
