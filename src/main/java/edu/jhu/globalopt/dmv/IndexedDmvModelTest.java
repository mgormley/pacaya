package edu.jhu.globalopt.dmv;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.Sentence;
import edu.jhu.data.SentenceCollection;
import edu.jhu.data.Word;
import edu.jhu.globalopt.dmv.DmvDantzigWolfeRelaxation.DmvDwRelaxPrm;
import edu.jhu.induce.model.dmv.DmvModel;
import edu.jhu.induce.model.dmv.DmvModelFactory;
import edu.jhu.induce.model.dmv.DmvSentParamCache;
import edu.jhu.induce.model.dmv.RandomDmvModelFactory;
import edu.jhu.induce.train.dmv.DmvTrainCorpus;
import edu.jhu.induce.train.dmv.DmvViterbiEMTrainer;
import edu.jhu.induce.train.dmv.DmvViterbiEMTrainer.DmvViterbiEMTrainerPrm;
import edu.jhu.parse.dep.DepParser;
import edu.jhu.parse.dmv.DmvCkyParser;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Prng;


public class IndexedDmvModelTest {

    static {
        Logger.getRootLogger().setLevel(Level.TRACE);
    }

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }
    
    @Test
    public void testCounts() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V");
        sentences.addSentenceFromString("N V N N");
        sentences.addSentenceFromString("D N");
        IndexedDmvModel idm = getIdm(sentences);
                
        assertEquals(1+6+3*2*2, idm.getNumConds());
        // Root
        assertEquals(3, idm.getNumParams(0));
        // Child
        assertEquals(3, idm.getNumParams(4));
        // Decision
        assertEquals(2, idm.getNumParams(10));
        
        assertEquals("child_{N,l,0}(D)", idm.getName(1,2));
        

        
        for (int i=0; i<idm.getNumSentVars(0); i++) {
            int c = idm.getC(0, i);
            int m = idm.getM(0, i);
            System.out.println(idm.getName(c, m));
        }
        assertEquals(2+2+(1+3+1+3), idm.getNumSentVars(0));
        
        int c,m,s;
        // Root: D
        c = 0;
        m = 2;
        s = 2;
        assertEquals("root(D)", idm.getName(c,m));
        // Should exist in sent 2
        assertEquals(1, idm.getMaxFreq(s, idm.getSi(s, c, m)));
        // Should not exists in sent 0 or 1
        assertEquals(-1, idm.getSi(0, c, m));
        assertEquals(-1, idm.getSi(1, c, m));
        
        // Child: Noun --> Noun, Left
        c = 1;
        m = 0;
        s = 1;
        assertEquals("child_{N,l,0}(N)", idm.getName(c,m));
        // Should exist in sent 1
        assertEquals(3, idm.getMaxFreq(s, idm.getSi(s, c, m)));
        // Should not exist in sent 0 or 2 (sent 2 should only have the right edge)
        assertEquals(-1, idm.getSi(0, c, m));
        assertEquals(-1, idm.getSi(2, c, m));
        
        // Decision: 
        c = 1+6;
        m = 0;
        s = 1;
        assertEquals("dec_{N,l,0}(s)", idm.getName(c,m));
        // Should exist in sent 1
        assertEquals(3, idm.getMaxFreq(s, idm.getSi(s, c, m)));
        s=0;
        assertEquals(1, idm.getMaxFreq(s, idm.getSi(s, c, m)));
        s=2;
        assertEquals(1, idm.getMaxFreq(s, idm.getSi(s, c, m)));
        
        int[][] maxFreqCm = idm.getTotMaxFreqCm();
        assertEquals(2, maxFreqCm[0][1]);
    }

    public static IndexedDmvModel getIdm(SentenceCollection sentences) {
        return new IndexedDmvModel(new DmvTrainCorpus(sentences));
    }
    
    @Test
    public void testDepSentenceDist() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V");
        sentences.addSentenceFromString("N V N N");
        sentences.addSentenceFromString("D N");
        IndexedDmvModel idm = getIdm(sentences);

        System.out.print("alphabet: ");
        for (int i=0; i<sentences.getLabelAlphabet().size(); i++){
            System.out.printf("(%d,%s)\n",i,sentences.getLabelAlphabet().lookupObject(i));
        }
        
        
        int s=0;
        double[] sentParams = new double[idm.getNumSentVars(s)];

        assertEquals("child_{N,r,0}(V)", idm.getName(2,1));
        Arrays.fill(sentParams, 0.5);
        // Root --> Verb
        sentParams[idm.getSi(s, 0, 1)] = 0.1;
        // Noun --> Verb, Right
        sentParams[idm.getSi(s, 2, 1)] = 1.0;
        // Noun, Left, Adj, Stop
        sentParams[idm.getSi(s, 7, 0)] = 1.5;
        DmvSentParamCache dsd = idm.getDmvSentParamCache(sentences.get(0), s, sentParams);
        for (int i=0; i<dsd.child.length; i++) {
            for (int j=0; j<dsd.child[i].length; j++) {
                System.out.printf("(%d,%d)=%f\n", i,j,dsd.child[i][j][0]);
            }
        }
        assertEquals(0.1, dsd.root[1], 1e-13);
        assertEquals(1.0, dsd.child[1][0][0], 1e-13);
        assertEquals(1.5, dsd.decision[0][0][0][0], 1e-13);
    }
    
    @Test
    public void testGetDmvModel() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V");
        sentences.addSentenceFromString("N V N N");
        sentences.addSentenceFromString("D N");
        IndexedDmvModel idm = getIdm(sentences);
        
        double[][] logProbs = new double[idm.getNumConds()][];
        for (int c=0; c<logProbs.length; c++) {
            logProbs[c] = new double[idm.getNumParams(c)];
            Assert.assertTrue(logProbs[c].length == 2 || logProbs[c].length == 3);
        }
        
        logProbs[0][1] = FastMath.log(0.2);
        logProbs[1][1] = FastMath.log(0.4);
        logProbs[3][0] = FastMath.log(0.6);
        
        DmvModel model = idm.getDmvModel(logProbs);
        model.convertLogToReal();
        
        assertEquals(0.2, model.getRootWeights().get(new Word("V")), 1e-13);
        assertEquals(0.4, model.getChildWeights(new Word("N"), "l").get(new Word("V")), 1e-13);
        assertEquals(0.6, model.getChildWeights(new Word("V"), "l").get(new Word("N")), 1e-13);
    }
    

    @Test
    public void testGetSentSol() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V");
        sentences.addSentenceFromString("N V N N N");
        sentences.addSentenceFromString("D N");
        IndexedDmvModel idm = getIdm(sentences);
        
        int s = 1;
        Sentence sentence = sentences.get(s);
        DepTree depTree = new DepTree(sentence, new int[]{1, -1, 4, 4, 1}, true);
        
        int[] sentSol = idm.getSentSol(sentence, s, depTree);
        System.out.println("Sentence solution: "); //Arrays.toString(sentSol));
        for (int i=0; i<sentSol.length; i++) {
            int c = idm.getC(s, i);
            int m = idm.getM(s, i);
            System.out.println(i + ": " + idm.getName(c, m) + " " + sentSol[i]);
        }
        
        assertEquals(1, sentSol[1]);
        // noun --> noun left
        assertEquals(2, sentSol[2]);
        // stop right noun adj
        assertEquals(4, sentSol[12]); 
        // cont left noun adj
        assertEquals(1, sentSol[9]); 
        // cont left noun non-adj
        assertEquals(1, sentSol[11]); 
    }
    
    /**
     * Tests getCmLogProbs() and also (indirectly) dwRelax.computeTrueObjective()
     */
    @Test
    public void testGetCmLogProbs() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat");
        sentences.addSentenceFromString("the hat");
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);

        double lambda = 0.0;
        int iterations = 25;
        DepParser parser = new DmvCkyParser();
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        DmvViterbiEMTrainerPrm vtPrm = new DmvViterbiEMTrainerPrm(iterations, 0.99999, 9, 5, lambda, null, parser, modelFactory);
        DmvViterbiEMTrainer trainer = new DmvViterbiEMTrainer(vtPrm);
        // TODO: use random restarts
        trainer.train(corpus);
        double trainerLogLikelihood = trainer.getLogLikelihood();
        DepTreebank treebank = trainer.getCounts();

        DmvDwRelaxPrm prm = new DmvDwRelaxPrm();
        prm.maxCutRounds = 2;
        DmvDantzigWolfeRelaxation dwRelax = new DmvDantzigWolfeRelaxation(prm);
        dwRelax.init1(corpus);
        
        IndexedDmvModel idm = dwRelax.getIdm();//new IndexedDmvModel(sentences);
        double[][] logProbs = idm.getCmLogProbs((DmvModel)trainer.getModel());
        DmvSolution sol = new DmvSolution(logProbs, idm, treebank, Double.NaN);
        
        // Compute the score for the initial solution
        assert(Double.isNaN(sol.getScore()));
        sol.setScore(dwRelax.computeTrueObjective(sol.getLogProbs(), sol.getTreebank()));
        
        System.out.println(treebank);
        System.out.println(trainer.getModel());
        
        System.out.println("logProbs: ");
        for (int c=0; c<logProbs.length; c++) {
            System.out.println(Arrays.toString(DoubleArrays.getExp(logProbs[c])));
        }
        
        // The weights after running Viterbi EM are just wrong
        assertEquals(trainerLogLikelihood, sol.getScore(), 1e-13);
    }
    
}
