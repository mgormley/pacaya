package edu.jhu.nlp.depparse;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.autodiff.erma.InsideOutsideDepParse;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.inf.BruteForceInferencer;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FeExpFamFactor;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.gm.model.globalfac.LinkVar;
import edu.jhu.gm.train.CrfTrainerTest.SimpleVCFeatureExtractor;
import edu.jhu.nlp.FeTypedFactor;
import edu.jhu.nlp.data.DepEdgeMask;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorGraphBuilderPrm;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.GraFeTypedFactor;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebras;

public class O2AllGraFgInferencerTest {
    
    private boolean oldSingleRoot;

    @Before
    public void setUp() {
        oldSingleRoot = InsideOutsideDepParse.singleRoot;
    }
    
    @After
    public void tearDown() {
        InsideOutsideDepParse.singleRoot = oldSingleRoot;
    }
    
    @Test
    public void testZeroModelSingleRoot() {
        InsideOutsideDepParse.singleRoot = true;
        checkBruteForceEqualsDynamicProgramming(true, Lists.getList("a"));
        checkBruteForceEqualsDynamicProgramming(true, Lists.getList("a", "b"));
        checkBruteForceEqualsDynamicProgramming(true, Lists.getList("a", "b", "c"));
        checkBruteForceEqualsDynamicProgramming(true, Lists.getList("a", "b", "c", "d"));
    }
    
    @Test
    public void testZeroModelMultiRoot() {
        InsideOutsideDepParse.singleRoot = false;
        checkBruteForceEqualsDynamicProgramming(true, Lists.getList("a"));
        checkBruteForceEqualsDynamicProgramming(true, Lists.getList("a", "b"));
        checkBruteForceEqualsDynamicProgramming(true, Lists.getList("a", "b", "c"));
        checkBruteForceEqualsDynamicProgramming(true, Lists.getList("a", "b", "c", "d"));
    }
    
    @Test
    public void testNonzeroModelSingleRoot() {
        InsideOutsideDepParse.singleRoot = true;
        checkBruteForceEqualsDynamicProgramming(false, Lists.getList("a"));
        checkBruteForceEqualsDynamicProgramming(false, Lists.getList("a", "b"));
        checkBruteForceEqualsDynamicProgramming(false, Lists.getList("a", "b", "c"));
        checkBruteForceEqualsDynamicProgramming(false, Lists.getList("a", "b", "c", "d"));
    }
    
    @Test
    public void testNonzeroModelMultiRoot() {
        InsideOutsideDepParse.singleRoot = false;
        checkBruteForceEqualsDynamicProgramming(false, Lists.getList("a"));
        checkBruteForceEqualsDynamicProgramming(false, Lists.getList("a", "b"));
        checkBruteForceEqualsDynamicProgramming(false, Lists.getList("a", "b", "c"));
        checkBruteForceEqualsDynamicProgramming(false, Lists.getList("a", "b", "c", "d"));
    }
    
    private static void checkBruteForceEqualsDynamicProgramming(boolean zeroModel, List<String> words) {
        DepParseFactorGraphBuilderPrm prm = new DepParseFactorGraphBuilderPrm();
        prm.useProjDepTreeFactor = true;
        prm.grandparentFactors = true;
        prm.arbitrarySiblingFactors = false;
        prm.linkVarType = VarType.PREDICTED;
        DepParseFactorGraphBuilder builder = new DepParseFactorGraphBuilder(prm);
        FactorGraph fg = new FactorGraph();
        
        DepEdgeMask depEdgeMask = new DepEdgeMask(words.size(), true);
        AnnoSentence sent = new AnnoSentence();
        sent.setWords(words);
        sent.setDepEdgeMask(depEdgeMask);

        FeatureNames alphabet = new FeatureNames();
        LinkVarFe fe = new LinkVarFe(alphabet);
        fe.obsConfig = new VarConfig();
        builder.build(sent, fe, fg);
        
        FgModel model = new FgModel(1000);
        if (!zeroModel) {
            for (int i=0; i<model.getNumParams(); i++) {
                model.getParams().set(i, Math.log(i+2));// (i*31 % 1009)/100.);
            }
            //model.setRandomStandardNormal();
        }
        fg.updateFromModel(model);
        System.out.println("Factors:");
        for (Factor f : fg.getFactors()) {
            System.out.println(f);
        }
        
        BruteForceInferencer bf = new BruteForceInferencer(fg, Algebras.REAL_ALGEBRA);
        bf.run();
        O2AllGraFgInferencer dp = new O2AllGraFgInferencer(fg, Algebras.REAL_ALGEBRA);
        dp.run();
        
        if (words.size() <= 3) {
            System.out.println("joint: "+bf.getJointFactor().toString(true));
        }

        System.out.println("edgeMarginals: " + dp.getEdgeMarginals());
        double tolerance = 1e-10;
        // Scale is too large: assertEquals(bf.getPartition(), dp.getPartition(), tolerance);
        assertEquals(bf.getLogPartition(), dp.getLogPartition(), tolerance);
        assertEqualMarginals(fg, bf, dp, tolerance, false);
    }
    
    private static class LinkVarFe extends SimpleVCFeatureExtractor implements FeatureExtractor {

        public LinkVarFe(FeatureNames alphabet) {
            super(alphabet);
        }

        @Override
        public FeatureVector calcFeatureVector(FeExpFamFactor f, VarConfig vc) {
            // No features if one of the values is false.
            for (Var v : vc.getVars()) {
                if (vc.getState(v) == LinkVar.FALSE) {
                    return new FeatureVector();
                }
            }
            // Identity features for the other arcs.
            FeatureVector fv = new FeatureVector();
            if (f instanceof FeTypedFactor) {
                LinkVar lv = (LinkVar) f.getVars().get(0);
                fv.add(alphabet.lookupIndex(lv.getParent() + "_" + lv.getChild()), 1.0);
            } else if (f instanceof GraFeTypedFactor) {
                GraFeTypedFactor ff = (GraFeTypedFactor)f;
                fv.add(alphabet.lookupIndex(ff.g + "_" + ff.p + "_" + ff.c), 1.0);
            } else {
                throw new RuntimeException("unsupported factor type");
            }
            return fv;
        }
        
    }
    

    public static void assertEqualMarginals(FactorGraph fg, FgInferencer bf,
            FgInferencer bp, double tolerance, boolean compareGlobalFactors) {
        for (Var var : fg.getVars()) {
            {
                VarTensor bfm = bf.getMarginals(var);
                VarTensor bpm = bp.getMarginals(var);
                if (!bfm.equals(bpm, tolerance)) {
                    assertEquals(bfm, bpm);
                }
            }
            {
                VarTensor bfm = bf.getLogMarginals(var);
                VarTensor bpm = bp.getLogMarginals(var);
                if (!bfm.equals(bpm, tolerance)) {
                    assertEquals(bfm, bpm);
                }
            }
        }
        for (Factor f : fg.getFactors()) {
            if (!compareGlobalFactors && f instanceof GlobalFactor) {
                continue;
            } else if (f instanceof GraFeTypedFactor) {
                // Compare only the value for the marginal where both values are true.
                {
                    VarTensor bfm = bf.getMarginals(f);
                    VarTensor bpm = bp.getMarginals(f);
                    assertEquals(bfm.getValue(LinkVar.TRUE_TRUE), bpm.getValue(LinkVar.TRUE_TRUE), tolerance);
                }
                {
                    VarTensor bfm = bf.getLogMarginals(f);
                    VarTensor bpm = bp.getLogMarginals(f);
                    assertEquals(bfm.getValue(LinkVar.TRUE_TRUE), bpm.getValue(LinkVar.TRUE_TRUE), tolerance);
                }
            } else {
                // Compare full marginals.
                {
                    VarTensor bfm = bf.getMarginals(f);
                    VarTensor bpm = bp.getMarginals(f);                
                    if (!bfm.equals(bpm, tolerance)) {
                        assertEquals(bfm, bpm);
                    }
                }
                {
                    VarTensor bfm = bf.getLogMarginals(f);
                    VarTensor bpm = bp.getLogMarginals(f);
                    if (!bfm.equals(bpm, tolerance)) {
                        assertEquals(bfm, bpm);
                    }
                }
            }
        }
        // Scale is too large: assertEquals(bf.getPartition(), bp.getPartition(), tolerance);
        assertEquals(bf.getLogPartition(), bp.getLogPartition(), tolerance);
    }

}
