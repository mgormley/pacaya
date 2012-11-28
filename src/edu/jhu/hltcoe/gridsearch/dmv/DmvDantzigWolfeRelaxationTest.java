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
import edu.jhu.hltcoe.gridsearch.cpt.CptBounds;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaList;
import edu.jhu.hltcoe.gridsearch.cpt.LpSumToOneBuilder;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.gridsearch.cpt.LpSumToOneBuilder.CutCountComputer;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.model.dmv.DmvDepTreeGenerator;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.RandomDmvModelFactory;
import edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.DmvCkyParserTest;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.train.LocalBnBDmvTrainer;
import edu.jhu.hltcoe.train.ViterbiTrainer;
import edu.jhu.hltcoe.train.LocalBnBDmvTrainer.InitSol;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Time;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.rproj.RDataFrame;
import edu.jhu.hltcoe.util.rproj.RRow;


public class DmvDantzigWolfeRelaxationTest {

    @BeforeClass
    public static void classSetUp() {
        BasicConfigurator.configure();
        //Logger.getRootLogger().setLevel(Level.TRACE);
    }

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }
        
    @Test
    public void testOneWordSentence() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N");

        DmvDantzigWolfeRelaxation dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
        
        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
            for (int m=0; m<logProbs[c].length; m++) {
                
            }
        }
        assertEquals(0.0, relaxSol.getScore(), 1e-13);
    }
    
    @Test
    public void testThreeWordSentence() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V P");

        DmvDantzigWolfeRelaxation dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), 1e-13);

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

        DmvDantzigWolfeRelaxation dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), 1e-13);

        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
        }
    }

    @Test
    public void testCutsOnManyPosTags() {
        // This seed is just to give us a smaller number of cut rounds, so 
        // that the test runs faster.
        Prng.seed(3);
        
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("Det N");
        sentences.addSentenceFromString("Adj N");
        sentences.addSentenceFromString("Adj N a b c d e f g");
        sentences.addSentenceFromString("Adj N a c d f e b g");
        sentences.addSentenceFromString("Adj N a c d f e b g");
        sentences.addSentenceFromString("Adj N g f e d c b a");

        DmvDantzigWolfeRelaxation dw = getDw(sentences, 20);

        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), 1e-13);

        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
            Assert.assertTrue(Vectors.sum(logProbs[c]) <= LpSumToOneBuilder.DEFAULT_MIN_SUM_FOR_CUTS);
        }
    }
    
    
    @Test
    public void testBounds() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("Det N");
        sentences.addSentenceFromString("Adj N");
        //sentences.addSentenceFromString("N V");
        //sentences.addSentenceFromString("N V N N");
        //sentences.addSentenceFromString("D N");

        DmvDantzigWolfeRelaxation dw = getDw(sentences);
        
        CptBounds bds = dw.getBounds();
        double origLower = bds.getLb(Type.PARAM, 0, 0);
        double origUpper = bds.getUb(Type.PARAM, 0, 0);
        
        double newL, newU;

        newL = Utilities.log(0.11);
        newU = Utilities.log(0.90);
        
        RelaxedDmvSolution relaxSol;
        
        relaxSol = testBoundsHelper(dw, newL, newU, true);
        assertEquals(-1.4750472192095685, relaxSol.getScore(), 1e-13);

        newL = origLower;
        newU = origUpper;
        relaxSol = testBoundsHelper(dw, newL, newU, true);
        assertEquals(0.0, relaxSol.getScore(), 1e-13);
        
        assertEquals(origLower, bds.getLb(Type.PARAM, 0, 0), 1e-7);
        assertEquals(origUpper, bds.getUb(Type.PARAM, 0, 0), 1e-13);
        
    }

    private RelaxedDmvSolution testBoundsHelper(DmvDantzigWolfeRelaxation dw, double newL, double newU, boolean forward) {
        
        adjustBounds(dw, newL, newU, forward);
        
        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 

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

    public static void adjustBounds(DmvRelaxation dw, double newL, double newU, boolean forward) {
        // Adjust bounds
        for (int c=0; c<dw.getIdm().getNumConds(); c++) {
            for (int m=0; m<dw.getIdm().getNumParams(c); m++) {
                CptBounds origBounds = dw.getBounds();
                double lb = origBounds.getLb(Type.PARAM, c, m);
                double ub = origBounds.getUb(Type.PARAM, c, m);

                double deltU = newU - ub;
                double deltL = newL - lb;
                //double mid = Utilities.logAdd(lb, ub) - Utilities.log(2.0);
                CptBoundsDeltaList deltas1 = new CptBoundsDeltaList(new CptBoundsDelta(Type.PARAM, c, m, Lu.UPPER, deltU));
                CptBoundsDeltaList deltas2 = new CptBoundsDeltaList(new CptBoundsDelta(Type.PARAM, c, m, Lu.LOWER, deltL));
                if (forward) {
                    dw.forwardApply(deltas1);
                    dw.forwardApply(deltas2);
                } else {
                    dw.reverseApply(deltas1);
                    dw.reverseApply(deltas2);
                }
                System.out.println("l, u = " + dw.getBounds().getLb(Type.PARAM,c, m) + ", " + dw.getBounds().getUb(Type.PARAM,c, m));
            }
        }
    }
    
    @Test 
    public void testFracParseSum() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V P");
        sentences.addSentenceFromString("N V N N N");
        sentences.addSentenceFromString("N V P N");

        DmvDantzigWolfeRelaxation dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), 1e-13);

        for (int s = 0; s < sentences.size(); s++) {
            double[] fracRoots = relaxSol.getTreebank().getFracRoots()[s];
            double[][] fracChildren = relaxSol.getTreebank().getFracChildren()[s];
            double sum = Vectors.sum(fracChildren) + Vectors.sum(fracRoots);
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
            DmvDantzigWolfeRelaxation dw = getDw(sentences, numCuts);
            RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
            assertEquals(0.0, relaxSol.getScore(), 1e-13);
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
    public void testSemiSupervisedOnSynthetic() {
        DmvModel dmvModel = SimpleStaticDmvModel.getThreePosTagInstance();
        DmvTrainCorpus trainCorpus = DmvCkyParserTest.getSyntheticCorpus(dmvModel); 

        DmvDantzigWolfeRelaxation dw = getDw(trainCorpus, 10);

        DmvSolution initBoundsSol = LocalBnBDmvTrainer.getInitSol(InitSol.VITERBI_EM, trainCorpus, dw, null, null);
        LocalBnBDmvTrainer.setBoundsFromInitSol(dw, initBoundsSol, 0.1, 0.0);
            
        // TODO: is this relaxation really independent of the frequency bounds? That's what seems to be happening.
        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
        assertEquals(-14.813, relaxSol.getScore(), 1e-3);
    }
    
    @Test
    public void testSupervised() {
        DmvModel dmvModel = SimpleStaticDmvModel.getThreePosTagInstance();

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, Prng.nextInt(1000000));
        DepTreebank treebank = generator.getTreebank(10);        
        DmvTrainCorpus corpus = new DmvTrainCorpus(treebank, 1.0);

        // Get the relaxed solution.
        DmvDantzigWolfeRelaxation dwRelax = new DmvDantzigWolfeRelaxation(null, 100, new CutCountComputer());
        dwRelax.setMinSumForCuts(1.000001);
        dwRelax.init1(corpus);
        dwRelax.init2(DmvDantzigWolfeRelaxationTest.getInitFeasSol(corpus));
        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution)dwRelax.solveRelaxation();
        
        // Get the model from a single M-step.
        DmvMStep mStep = new DmvMStep(0.0);
        DmvModel m1 = mStep.getModel(treebank);
                
        DmvObjective obj = new DmvObjective(corpus);
        double m1Obj = obj.computeTrueObjective(m1, treebank);
        
        Assert.assertEquals(m1Obj, relaxSol.getScore(), 1e-4);
    }
    
    @Test
    public void testQualityOfRelaxation() throws IOException {
        
        
        // TODO: use real model and real trees to compute a better
        // lower bound
        
        DmvModel goldModel = SimpleStaticDmvModel.getThreePosTagInstance();
        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(goldModel, Prng.nextInt(1000000));
        DepTreebank goldTreebank = generator.getTreebank(100);
        DmvTrainCorpus corpus = new DmvTrainCorpus(goldTreebank, 0.0);
        System.out.println(goldTreebank);
        System.out.println(goldModel);
        SentenceCollection sentences = goldTreebank.getSentences();
                
        DmvDantzigWolfeRelaxation dw = getDw(sentences, 100);
        IndexedDmvModel idm = dw.getIdm();

        double[][] goldLogProbs = idm.getCmLogProbs(goldModel);
        DmvSolution goldSol = new DmvSolution(goldLogProbs, idm, goldTreebank, dw.computeTrueObjective(goldLogProbs, goldTreebank));            
        
        InitSol opt = InitSol.GOLD;
        DmvSolution initSol = LocalBnBDmvTrainer.getInitSol(opt, corpus, dw, goldTreebank, goldSol);

        StringBuilder sb = new StringBuilder();        
        sb.append("gold score: " + goldSol.getScore() + "\n");
        sb.append("init score: " + initSol.getScore());
        sb.append("\n");
                        
//        for (double offsetProb = 0.0; offsetProb < 0.5; offsetProb += 0.01) {
//            double probOfSkipCm = 0.00;
//            setBoundsFromInitSol(dw, initSol, offsetProb, probOfSkipCm);
//            RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation();
//            
//            sb.append(String.format("offset: +/-%.2f", offsetProb));
//            sb.append(String.format(" skip: %.2f%%", probOfSkipCm*100));
//            sb.append(String.format(" relax bound: %7.2f", relaxSol.getScore()));
//            sb.append(String.format(" relative: %.2f", Math.abs(relaxSol.getScore() - initSol.getScore()) / Math.abs(initSol.getScore())));
//            sb.append("\n");
//        }
//        sb.append("\n");

        RDataFrame df = new RDataFrame();
        Stopwatch timer = new Stopwatch();
        for (double offsetProb = 10e-13; offsetProb <= 1.001; offsetProb += 0.2) {
            for (double probOfSkipCm = 0.0; probOfSkipCm <= 0.2; probOfSkipCm += 0.1) {
                int numTimes = 1; // TODO: revert 2
                double avgScore = 0.0;
                for (int i=0; i<numTimes; i++) {
                    timer.start();
                    LocalBnBDmvTrainer.setBoundsFromInitSol(dw, initSol, offsetProb, probOfSkipCm);
                    RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation();
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

    private boolean containsInitSol(CptBounds bounds, double[][] logProbs) {
        for (int c=0; c<logProbs.length; c++) {
            for (int m=0; m<logProbs[c].length; m++) {
                double logProb = logProbs[c][m];
                if (logProb < CptBounds.DEFAULT_LOWER_BOUND) {
                    logProb = CptBounds.DEFAULT_LOWER_BOUND;
                }
                if (bounds.getLb(Type.PARAM, c, m) > logProb || bounds.getUb(Type.PARAM, c, m) < logProb) {
                    return false;
                }
            }
        }
        return true;
    }

    private DmvDantzigWolfeRelaxation getDw(SentenceCollection sentences) {
        return getDw(sentences, 1);
    }
    
    /**
     * Helper function 
     * @return DW relaxation with 1 round of cuts, and 1 initial cut per parameter
     */
    public static DmvDantzigWolfeRelaxation getDw(SentenceCollection sentences, final int numCuts) {
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);
        return getDw(corpus, numCuts);
    }

    private static DmvDantzigWolfeRelaxation getDw(DmvTrainCorpus corpus, final int numCuts) {
        DmvSolution initSol = getInitFeasSol(corpus);
        System.out.println(initSol);
        CutCountComputer ccc = new CutCountComputer(){ 
            @Override
            public int getNumCuts(int numParams) {
                return numCuts;
            }
        };
        DmvDantzigWolfeRelaxation dw = new DmvDantzigWolfeRelaxation(new File("."), numCuts, ccc);
        dw.init1(corpus);
        dw.init2(initSol);
        return dw;
    }
    
    public static DmvSolution getInitFeasSol(DmvTrainCorpus corpus) {
        int numRestarts = 9;
        return getInitFeasSol(corpus, numRestarts);
    }
    
    public static DmvSolution getInitFeasSol(DmvTrainCorpus corpus, int numRestarts) {
        // Run Viterbi EM to get a reasonable starting incumbent solution
        int iterations = 25;        
        double lambda = 0.1;
        double convergenceRatio = 0.99999;
        double timeoutSeconds = 5;
        
        ViterbiParser parser = new DmvCkyParser();
        DmvMStep mStep = new DmvMStep(lambda);
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        ViterbiTrainer trainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations, convergenceRatio, numRestarts, timeoutSeconds, null);
        // TODO: use random restarts
        trainer.train(corpus);
        
        DepTreebank treebank = trainer.getCounts();
        IndexedDmvModel idm = new IndexedDmvModel(corpus);
        double[][] logProbs = idm.getCmLogProbs((DmvModel)trainer.getModel());
        
        // We let the DmvProblemNode compute the score
        DmvSolution sol = new DmvSolution(logProbs, idm, treebank, trainer.getLogLikelihood());
        return sol;
    }
        
}
