package edu.jhu.hltcoe.gridsearch.dmv;

import static org.junit.Assert.assertEquals;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jboss.dna.common.statistic.Stopwatch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.cpt.CptBounds;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.model.dmv.DmvDepTreeGenerator;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel;
import edu.jhu.hltcoe.parse.DmvCkyParserTest;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.train.LocalBnBDmvTrainer;
import edu.jhu.hltcoe.train.LocalBnBDmvTrainer.InitSol;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Time;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.rproj.RDataFrame;
import edu.jhu.hltcoe.util.rproj.RRow;


public class ResDmvDantzigWolfeRelaxationTest {

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

        ResDmvDantzigWolfeRelaxation dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
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

        ResDmvDantzigWolfeRelaxation dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
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

        ResDmvDantzigWolfeRelaxation dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), DEFAULT_SOLUTION_TOLERANCE);

        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
        }
    }

    @Test
    public void testSumsOnManyPosTags() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("Det N");
        sentences.addSentenceFromString("Adj N");
        sentences.addSentenceFromString("Adj N a b c d e f g");
        sentences.addSentenceFromString("Adj N a c d f e b g");
        sentences.addSentenceFromString("Adj N a c d f e b g");
        sentences.addSentenceFromString("Adj N g f e d c b a");

        ResDmvDantzigWolfeRelaxation dw = getDw(sentences);

        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
        assertEquals(0.0, relaxSol.getScore(), DEFAULT_SOLUTION_TOLERANCE);

        double[][] logProbs = relaxSol.getLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            Vectors.exp(logProbs[c]);
            System.out.println(dw.getIdm().getName(c, 0) + " sum=" + Vectors.sum(logProbs[c]));
            Assert.assertTrue(Utilities.lte(Vectors.sum(logProbs[c]), 1.0, 1e-13));
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

        ResDmvDantzigWolfeRelaxation dw = getDw(sentences);
        
        CptBounds bds = dw.getBounds();
        double origLower = bds.getLb(Type.PARAM, 0, 0);
        double origUpper = bds.getUb(Type.PARAM, 0, 0);
                
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
        
        assertEquals(origLower, bds.getLb(Type.PARAM, 0, 0), 1e-7);
        assertEquals(origUpper, bds.getUb(Type.PARAM, 0, 0), 1e-13);
        
    }

    private RelaxedDmvSolution testBoundsHelper(ResDmvDantzigWolfeRelaxation dw, double newL, double newU, boolean forward) {
        
        DmvDantzigWolfeRelaxationTest.adjustBounds(dw, newL, newU, forward);
        
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
    
    @Test 
    public void testFracParseSum() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V P");
        sentences.addSentenceFromString("N V N N N");
        sentences.addSentenceFromString("N V P N");

        ResDmvDantzigWolfeRelaxation dw = getDw(sentences);

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
    public void testMaxSumOfModelParamters() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("D N");
        sentences.addSentenceFromString("A N");
        sentences.addSentenceFromString("N V");
        sentences.addSentenceFromString("N V N N");
        sentences.addSentenceFromString("D N");

        Prng.seed(12345);
        ResDmvDantzigWolfeRelaxation dw = getDw(sentences);
        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
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
        Assert.assertTrue(Utilities.lte(maxSum, 1.0, 1e-13));
    }
    
    @Test
    public void testSemiSupervisedOnSynthetic() {
        DmvModel dmvModel = SimpleStaticDmvModel.getThreePosTagInstance();
        DmvTrainCorpus trainCorpus = DmvCkyParserTest.getSyntheticCorpus(dmvModel); 

        DmvDantzigWolfeRelaxation dw = getDw(trainCorpus);

        RelaxedDmvSolution relaxSol = (RelaxedDmvSolution) dw.solveRelaxation(); 
        assertEquals(-14.866, relaxSol.getScore(), 1e-3);
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
                
        ResDmvDantzigWolfeRelaxation dw = getDw(sentences);
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

        // Do an initial bounds adjustment so that we don't step through any infeasible bounds
        //adjustBounds(dw, DmvBounds.DEFAULT_LOWER_BOUND, DmvBounds.DEFAULT_UPPER_BOUND, true);
        
        RDataFrame df = new RDataFrame();
        Stopwatch timer = new Stopwatch();
        for (double offsetProb = 10e-7; offsetProb <= 1.001; offsetProb += 0.2) {
        //for (double offsetProb = 0.05; offsetProb <= 1.001; offsetProb += 0.05) {
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

    private static ResDmvDantzigWolfeRelaxation getDw(SentenceCollection sentences) {
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);
        return getDw(corpus);
    }

    private static ResDmvDantzigWolfeRelaxation getDw(DmvTrainCorpus corpus) {
        DmvSolution initSol = DmvDantzigWolfeRelaxationTest.getInitFeasSol(corpus);
        System.out.println(initSol);
        ResDmvDantzigWolfeRelaxation dw = new ResDmvDantzigWolfeRelaxation(new File("."));
        dw.init1(corpus);
        dw.init2(initSol);
        return dw;
    }
        
}
