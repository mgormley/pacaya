package edu.jhu.srl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.DepTree;
import edu.jhu.data.concrete.SimpleAnnoSentenceCollection;
import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.feat.FeatureTemplateList;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.train.CrfTrainer;
import edu.jhu.gm.train.CrfTrainer.CrfTrainerPrm;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFgExamplesBuilder.SrlFgExampleBuilderPrm;

/**
 * Unit tests for {@link SrlFgExamplesBuilderTest}.
 * @author mgormley
 * @author mmitchell
 */
public class SrlFgExamplesBuilderTest {

    @Test
    public void testGetData() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        
        SimpleAnnoSentenceCollection simpleSents = new SimpleAnnoSentenceCollection();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        for (CoNLL09Sentence s : sents) {
            s.normalizeRoleNames();
            simpleSents.add(s.toSimpleAnnoSentence(csPrm.useGoldSyntax));
        }
        cs.init(simpleSents);
        
        System.out.println("Done reading.");
        FeatureTemplateList fts = new FeatureTemplateList();
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        
        prm.fgPrm.useProjDepTreeFactor = true;
        prm.fePrm.biasOnly = true;
        
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, fts, cs);
        FgExampleList data = builder.getData(simpleSents);
        
//        System.out.println("Num features: " + alphabet.size());
//        FgModel model = new FgModel(alphabet);
//        model = train(model, data);
    }

    @Test
    public void testRoleTrainAssignment() throws Exception {
        FeatureTemplateList fts = new FeatureTemplateList();

        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        SimpleAnnoSentenceCollection simpleSents = new SimpleAnnoSentenceCollection();
        for (CoNLL09Sentence s : sents) {
            s.normalizeRoleNames();
            simpleSents.add(s.toSimpleAnnoSentence(csPrm.useGoldSyntax));
        }
        cs.init(simpleSents);        
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.biasOnly = true;
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        prm.fePrm = fePrm;
        //prm.includeUnsupportedFeatures = 
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.alwaysIncludeLinkVars = true;
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, fts, cs);
        FgExampleList data = builder.getData(simpleSents);
        FgExample ex = data.get(0);
        //assertEquals(1, obsAlphabet.size());
        //assertEquals(6*2 + 2 + 6, fts.size());

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
    public void testPreprocess() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);

        SimpleAnnoSentenceCollection simpleSents = new SimpleAnnoSentenceCollection();
        for (CoNLL09Sentence s : sents) {
            s.normalizeRoleNames();
            simpleSents.add(s.toSimpleAnnoSentence(csPrm.useGoldSyntax));
        }

        cs.init(simpleSents);
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        //fePrm.biasOnly = true;
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        prm.fePrm.useZhaoFeats = false;
        prm.fePrm.useSimpleFeats = false;
        prm.fePrm.useDepPathFeats = false;
        prm.fePrm = fePrm;
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.alwaysIncludeLinkVars = true;
        {
            FeatureTemplateList fts = new FeatureTemplateList();
            prm.exPrm.featCountCutoff = 0;
            SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, fts, cs);
            FgExampleList data = builder.getData(simpleSents);            
            // Used to be:  assertEquals(13388, fts.getNumObsFeats());
            assertEquals(13313, fts.getNumObsFeats());
            FgModel model = new FgModel(data, false);
            // Used to be:  assertEquals(26308, model.getNumParams());
            assertEquals(26168, model.getNumParams());
        }
        {
            FeatureTemplateList fts = new FeatureTemplateList();
            prm.exPrm.featCountCutoff = 5;
            SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, fts, cs);
            FgExampleList data = builder.getData(simpleSents);
            // Used to be:  assertEquals(2729, fts.getNumObsFeats());
            assertEquals(2749, fts.getNumObsFeats());
            FgModel model = new FgModel(data, false);
            // Used to be:  assertEquals(6108, model.getNumParams());
            assertEquals(6113, model.getNumParams());
        }
    }

    @Test
    public void testLinkTrainAssignment() throws Exception {
        FeatureTemplateList fts = new FeatureTemplateList();

        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.biasOnly = true;
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        
        SimpleAnnoSentenceCollection simpleSents = new SimpleAnnoSentenceCollection();
        for (CoNLL09Sentence s : sents) {
            s.normalizeRoleNames();
            simpleSents.add(s.toSimpleAnnoSentence(csPrm.useGoldSyntax));
        }
        
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(simpleSents);        
        
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        prm.fePrm = fePrm;
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.linkVarType = VarType.PREDICTED;
        prm.fgPrm.alwaysIncludeLinkVars = true;
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, fts, cs);
        FgExampleList data = builder.getData(simpleSents);
        FgExample ex = data.get(0);
        
        VarConfig vc = ex.getGoldConfig();
        System.out.println(vc.toString().replace(",", "\n"));

        assertEquals(18 + 19*18 + 19, vc.size());
        
        int[] parents = getParents(sents.get(0).size(), vc);
        System.out.println(Arrays.toString(parents));
        assertTrue(DepTree.checkIsProjective(parents));
        assertArrayEquals(new int[]{2, 2, -1, 4, 2, 4, 7, 5, 7, 8, 7, 14, 11, 11, 10, 14, 17, 15, 2}, parents);
    }

    @Test
    public void testSenseTrainAssignment() throws Exception {
        FeatureTemplateList fts = new FeatureTemplateList();

        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.biasOnly = true;
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        
        SimpleAnnoSentenceCollection simpleSents = new SimpleAnnoSentenceCollection();
        for (CoNLL09Sentence s : sents) {
            s.normalizeRoleNames();
            simpleSents.add(s.toSimpleAnnoSentence(csPrm.useGoldSyntax));
        }
        
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(simpleSents);        
        
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        prm.fePrm = fePrm;
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.predictSense = true;
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, fts, cs);
        FgExampleList data = builder.getData(simpleSents);
        FgExample ex = data.get(0);
        
        VarConfig vc = ex.getGoldConfig();
        System.out.println(vc.toString().replace(",", "\n"));

        // 18 role vars and 1 sense.
        assertEquals(18+1, vc.size());
        SrlGraph srlGraph = SrlDecoder.getSrlGraphFromVarConfig(vc, simpleSents.get(0).size());
        assertEquals(2, srlGraph.getPredAt(2).getPosition());
        assertEquals("fer.a2", srlGraph.getPredAt(2).getLabel());
    }
   
    
    /**
     * Decodes the parents defined by a variable assignment for a single
     * sentence.
     * 
     * @param n The sentence length.
     * @param vc The variable assignment.
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
    
    private static FgModel train(FgModel model, FgExampleList data) {
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
        trainer.train(model, data);
        return model;
    }
}
