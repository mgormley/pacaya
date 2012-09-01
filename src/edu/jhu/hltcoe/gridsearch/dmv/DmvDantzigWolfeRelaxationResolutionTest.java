package edu.jhu.hltcoe.gridsearch.dmv;

import static org.junit.Assert.assertEquals;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.BasicConfigurator;
import org.jboss.dna.common.statistic.Stopwatch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta.Lu;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.model.dmv.DmvDepTreeGenerator;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelConverter;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.model.dmv.DmvUniformWeightGenerator;
import edu.jhu.hltcoe.model.dmv.DmvWeightGenerator;
import edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.train.ViterbiTrainer;
import edu.jhu.hltcoe.train.LocalBnBDmvTrainer.InitSol;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Time;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.rproj.RDataFrame;
import edu.jhu.hltcoe.util.rproj.RRow;


public class DmvDantzigWolfeRelaxationResolutionTest {

    private static final double DEFAULT_SOLUTION_TOLERANCE = 1e-8;

    @BeforeClass
    public static void classSetUp() {
        //BasicConfigurator.configure();
        //Logger.getRootLogger().setLevel(Level.TRACE);
    }

    public static double[][] tempStaticLogProbs;

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }
    
    @Test
    public void testQuadraticObjectiveInCplex() throws IloException {
        System.out.println("Trying to solve quadratic");
        IloCplex cplex = new IloCplex();
        
        IloNumVar thetaVar = cplex.numVar(-20, 0, "theta");
        IloNumVar edgeVar = cplex.numVar(0, 1, "edge");
        
        cplex.addMinimize(cplex.prod(edgeVar, thetaVar), "obj");
        
        cplex.exportModel(new File("quad.lp").getAbsolutePath());
        
        try {
            cplex.solve();
            Assert.fail();
        } catch (Exception e) {
            // pass
        }
    }
    
    @Test
    public void testOneWordSentence() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N");

        DmvDantzigWolfeRelaxationResolution dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), DEFAULT_SOLUTION_TOLERANCE);
        
        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
            for (int m=0; m<logProbs[c].length; m++) {
                
            }
        }
    }
    
    @Test
    public void testThreeWordSentence() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V P");

        DmvDantzigWolfeRelaxationResolution dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), DEFAULT_SOLUTION_TOLERANCE);

        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
            for (int m=0; m<logProbs[c].length; m++) {
                
            }
        }
    }
    
    @Test
    public void testTwoSentences() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("Det N");
        sentences.addSentenceFromString("Adj N");

        DmvDantzigWolfeRelaxationResolution dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), DEFAULT_SOLUTION_TOLERANCE);

        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
        }
    }

    @Test
    public void testCutsOnManyPosTags() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("Det N");
        sentences.addSentenceFromString("Adj N");
        sentences.addSentenceFromString("Adj N a b c d e f g");
        sentences.addSentenceFromString("Adj N a c d f e b g");
        sentences.addSentenceFromString("Adj N a c d f e b g");
        sentences.addSentenceFromString("Adj N g f e d c b a");

        DmvDantzigWolfeRelaxationResolution dw = getDw(sentences, 15);

        RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), DEFAULT_SOLUTION_TOLERANCE);

        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
            Assert.assertTrue(Vectors.sum(logProbs[c]) <= DmvDantzigWolfeRelaxation.DEFAULT_MIN_SUM_FOR_CUTS);
        }
    }
    
    
    @Test
    public void testBounds() {
        SentenceCollection sentences = new SentenceCollection();
//        sentences.addSentenceFromString("N");
        //TODO: revert back to thses sents
        sentences.addSentenceFromString("Det N");
        sentences.addSentenceFromString("Adj N");
        
        //sentences.addSentenceFromString("N V");
        //sentences.addSentenceFromString("N V N N");
        //sentences.addSentenceFromString("D N");

        DmvDantzigWolfeRelaxationResolution dw = getDw(sentences);
        
        DmvBounds bds = dw.getBounds();
        double origLower = bds.getLb(0, 0);
        double origUpper = bds.getUb(0, 0);
                
        double newL, newU;

        newL = Utilities.log(0.11);
        newU = Utilities.log(0.90);
        
        RelaxedDmvSolution relaxSol;
        
        // Do an initial bounds adjustment so that we don't step through any infeasible bounds
        //adjustBounds(dw, DmvBounds.DEFAULT_LOWER_BOUND, DmvBounds.DEFAULT_UPPER_BOUND, true);

        relaxSol = testBoundsHelper(dw, origLower, origUpper, true);
        tempStaticLogProbs = relaxSol.getLogProbs();
        assertEquals(0.0, relaxSol.getScore(), DEFAULT_SOLUTION_TOLERANCE);
        
        relaxSol = testBoundsHelper(dw, newL, newU, true);
        //TODO: assertEquals(-1.4750472192095685, relaxSol.getScore(), 1e-13);

        relaxSol = testBoundsHelper(dw, origLower, origUpper, true);
        assertEquals(0.0, relaxSol.getScore(), 1e-7);
        
        assertEquals(origLower, bds.getLb(0, 0), 1e-7);
        assertEquals(origUpper, bds.getUb(0, 0), 1e-13);
        
    }

    private RelaxedDmvSolution testBoundsHelper(DmvDantzigWolfeRelaxationResolution dw, double newL, double newU, boolean forward) {
        
        adjustBounds(dw, newL, newU, forward);
        
        RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 

        System.out.println("Printing probabilities");
        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            double[] probs = Vectors.getExp(logProbs[c]);
            //System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
            for (int m=0; m<logProbs[c].length; m++) {
                System.out.println(dw.getIdm().getName(c, m) + "=" + probs[m]);
                // TODO: remove
                // We don't bound the probabilities
                //                Assert.assertTrue(dw.getBounds().getLb(c,m) <= logProbs[c][m]);
                //                Assert.assertTrue(dw.getBounds().getUb(c,m) >= logProbs[c][m]);
            }
            System.out.println("");
        }
        return relaxSol;
    }

    private void adjustBounds(DmvDantzigWolfeRelaxationResolution dw, double newL, double newU, boolean forward) {
        // Adjust bounds
        for (int c=0; c<dw.getIdm().getNumConds(); c++) {
            for (int m=0; m<dw.getIdm().getNumParams(c); m++) {
                DmvBounds origBounds = dw.getBounds();
                double lb = origBounds.getLb(c, m);
                double ub = origBounds.getUb(c, m);

                double deltU = newU - ub;
                double deltL = newL - lb;
                //double mid = Utilities.logAdd(lb, ub) - Utilities.log(2.0);
                DmvBoundsDelta deltas1 = new DmvBoundsDelta(c, m, Lu.UPPER, deltU);
                DmvBoundsDelta deltas2 = new DmvBoundsDelta(c, m, Lu.LOWER, deltL);
                if (forward) {
                    dw.forwardApply(deltas1);
                    dw.forwardApply(deltas2);
                } else {
                    dw.reverseApply(deltas1);
                    dw.reverseApply(deltas2);
                }
                System.out.println("l, u = " + dw.getBounds().getLb(c,m) + ", " + dw.getBounds().getUb(c,m));
            }
        }
    }
    
    @Test 
    public void testFracParseSum() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V P");
        sentences.addSentenceFromString("N V N N N");
        sentences.addSentenceFromString("N V P N");

        DmvDantzigWolfeRelaxationResolution dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), 1e-13);

        for (int s = 0; s < sentences.size(); s++) {
            double sum = Vectors.sum(relaxSol.getFracChildren()[s]) + Vectors.sum(relaxSol.getFracRoots()[s]);
            System.out.println(s + " fracParseSum: " + sum);
            assertEquals(sum, sentences.get(s).size(), 1e-13);
        }
    }
    
    @Test
    public void testAdditionalCuttingPlanes() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("D N");
        sentences.addSentenceFromString("A N");
        sentences.addSentenceFromString("N V");
        sentences.addSentenceFromString("N V N N");
        sentences.addSentenceFromString("D N");

        int maxCuts = 5;
        double[] maxSums = new double[maxCuts];
        double prevSum = Double.POSITIVE_INFINITY;
        for (int numCuts=1; numCuts<maxCuts; numCuts++) {
            Prng.seed(12345);
            DmvDantzigWolfeRelaxationResolution dw = getDw(sentences, numCuts);
            RelaxedDmvSolution relaxSol = dw.solveRelaxation(); 
            assertEquals(0.0, relaxSol.getScore(), DEFAULT_SOLUTION_TOLERANCE);
            double maxSum = 0.0;
            double[][] logProbs = relaxSol.getLogProbs();
            for (int c=0; c<logProbs.length; c++) {
                Vectors.exp(logProbs[c]);
                double sum = Vectors.sum(logProbs[c]);
                if (sum > maxSum) {
                    maxSum = sum;
                }
            }
            maxSums[numCuts] = maxSum;
            System.out.println("maxSums=" + Arrays.toString(maxSums));
            Assert.assertTrue(maxSum <= prevSum);
            prevSum = maxSum;
        }
        System.out.println("maxSums=" + Arrays.toString(maxSums));
    }
    
    @Test
    public void testQualityOfRelaxation() throws IOException {
        
        
        // TODO: use real model and real trees to compute a better
        // lower bound
        
        DmvModel goldModel = SimpleStaticDmvModel.getThreePosTagInstance();
        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(goldModel, Prng.nextInt(1000000));
        DepTreebank goldTreebank = generator.getTreebank(100);
        System.out.println(goldTreebank);
        System.out.println(goldModel);
        SentenceCollection sentences = goldTreebank.getSentences();
                
        DmvDantzigWolfeRelaxationResolution dw = getDw(sentences, 100);
        IndexedDmvModel idm = dw.getIdm();

        double[][] goldLogProbs = idm.getCmLogProbs(DmvModelConverter.getDepProbMatrix(goldModel, sentences.getLabelAlphabet()));
        DmvSolution goldSol = new DmvSolution(goldLogProbs, idm, goldTreebank, dw.computeTrueObjective(goldLogProbs, goldTreebank));            
        
        InitSol opt = InitSol.GOLD;
        DmvSolution initSol;
        if (opt == InitSol.VITERBI_EM) {
            initSol = getInitFeasSol(sentences);
        } else if (opt == InitSol.GOLD) {
            initSol = goldSol;
        } else if (opt == InitSol.RANDOM || opt == InitSol.UNIFORM){
            DmvWeightGenerator weightGen;
            if (opt == InitSol.RANDOM) {
                Prng.seed(System.currentTimeMillis());
                weightGen = new DmvRandomWeightGenerator(0.00001);
            } else {
                weightGen = new DmvUniformWeightGenerator();
            }
            DmvModelFactory modelFactory = new DmvModelFactory(weightGen);
            DmvModel randModel = (DmvModel)modelFactory.getInstance(sentences);
            double[][] logProbs = idm.getCmLogProbs(DmvModelConverter.getDepProbMatrix(randModel, sentences.getLabelAlphabet()));
            ViterbiParser parser = new DmvCkyParser();
            DepTreebank treebank = parser.getViterbiParse(sentences, randModel);
            initSol = new DmvSolution(logProbs, idm, treebank, dw.computeTrueObjective(logProbs, treebank));            
        } else {
            throw new IllegalStateException("unsupported initialization: " + opt);
        }

        StringBuilder sb = new StringBuilder();        
        sb.append("gold score: " + goldSol.getScore() + "\n");
        sb.append("init score: " + initSol.getScore());
        sb.append("\n");
                        
//        for (double offsetProb = 0.0; offsetProb < 0.5; offsetProb += 0.01) {
//            double probOfSkipCm = 0.00;
//            setBoundsFromInitSol(dw, initSol, offsetProb, probOfSkipCm);
//            RelaxedDmvSolution relaxSol = dw.solveRelaxation();
//            
//            sb.append(String.format("offset: +/-%.2f", offsetProb));
//            sb.append(String.format(" skip: %.2f%%", probOfSkipCm*100));
//            sb.append(String.format(" relax bound: %7.2f", relaxSol.getScore()));
//            sb.append(String.format(" relative: %.2f", Math.abs(relaxSol.getScore() - initSol.getScore()) / Math.abs(initSol.getScore())));
//            sb.append("\n");
//        }
//        sb.append("\n");

        // Do an initial bounds adjustment so that we don't step through any infeasible bounds
        //adjustBounds(dw, DmvBounds.DEFAULT_LOWER_BOUND, DmvBounds.DEFAULT_UPPER_BOUND, true);
        
        RDataFrame df = new RDataFrame();
        Stopwatch timer = new Stopwatch();
        for (double offsetProb = 10e-7; offsetProb <= 1.001; offsetProb += 0.05) {
        //for (double offsetProb = 0.05; offsetProb <= 1.001; offsetProb += 0.05) {
            for (double probOfSkipCm = 0.0; probOfSkipCm <= 0.2; probOfSkipCm += 0.1) {
                int numTimes = 1; // TODO: revert 2
                double avgScore = 0.0;
                for (int i=0; i<numTimes; i++) {
                    timer.start();
                    setBoundsFromInitSol(dw, initSol, offsetProb, probOfSkipCm);
                    RelaxedDmvSolution relaxSol = dw.solveRelaxation();
                    avgScore += relaxSol.getScore();
                    timer.stop();
                    System.out.println("Time remaining: " + Time.avgMs(timer)*(numTimes*0.5/0.01*1.0/0.1 - i*offsetProb/0.01*probOfSkipCm/0.1)/1000);
                }
                avgScore /= (double)numTimes;
                
                RRow row = new RRow();
                row.put("offset", offsetProb);
                row.put("skip", probOfSkipCm*100);
                row.put("relaxBound", avgScore);
                row.put("relative", Math.abs(avgScore - initSol.getScore()) / Math.abs(initSol.getScore()));
                row.put("containsGoldSol", containsInitSol(dw.getBounds(), goldSol.getLogProbs()));
                df.add(row);
//                sb.append(String.format("offset: +/-%.2f", offsetProb));
//                sb.append(String.format(" skip: %.2f%%", probOfSkipCm*100));
//                sb.append(String.format(" relax bound: %7.2f", avgScore));
//                sb.append(String.format(" relative: %.2f", Math.abs(avgScore - initSol.getScore()) / Math.abs(initSol.getScore())));
//                sb.append("\n");
            }
        }
        System.out.println(df);
        System.out.println(sb);
        System.out.println("Avg time (ms) per relaxation: " + Time.totMs(timer)/df.getNumRows());
        FileWriter writer = new FileWriter("relax-quality.data");
        df.write(writer);
        writer.close();
    }

    private boolean containsInitSol(DmvBounds bounds, double[][] logProbs) {
        for (int c=0; c<logProbs.length; c++) {
            for (int m=0; m<logProbs[c].length; m++) {
                double logProb = logProbs[c][m];
                if (logProb < DmvBounds.DEFAULT_LOWER_BOUND) {
                    logProb = DmvBounds.DEFAULT_LOWER_BOUND;
                }
                if (bounds.getLb(c, m) > logProb || bounds.getUb(c, m) < logProb) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void setBoundsFromInitSol(DmvDantzigWolfeRelaxationResolution dw, DmvSolution initSol, double offsetProb, double probOfSkipCm) {
        boolean forward = true;
        double offsetLogProb = Utilities.log(offsetProb);
        double[][] logProbs = initSol.getLogProbs();
        
        // Adjust bounds
        for (int c=0; c<dw.getIdm().getNumConds(); c++) {
            for (int m=0; m<dw.getIdm().getNumParams(c); m++) {

                double newL, newU;
                DmvBounds origBounds = dw.getBounds();
                double lb = origBounds.getLb(c, m);
                double ub = origBounds.getUb(c, m);
                
                if (Prng.nextDouble() < probOfSkipCm) {
                    // Don't constrain this variable
                    newL = DmvBounds.DEFAULT_LOWER_BOUND;
                    newU = DmvBounds.DEFAULT_UPPER_BOUND;
                } else {
                    // Constrain the bounds to be +/- offsetLogProb from logProbs[c][m]
                    newU = Utilities.logAdd(logProbs[c][m], offsetLogProb);
                    if (newU > DmvBounds.DEFAULT_UPPER_BOUND) {
                        newU = DmvBounds.DEFAULT_UPPER_BOUND;
                    }
    
                    if (logProbs[c][m] > offsetLogProb) {
                        newL = Utilities.logSubtract(logProbs[c][m], offsetLogProb);                    
                    } else {
                        newL = DmvBounds.DEFAULT_LOWER_BOUND;
                    }
                }
                
                double deltU = newU - ub;
                double deltL = newL - lb;
                //double mid = Utilities.logAdd(lb, ub) - Utilities.log(2.0);
                DmvBoundsDelta deltas1 = new DmvBoundsDelta(c, m, Lu.UPPER, deltU);
                DmvBoundsDelta deltas2 = new DmvBoundsDelta(c, m, Lu.LOWER, deltL);
                if (forward) {
                    dw.forwardApply(deltas1);
                    dw.forwardApply(deltas2);
                } else {
                    dw.reverseApply(deltas1);
                    dw.reverseApply(deltas2);
                }
                System.out.println("l, u = " + dw.getBounds().getLb(c,m) + ", " + dw.getBounds().getUb(c,m));
            }
        }
    }

    private DmvDantzigWolfeRelaxationResolution getDw(SentenceCollection sentences) {
        return getDw(sentences, 1);
    }
    
    /**
     * Helper function 
     * @return DW relaxation with 1 round of cuts, and 1 initial cut per parameter
     */
    public static DmvDantzigWolfeRelaxationResolution getDw(SentenceCollection sentences, final int numCuts) {
        DmvSolution initSol = getInitFeasSol(sentences);
        System.out.println(initSol);
        DmvDantzigWolfeRelaxationResolution dw = new DmvDantzigWolfeRelaxationResolution(new File("."));
        dw.setSentences(sentences);
        dw.init(initSol);
        return dw;
    }
    
    public static DmvSolution getInitFeasSol(SentenceCollection sentences) {
        // Run Viterbi EM to get a reasonable starting incumbent solution
        int iterations = 25;        
        double lambda = 0.1;
        double convergenceRatio = 0.99999;
        int numRestarts = 9;

        ViterbiParser parser = new DmvCkyParser();
        DmvMStep mStep = new DmvMStep(lambda);
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        ViterbiTrainer trainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations, convergenceRatio, numRestarts);
        // TODO: use random restarts
        trainer.train(sentences);
        
        DepTreebank treebank = trainer.getCounts();
        IndexedDmvModel idm = new IndexedDmvModel(sentences);
        DepProbMatrix dpm = DmvModelConverter.getDepProbMatrix((DmvModel)trainer.getModel(), sentences.getLabelAlphabet());
        double[][] logProbs = idm.getCmLogProbs(dpm);
        
        // We let the DmvProblemNode compute the score
        DmvSolution sol = new DmvSolution(logProbs, idm, treebank, trainer.getLogLikelihood());
        return sol;
    }
        
}
