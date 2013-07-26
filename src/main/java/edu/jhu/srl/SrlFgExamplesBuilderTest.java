package edu.jhu.srl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.DepTree;
import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.CrfTrainer;
import edu.jhu.gm.CrfTrainer.CrfTrainerPrm;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.FgModel;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFgExamplesBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.util.Alphabet;

/**
 * Unit tests for {@link SrlFgExamplesBuilderTest}.
 * @author mgormley
 */
public class SrlFgExamplesBuilderTest {

    @Test
    public void testGetData() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        
        System.out.println("Done reading.");
        Alphabet<Feature> alphabet = new Alphabet<Feature>();
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        
        prm.fgPrm.useProjDepTreeFactor = true;
        prm.fePrm.biasOnly = true;
        
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, alphabet);
        FgExamples data = builder.getData(sents);
        
//        System.out.println("Num features: " + alphabet.size());
//        FgModel model = new FgModel(alphabet);
//        model = train(model, data);
    }

    @Test
    public void testRoleTrainAssignment() throws Exception {
        Alphabet<Feature> alphabet = new Alphabet<Feature>();        

        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        CorpusStatistics.normalizeRoleNames(sents);
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.biasOnly = true;    
        CorpusStatistics cs = new CorpusStatistics(fePrm);
        cs.init(sents);
                        
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        prm.fePrm = fePrm;
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.alwaysIncludeLinkVars = true;
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, alphabet);
        FgExamples data = builder.getData(sents);
        FgExample ex = data.get(0);
        
        //assertEquals(1, obsAlphabet.size());
        assertEquals(5*2 + 2 + 5, alphabet.size());
        
        VarConfig vc = ex.getGoldConfig();
        System.out.println(vc.toString().replace(",", "\n"));
        for (Var v : vc.getVars()) {
            RoleVar role = (RoleVar) v;
            if (role.getParent() == 2 && role.getChild() == 0) {
                assertEquals("arg0", vc.getStateName(v));
            } else if (role.getParent() == 2 && role.getChild() == 4) {
                assertEquals("arg1", vc.getStateName(v));
            } else {
                assertEquals("_", vc.getStateName(v));
            }
        }
        assertEquals(18, vc.size());
    }


    @Test
    public void testLinkTrainAssignment() throws Exception {
        Alphabet<Feature> alphabet = new Alphabet<Feature>();        

        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.biasOnly = true;
        fePrm.useGoldSyntax = true;
                
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        prm.fePrm = fePrm;
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.linkVarType = VarType.PREDICTED;
        prm.fgPrm.alwaysIncludeLinkVars = true;
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, alphabet);
        FgExamples data = builder.getData(sents);
        FgExample ex = data.get(0);
        
        VarConfig vc = ex.getGoldConfig();
        System.out.println(vc.toString().replace(",", "\n"));

        assertEquals(18 + 19*18 + 19, vc.size());
        
        int[] parents = getParents(sents.get(0).size(), vc);
        System.out.println(Arrays.toString(parents));
        assertTrue(DepTree.checkIsProjective(parents));
        assertArrayEquals(new int[]{2, 2, -1, 4, 2, 4, 7, 5, 7, 8, 7, 14, 11, 11, 10, 14, 17, 15, 2}, parents);
    }

    /**
     * Decodes the parents defined by a variable assignment for a single
     * sentence.
     * 
     * @param n The sentence length.
     * @param vc The variable assignement.
     * @return The parents array.
     */
    // TODO: Maybe use this somewhere? Probably not... we should really decode
    // using MBR decoding over the link marginals.
    private static int[] getParents(int n, VarConfig vc) {
        int[] parents = new int[n];
        Arrays.fill(parents, -2);
        for (Var v : vc.getVars()) {
            if (v instanceof LinkVar) {
                LinkVar link = (LinkVar) v;
                if (vc.getState(v) == LinkVar.TRUE) {
                    if (parents[link.getChild()] != -2) {
                        throw new IllegalStateException(
                                "Multiple link vars define the same parent/child edge. Is this VarConfig for only one example?");
                    }
                    parents[link.getChild()] = link.getParent();
                }
            }
        }
        return parents;
    }
    
    private static FgModel train(FgModel model, FgExamples data) {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.logDomain = true;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        
        CrfTrainerPrm prm = new CrfTrainerPrm();
        prm.infFactory = bpPrm;
        
        // To run with SGD, uncomment these lines.
        //        SGDPrm optPrm = new SGDPrm();
        //        optPrm.iterations = 100;
        //        optPrm.lrAtMidpoint = 0.1;
        //        prm.maximizer = new SGD(optPrm);
        prm.regularizer = null;
        
        CrfTrainer trainer = new CrfTrainer(prm);
        return trainer.train(model, data);
    }
}
