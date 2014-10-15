package edu.jhu.nlp.depparse;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.erma.ErmaBp;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.LinkVar;
import edu.jhu.hypergraph.depparse.HyperDepParser;
import edu.jhu.hypergraph.depparse.O2AllGraDpHypergraph.DependencyScorer;
import edu.jhu.hypergraph.depparse.O2AllGraDpHypergraphTest.ExplicitDependencyScorer;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.FeTypedFactor;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReaderSpeedTest;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorGraphBuilderPrm;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.O2FeTypedFactor;
import edu.jhu.nlp.depparse.DepParseFeatureExtractor.DepParseFeatureExtractorPrm;
import edu.jhu.nlp.features.TemplateSets;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.Prng;
import edu.jhu.util.Timer;
import edu.jhu.util.semiring.Algebras;

/**
 * Tests comparing inference in first and second order dep parsing models.
 * @author mgormley
 */
public class DepParseFirstVsSecondOrderTest {
    
    /**
     * Commenting out lines 80 and 83 in FastDepParseFeatureExtractor allows this test to (correctly) pass.
     *   // FastDepParseFe.add2ndOrderSiblingFeats(isent, f2.i, f2.j, f2.k, feats);
     *   // FastDepParseFe.add2ndOrderGrandparentFeats(isent, f2.k, f2.i, f2.j, feats);
     *
     * This compares the behavior a first-order model and a second-order model where the second order model has 
     * some extra "dummy" factors that are only multiplying in the value 1.
     */
    //@Test
    public void testEqualMarginalsAndParentsFirstVsSecondOrder() {
        AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.readPtbYmConllx();

        int numParams = 1000000;
        FgModel model = new FgModel(numParams);
        model.setRandomStandardNormal();
        
        CorpusStatistics cs = new CorpusStatistics(new CorpusStatisticsPrm());
        cs.init(sents);
        FeatureNames alphabet = new FeatureNames();
        boolean onlyFast = true;
        
        int s=0;
        int n=0;

        Timer t = new Timer();
        t.start();
        for (AnnoSentence sent : sents) {
            FactorGraph fg1, fg2;
            ErmaBp bp1, bp2;
            int[] parents1, parents2;            
            {
                // First order
                UFgExample ex = DepParseFactorGraphBuilderSpeedTest.get1stOrderFg(sent, cs, alphabet, numParams, onlyFast);
                fg1 = ex.getFgLatPred();
                fg1.updateFromModel(model);
                bp1 = DepParseInferenceSpeedTest.runBp(fg1);
                DepParseDecoder decode = new DepParseDecoder();
                parents1 = decode.decode(bp1, ex, sent);
            }
            {
                // Second order
                UFgExample ex = get2ndOrderFg(sent, cs, alphabet, numParams, onlyFast);
                fg2 = ex.getFgLatPred();
                fg2.updateFromModel(model);
                bp2 = DepParseInferenceSpeedTest.runBp(fg2);
                DepParseDecoder decode = new DepParseDecoder();
                parents2 = decode.decode(bp2, ex, sent);
            }
            
            assertEqualMarginals(fg1, bp1, fg2, bp2, 1e-7);
            assertArrayEquals(parents1, parents2);
            
            n+=sent.size();
            if (s++%1 == 0) {
                t.stop();
                System.out.println(String.format("s=%d n=%d tot=%7.2f", s, n, n/t.totSec()));
                t.start();
            }
            if (s > 102) {
                break;
            }
        }
        t.stop();
        
        System.out.println("Total secs: " + t.totSec());
        System.out.println("Tokens / sec: " + (sents.getNumTokens() / t.totSec()));
    }
    
    /**
     * Compares marginals and MBR decode for different numbers of BP iterations.
     * 
     * Prng.seed(12345);
     * 2 iters vs. 10 iters: Errors: marginals=93 parents=36
     * 5 iters vs. 10 iters: Errors: marginals=14 parents=2
     * 6 iters vs. 10 iters: Errors: marginals=8 parents=2
     * 9 iters vs. 10 iters: Errors: marginals=1 parents=0
     * 
     * Prng.seed(123456789101112l);
     * 2 iters vs. 10 iters: Errors: marginals=94 parents=51
     * 5 iters vs. 10 iters: Errors: marginals=61 parents=23
     * 6 iters vs. 10 iters: Errors: marginals=42 parents=11
     * 9 iters vs. 10 iters: Errors: marginals=35 parents=16
     */
    @Test
    public void testEqualMarginalsAndParentsNumBpIters() {
        AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.readPtbYmConllx();

        int numParams = 1000000;
        FgModel model = new FgModel(numParams);
        Prng.seed(123456789101112l);
        model.setRandomStandardNormal();
        
        CorpusStatistics cs = new CorpusStatistics(new CorpusStatisticsPrm());
        cs.init(sents);
        FeatureNames alphabet = new FeatureNames();
        boolean onlyFast = true;
        
        int s=0;
        int n=0;

        int numSentsWithDiffMargs = 0;
        int numSentsWithDiffParents = 0;
        
        Timer t = new Timer();
        t.start();
        for (AnnoSentence sent : sents) {
            if (sent.size() > 10) { continue; }
            FactorGraph fg1, fg2;
            ErmaBp bp1, bp2;
            int[] parents1, parents2;            
            {
                // Second order 5 iters
                UFgExample ex = get2ndOrderFg(sent, cs, alphabet, numParams, onlyFast);
                fg1 = ex.getFgLatPred();
                fg1.updateFromModel(model);
                bp1 = DepParseInferenceSpeedTest.runBp(fg1, 6);
                DepParseDecoder decode = new DepParseDecoder();
                parents1 = decode.decode(bp1, ex, sent);
            }
            {
                // Second order 10 iters
                UFgExample ex = get2ndOrderFg(sent, cs, alphabet, numParams, onlyFast);
                fg2 = ex.getFgLatPred();
                fg2.updateFromModel(model);
                bp2 = DepParseInferenceSpeedTest.runBp(fg2, 10);
                DepParseDecoder decode = new DepParseDecoder();
                parents2 = decode.decode(bp2, ex, sent);
            }
            
            try {
                assertEqualMarginals(fg1, bp1, fg2, bp2, 1);
            } catch (AssertionError e) {
                numSentsWithDiffMargs++;
            }
            try {
                assertArrayEquals(parents1, parents2);
            } catch (AssertionError e) {
                numSentsWithDiffParents++;
            }
            
            n+=sent.size();
            if (s++%1 == 0) {
                t.stop();
                System.out.println(String.format("s=%d n=%d tot=%7.2f", s, n, n/t.totSec()));
                t.start();
            }
            if (s > 102) {
                break;
            }
        }
        t.stop();
        
        System.out.printf("Errors: marginals=%d parents=%d\n", numSentsWithDiffMargs, numSentsWithDiffParents);
        System.out.println("Total secs: " + t.totSec());
        System.out.println("Tokens / sec: " + (sents.getNumTokens() / t.totSec()));
    }

    public static UFgExample get2ndOrderFg(AnnoSentence sent, CorpusStatistics cs, FeatureNames alphabet, int numParams, boolean onlyFast) {
        FactorGraph fg = new FactorGraph();
        DepParseFeatureExtractorPrm fePrm = new DepParseFeatureExtractorPrm();
        fePrm.featureHashMod = numParams;
        fePrm.firstOrderTpls = TemplateSets.getFromResource(TemplateSets.mcdonaldDepFeatsResource);
        FeatureExtractor fe = onlyFast?
                new FastDepParseFeatureExtractor(sent, cs, numParams, alphabet) :
                new DepParseFeatureExtractor(fePrm, sent, cs, alphabet);
        
        DepParseFactorGraphBuilderPrm fgPrm = new DepParseFactorGraphBuilderPrm();
        fgPrm.grandparentFactors = true;
        fgPrm.siblingFactors = true;    
        DepParseFactorGraphBuilder builder = new DepParseFactorGraphBuilder(fgPrm);
        builder.build(sent, fe, fg);
        
        UnlabeledFgExample ex = new UnlabeledFgExample(fg, new VarConfig());
        return ex;
    }
    
    @Test
    public void testExactVsApproxInf2ndOrderGraOnly() {
        AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.readPtbYmConllx();

        int numParams = 1000000;
        FgModel model = new FgModel(numParams);
        Prng.seed(123456789101112l);
        model.setRandomStandardNormal();
        
        CorpusStatistics cs = new CorpusStatistics(new CorpusStatisticsPrm());
        cs.init(sents);
        FeatureNames alphabet = new FeatureNames();
        boolean onlyFast = true;
        
        int s=0;
        int n=0;

        double sumDiffs = 0;
        int diffCount = 0;
        
        Timer t = new Timer();
        t.start();
        for (AnnoSentence sent : sents) {
            if (sent.size() > 10) { continue; }
            FactorGraph fg1, fg2;
            EdgeScores es1, es2;
            {
                // Second order 10 iters
                UFgExample ex = get2ndOrderGraOnlyFg(sent, cs, alphabet, numParams, onlyFast);
                fg1 = ex.getFgLatPred();
                fg1.updateFromModel(model);
                ErmaBp bp1 = DepParseInferenceSpeedTest.runBp(fg1, 10);
                es1 = DepParseDecoder.getEdgeScores(bp1, fg1, sent.size()).get1();
            }
            {
                // Second order exact
                UFgExample ex = get2ndOrderGraOnlyFg(sent, cs, alphabet, numParams, onlyFast);
                fg2 = ex.getFgLatPred();
                fg2.updateFromModel(model);
                DependencyScorer scorer = getDepScorerFromFg(fg2);
                es2 = HyperDepParser.insideOutsideO2AllGra(scorer, Algebras.LOG_SEMIRING);
            }
            
            for (int p=-1; p<sent.size(); p++) {
                for (int c=0; c<sent.size(); c++) {
                    if (p == c) { continue; } 
                    sumDiffs += Math.abs(es1.getScore(p, c) - es2.getScore(p, c));
                    diffCount++;
                    assert !Double.isInfinite(sumDiffs) && !Double.isNaN(sumDiffs);
                }
            }            
            
            n+=sent.size();
            if (s++%1 == 0) {
                t.stop();
                System.out.println(String.format("s=%d n=%d tot=%7.2f", s, n, n/t.totSec()));
                t.start();
            }
            if (s > 102) {
                break;
            }
        }
        t.stop();
        
        System.out.printf("Avg diff=%f\n", sumDiffs / diffCount);
        System.out.println("Total secs: " + t.totSec());
        System.out.println("Tokens / sec: " + (sents.getNumTokens() / t.totSec()));
    }
    
    public DependencyScorer getDepScorerFromFg(FactorGraph fg) {
        int n = -1;
        for (Var v : fg.getVars()) {
            LinkVar lv = (LinkVar) v;
            n = Math.max(n, lv.getChild()+1);
            n = Math.max(n, lv.getParent()+1);
        }
        
        double[][][] scores = new double[n+1][n+1][n+1];
        DoubleArrays.fill(scores, 0.0);
        for (Factor f : fg.getFactors()) {
            if (f instanceof O2FeTypedFactor) {
                O2FeTypedFactor ff = (O2FeTypedFactor) f;
                scores[ff.i+1][ff.j+1][ff.k+1] = ff.getLogUnormalizedScore(3); 
            } else if (f instanceof FeTypedFactor) {
                FeTypedFactor ff = (FeTypedFactor) f;
                LinkVar lv = (LinkVar) ff.getVars().get(0);
                int i = lv.getParent() + 1;
                int j = lv.getChild() + 1;
                if (i == 0) {
                    for (int g=0; g<n+1; g++) {
                        scores[j][g][i] += ff.getLogUnormalizedScore(LinkVar.TRUE);
                    }
                } else {
                    for (int g=0; g<n+1; g++) {
                        if (i <= g && g <= j) { continue; }
                        scores[i][j][g] += ff.getLogUnormalizedScore(LinkVar.TRUE);
                    }
                }
            }
        }
        
        return new ExplicitDependencyScorer(scores, n);
    }

    public static UFgExample get2ndOrderGraOnlyFg(AnnoSentence sent, CorpusStatistics cs, FeatureNames alphabet, int numParams, boolean onlyFast) {
        FactorGraph fg = new FactorGraph();
        DepParseFeatureExtractorPrm fePrm = new DepParseFeatureExtractorPrm();
        fePrm.featureHashMod = numParams;
        fePrm.firstOrderTpls = TemplateSets.getFromResource(TemplateSets.mcdonaldDepFeatsResource);
        FeatureExtractor fe = onlyFast?
                new FastDepParseFeatureExtractor(sent, cs, numParams, alphabet) :
                new DepParseFeatureExtractor(fePrm, sent, cs, alphabet);
        
        DepParseFactorGraphBuilderPrm fgPrm = new DepParseFactorGraphBuilderPrm();
        fgPrm.grandparentFactors = false;
        fgPrm.siblingFactors = false;    
        DepParseFactorGraphBuilder builder = new DepParseFactorGraphBuilder(fgPrm);
        builder.build(sent, fe, fg);
        
        UnlabeledFgExample ex = new UnlabeledFgExample(fg, new VarConfig());
        return ex;
    }
    
    private void assertEqualMarginals(FactorGraph fg1, FgInferencer bp1, FactorGraph fg2, FgInferencer bp2, double tolerance) {
        for (Var var1 : fg1.getVars()) {
            LinkVar lv1 = (LinkVar) var1;
            LinkVar lv2 = null;
            for (Var var2 : fg2.getVars()) {
                lv2 = (LinkVar) var2;
                if (lv1.getChild() == lv2.getChild() && lv1.getParent() == lv2.getParent()) {
                    break;
                } else {
                    lv2 = null;
                }
            }
            {
                VarTensor m1 = bp1.getMarginals(lv1);
                VarTensor m2 = bp2.getMarginals(lv2);
                // Ignore vars when testing equality
                if (!Tensor.equals(m1, m2, tolerance)) {
                    assertEquals(m1, m2);
                }
            }
            {
                VarTensor m1 = bp1.getLogMarginals(lv1);
                VarTensor m2 = bp2.getLogMarginals(lv2);
                if (!Tensor.equals(m1, m2, tolerance)) {
                    assertEquals(m1, m2);
                }
            }
        }
        //assertEquals(bp1.getPartition(), bp2.getPartition(), tolerance);
        assertEquals(bp1.getLogPartition(), bp2.getLogPartition(), tolerance);
    }
    
    public static void main(String[] args) {
        (new DepParseFirstVsSecondOrderTest()).testEqualMarginalsAndParentsNumBpIters();
    }
    
}
