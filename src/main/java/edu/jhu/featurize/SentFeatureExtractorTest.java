package edu.jhu.featurize;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.concrete.SimpleAnnoSentence;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.util.collections.Lists;

public class SentFeatureExtractorTest {

    @Test
    public void testGetParentsAndUseGoldSyntax() {
        CoNLL09Sentence sent = getDogConll09Sentence();
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        {
            // Test with gold syntax.
            CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
            csPrm.useGoldSyntax = true;
            CorpusStatistics cs = new CorpusStatistics(csPrm);
            SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
            cs.init(Lists.getList(simpleSent));
            SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
            int[] goldParents = fe.getParents(simpleSent);
            assertArrayEquals(new int[] { 1, 2, -1, 2 }, goldParents);
        }
        {
            // Test without gold syntax.
            CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
            csPrm.useGoldSyntax = false;
            CorpusStatistics cs = new CorpusStatistics(csPrm);
            SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
            cs.init(Lists.getList(simpleSent));
            SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
            int[] predParents = fe.getParents(simpleSent);
            assertArrayEquals(new int[] { 2, 0, -1, 2 }, predParents);
        }
    }
    
    @Test
    public void testAddZhaoFeatures() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.withSupervision = false;
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);

        ArrayList<String> allFeats = new ArrayList<String>();
        for (int i = 0; i < sent.size(); i++) {
            for (int j = 0; j < sent.size(); j++) {
                fe.addZhaoPairFeatures(i, j, allFeats);
            }
        }
        for (String f : allFeats) {
            System.out.println(f);
        }
        //Check that POS is not gold POS
        fePrm.withSupervision = true;
        fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
        allFeats = new ArrayList<String>();
        for (int i = 0; i < sent.size(); i++) {
            for (int j = 0; j < sent.size(); j++) {
                fe.addZhaoPairFeatures(i, j, allFeats);
            }
        }
    }
    
    @Test
    public void testZhaoPathFeatures() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
        int[] parents = fe.getParents(simpleSent);
        FeatureObject zhaoPred = new FeatureObject(1, parents, simpleSent);
        FeatureObject zhaoArg = new FeatureObject(0, parents, simpleSent);
        FeatureObject zhaoLink = new FeatureObject(1, 0, zhaoPred, zhaoArg, parents);
        List<Pair<Integer, Dir>> desiredDpPathShare = new ArrayList<Pair<Integer, Dir>>();
        desiredDpPathShare.add(new Pair<Integer, Dir>(1,Dir.UP));
        List<Pair<Integer, Dir>> observedDpPathShare = zhaoLink.getDpPathShare();
        System.out.println(observedDpPathShare);
        assertEquals(desiredDpPathShare,observedDpPathShare);
    }
    
    @Test
    public void testZhaoObjectPos() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
        int[] parents = fe.getParents(simpleSent);
        FeatureObject zhaoPred = new FeatureObject(3, parents, simpleSent);
        FeatureObject zhaoArg = new FeatureObject(4, parents, simpleSent);

        String predPos = zhaoPred.getPos();
        String argPos = zhaoArg.getPos();
        assertEquals(predPos,argPos,"p");
        
        csPrm.useGoldSyntax = false;
        cs = new CorpusStatistics(csPrm);
        simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        fePrm = new SentFeatureExtractorPrm();
        fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
        parents = fe.getParents(simpleSent);
        zhaoPred = new FeatureObject(3, parents, simpleSent);
        zhaoArg = new FeatureObject(4, parents, simpleSent);
        
        predPos = zhaoPred.getPos();
        argPos = zhaoArg.getPos();
        
        assertEquals(predPos,"p");
        assertEquals(argPos,"WRONG");
    }
    
    @Test
    public void testZhaoObjectFeat() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
        int[] parents = fe.getParents(simpleSent);
        FeatureObject zhaoPred = new FeatureObject(3, parents, simpleSent);
        FeatureObject zhaoArg = new FeatureObject(4, parents, simpleSent);
        List<String> predFeat = zhaoPred.getFeat();
        List<String> argFeat = zhaoArg.getFeat();
        ArrayList<String> intendedPredFeats = new ArrayList<String>();
        intendedPredFeats.add("postype=relative");
        intendedPredFeats.add("gen=c");
        intendedPredFeats.add("num=c");
        intendedPredFeats.add("NO_MORPH");
        intendedPredFeats.add("NO_MORPH");
        intendedPredFeats.add("NO_MORPH");
        assertEquals(predFeat,intendedPredFeats);
        ArrayList<String> intendedArgFeats = new ArrayList<String>();
        intendedArgFeats.add("NO_MORPH");
        intendedArgFeats.add("NO_MORPH");
        intendedArgFeats.add("NO_MORPH");
        intendedArgFeats.add("NO_MORPH");
        intendedArgFeats.add("NO_MORPH");
        intendedArgFeats.add("NO_MORPH");
        System.out.println(argFeat);
        assertEquals(argFeat,intendedArgFeats);
    }
    
    @Test
    public void testZhaoObjectPathSentence1() {
        CoNLL09Sentence sent = getSpanishConll09Sentence1();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
        int[] parents = fe.getParents(simpleSent);
        
        // Example indices.
        FeatureObject zhaoPred = new FeatureObject(3, parents, simpleSent);
        FeatureObject zhaoArg = new FeatureObject(4, parents, simpleSent);
        FeatureObject zhaoLink = new FeatureObject(3, 4, zhaoPred, zhaoArg, parents);

        // Path between two indices.
        ArrayList<Pair<Integer, Dir>> expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(3, Dir.UP));
        expectedPath.add(new Pair<Integer, Dir>(1, Dir.DOWN));
        List<Pair<Integer, Dir>> seenPath = zhaoLink.getDependencyPath();
        assertEquals(expectedPath,seenPath);

        // Shared path to root for two indices.
        List<Pair<Integer, Dir>> dpPathShare = zhaoLink.getDpPathShare();
        ArrayList<Pair<Integer, Dir>> expectedDpPathShare = new ArrayList<Pair<Integer, Dir>>();
        expectedDpPathShare.add(new Pair<Integer, Dir>(1, Dir.UP));
        assertEquals(dpPathShare,expectedDpPathShare);

        // New example indices.
        zhaoPred = new FeatureObject(0, parents, simpleSent);
        zhaoArg = new FeatureObject(4, parents, simpleSent);
        zhaoLink = new FeatureObject(0, 4, zhaoPred, zhaoArg, parents);

        // Path between two indices.
        expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(0, Dir.UP));
        expectedPath.add(new Pair<Integer, Dir>(1, Dir.DOWN));
        seenPath = zhaoLink.getDependencyPath();
        assertEquals(expectedPath,seenPath);        

        // Shared path to root for two indices.
        dpPathShare = zhaoLink.getDpPathShare();
        expectedDpPathShare = new ArrayList<Pair<Integer, Dir>>();
        expectedDpPathShare.add(new Pair<Integer, Dir>(1, Dir.UP));
        assertEquals(dpPathShare,expectedDpPathShare);
        
        // Line path (consecutive indices between two).
        ArrayList<Integer> linePath = zhaoLink.getLinePath();
        ArrayList<Integer> expectedLinePath = new ArrayList<Integer>();
        expectedLinePath.add(0);
        expectedLinePath.add(1);
        expectedLinePath.add(2);
        expectedLinePath.add(3);
        assertEquals(linePath,expectedLinePath);
    }
        
    @Test
    public void testZhaoObjectPathSentence2() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
        int[] parents = fe.getParents(simpleSent);
        
        // Example indices.
        FeatureObject zhaoPred = new FeatureObject(3, parents, simpleSent);
        FeatureObject zhaoArg = new FeatureObject(4, parents, simpleSent);
        FeatureObject zhaoLink = new FeatureObject(3, 4, zhaoPred, zhaoArg, parents);
        
        // Path between two indices.
        ArrayList<Pair<Integer, Dir>> expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(3, Dir.UP));
        expectedPath.add(new Pair<Integer, Dir>(5, Dir.DOWN));
        List<Pair<Integer, Dir>> seenPath = zhaoLink.getDependencyPath();
        assertEquals(expectedPath,seenPath);

        // Shared path to root for two indices.
        List<Pair<Integer, Dir>> dpPathShare = zhaoLink.getDpPathShare();
        ArrayList<Pair<Integer, Dir>> expectedDpPathShare = new ArrayList<Pair<Integer, Dir>>();
        expectedDpPathShare.add(new Pair<Integer, Dir>(5, Dir.UP));
        expectedDpPathShare.add(new Pair<Integer, Dir>(1, Dir.UP));
        assertEquals(dpPathShare,expectedDpPathShare);
        
        // New example indices.
        zhaoPred = new FeatureObject(0, parents, simpleSent);
        zhaoArg = new FeatureObject(4, parents, simpleSent);
        zhaoLink = new FeatureObject(0, 4, zhaoPred, zhaoArg, parents);

        // Path between two indices.
        expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(0, Dir.UP));
        expectedPath.add(new Pair<Integer, Dir>(1, Dir.DOWN));
        expectedPath.add(new Pair<Integer, Dir>(5, Dir.DOWN));
        seenPath = zhaoLink.getDependencyPath();
        assertEquals(expectedPath,seenPath);        

        // Shared path to root for two indices.
        dpPathShare = zhaoLink.getDpPathShare();
        expectedDpPathShare = new ArrayList<Pair<Integer, Dir>>();
        expectedDpPathShare.add(new Pair<Integer, Dir>(1, Dir.UP));
        assertEquals(dpPathShare,expectedDpPathShare);

        // Line path (consecutive indices between two).
        ArrayList<Integer> linePath = zhaoLink.getLinePath();
        ArrayList<Integer> expectedLinePath = new ArrayList<Integer>();
        expectedLinePath.add(0);
        expectedLinePath.add(1);
        expectedLinePath.add(2);
        expectedLinePath.add(3);
        assertEquals(linePath,expectedLinePath);
    }
    
    @Test
    public void testZhaoObjectPathSentence2PredictedSyntax() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = false;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
        int[] parents = fe.getParents(simpleSent);
        FeatureObject zhaoPred = new FeatureObject(3, parents, simpleSent);
        FeatureObject zhaoArg = new FeatureObject(4, parents, simpleSent);
        FeatureObject zhaoLink = new FeatureObject(3, 4, zhaoPred, zhaoArg, parents);

        ArrayList<Pair<Integer, Dir>> expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(3, Dir.UP));
        List<Pair<Integer, Dir>> seenPath = zhaoLink.getDependencyPath();
        assertEquals(expectedPath,seenPath);

        zhaoPred = new FeatureObject(0, parents, simpleSent);
        zhaoArg = new FeatureObject(4, parents, simpleSent);
        zhaoLink = new FeatureObject(0, 4, zhaoPred, zhaoArg, parents);

        expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(0, Dir.DOWN));
        expectedPath.add(new Pair<Integer, Dir>(6, Dir.DOWN));
        expectedPath.add(new Pair<Integer, Dir>(5, Dir.DOWN));
        seenPath = zhaoLink.getDependencyPath();
        assertEquals(expectedPath,seenPath);        
    }

    
    @Test
    public void testZhaoObjectParentsChildrenSentence2() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
        //int[] parents = new int[]{1, -1, 5, 5, 5, 1, 1}; 
        int[] parents = fe.getParents(simpleSent);
        FeatureObject zhaoObj = new FeatureObject(3, parents, simpleSent);
        assertEquals(zhaoObj.getParent(), 5);
        assertEquals(zhaoObj.getChildren(), new ArrayList<Integer>());
        assertEquals(zhaoObj.getFarLeftChild(), -2);
        assertEquals(zhaoObj.getFarLeftChild(), -2);
        assertEquals(zhaoObj.getFarRightChild(), -2);
        assertEquals(zhaoObj.getNearLeftChild(), -2);
        assertEquals(zhaoObj.getNearRightChild(), -2);
        assertEquals(zhaoObj.getArgHighSupport(), -1);
        assertEquals(zhaoObj.getArgLowSupport(), -1);
        assertEquals(zhaoObj.getPredHighSupport(), 1);
        assertEquals(zhaoObj.getPredLowSupport(), 5);
        ArrayList<Integer> expectedNoFarChildren = new ArrayList<Integer>();
        expectedNoFarChildren.add(-2);
        expectedNoFarChildren.add(-2);
        assertEquals(zhaoObj.getNoFarChildren(), expectedNoFarChildren);
    }
    
    @Test
    public void testAddNaradowskyFeatures() {
        CoNLL09Sentence sent = getSpanishConll09Sentence1();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);

        ArrayList<String> allFeats = new ArrayList<String>();
        for (int i = 0; i < sent.size(); i++) {
            fe.addNaradowskySoloFeatures(i, allFeats);
            for (int j = 0; j < sent.size(); j++) {
                fe.addNaradowskyPairFeatures(i, j, allFeats);
            }
        }
        for (String f : allFeats) {
            System.out.println(f);
        }
    }
    
    @Test
    public void testAddBjorkelundPairFeatures() {
        CoNLL09Sentence sent = getSpanishConll09Sentence1();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);

        ArrayList<String> allFeats = new ArrayList<String>();
        fe.addBjorkelundPairFeatures(0, 1, allFeats);
        for (String f : allFeats) {
            System.out.println(f);
        }        
    }
    
    @Test
    public void testBjorkelundObjectSiblings() {
        CoNLL09Sentence sent = getSpanishConll09Sentence1();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = false;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
        int[] parents = fe.getParents(simpleSent);
        FeatureObject b = new FeatureObject(3, parents, simpleSent);
        assertEquals(b.getRightSibling(), 4);
        assertEquals(b.getLeftSibling(), 0);
        b = new FeatureObject(0, parents, simpleSent);
        assertEquals(b.getLeftSibling(), -1);
        assertEquals(b.getRightSibling(), 3);
        sent = getSpanishConll09Sentence2();
        simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        parents = fe.getParents(simpleSent);
        b = new FeatureObject(3, parents, simpleSent);
        // Only true when we're using predicted siblings.
        assertEquals(b.getLeftSibling(), -1);
        assertEquals(b.getRightSibling(), 7);
        csPrm.useGoldSyntax = true;
        simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        parents = fe.getParents(simpleSent);
        b = new FeatureObject(3, parents, simpleSent);
        assertEquals(b.getLeftSibling(), 2);
        assertEquals(b.getRightSibling(), 4);
    }
    
    
    @Test
    public void testTemplates() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        ArrayList<String> allFeats = new ArrayList<String>();
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.withSupervision = false;

        fePrm.formFeats = true;
        fePrm.lemmaFeats = true;
        fePrm.tagFeats = true;
        fePrm.morphFeats = true;
        fePrm.deprelFeats = true;
        fePrm.childrenFeats = true;
        fePrm.pathFeats = true;
        fePrm.syntacticConnectionFeats = true;

        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
        //allFeats = new ArrayList<String>();
        // using "es" and "hicieron"...
        fe.addTemplatePairFeatures(1, 5, allFeats);
        /*for (int i = 0; i < sent.size(); i++) {
            for (int j = 0; j < sent.size(); j++) {
                ArrayList<String> pairFeatures = fe.addTemplatePairFeatures(i, j);
                allFeats.addAll(pairFeatures);
            }
        }*/
        for (String f : allFeats) {
            System.out.println(f);
        }
    }
    
    
    public static CoNLL09Sentence getSpanishConll09Sentence1() {
        List<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();  
        //tokens.add(new CoNLL09Token(id, form, lemma, plemma, pos, ppos, feat, pfeat, head, phead, deprel, pdeprel, fillpred, pred, apreds));
        tokens.add(new CoNLL09Token("1       _       _       _       p       p       _       _       2       2       suj     suj     _       _       arg1-tem        _"));
        tokens.add(new CoNLL09Token("2       Resultaban      resultar        resultar        v       v       postype=main|gen=c|num=p|person=3|mood=indicative|tense=imperfect       postype=main|gen=c|num=p|person=3|mood=indicative|tense=imperfect       0       0       sentence        sentence        Y       resultar.c2     _       _"));
        tokens.add(new CoNLL09Token("3       demasiado       demasiado       demasiado       r       r       _       _       4       4       spec    spec    _       _       _       _"));
        tokens.add(new CoNLL09Token("4       baratos barato  barato  a       a       postype=qualificative|gen=m|num=p       postype=qualificative|gen=m|num=p       2       2       cpred   cpred   _       _       arg2-atr        _"));
        tokens.add(new CoNLL09Token("5       para    para    para    s       s       postype=preposition|gen=c|num=c postype=preposition|gen=c|num=c 2       2       cc      cc      _       _       argM-fin        _"));
        tokens.add(new CoNLL09Token("6       ser     ser     ser     v       v       postype=semiauxiliary|gen=c|num=c|mood=infinitive       postype=semiauxiliary|gen=c|num=c|mood=infinitive       5       5       S       S       Y       ser.c2  _       _"));
        tokens.add(new CoNLL09Token("7       buenos  buen    bueno   a       a       postype=qualificative|gen=m|num=p       postype=qualificative|gen=m|num=p       6       6       atr     atr     _       _       _       arg2-atr"));
        tokens.add(new CoNLL09Token("8       .       .       .       f       f       punct=period    punct=period    2       2       f       f       _       _       _       _"));
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
        
        return sent;
    }
    
    public static CoNLL09Sentence getSpanishConll09Sentence2() {
        List<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();      
        tokens.add(new CoNLL09Token("1       Eso     Eso     ESO     n       n       postype=proper|gen=c|num=c      postype=proper|gen=c|num=c      2       0       suj     suj     _       _       arg1-tem        _"));
        tokens.add(new CoNLL09Token("2       es      ser     ser     v       v       postype=semiauxiliary|gen=c|num=s|person=3|mood=indicative|tense=present        postype=semiauxiliary|gen=c|num=s|person=3|mood=indicative|tense=present        0       3       sentence        sentence        Y       ser.c2  _       _"));
        tokens.add(new CoNLL09Token("3       lo      el      el      d       d       postype=article|gen=c|num=s     postype=article|gen=c|num=s     6       4       spec    spec    _       _       _       _"));
        tokens.add(new CoNLL09Token("4       que     que     que     p       p       postype=relative|gen=c|num=c    postype=relative|gen=c|num=c    6       5       cd      cd      _       _       _       arg1-pat"));
        tokens.add(new CoNLL09Token("5       _       _       _       p       WRONG       _       _       6       6       suj     suj     _       _       _       arg0-agt"));
        tokens.add(new CoNLL09Token("6       hicieron        hacer   hacer   v       v       postype=main|gen=c|num=p|person=3|mood=indicative|tense=past    postype=main|gen=c|num=p|person=3|mood=indicative|tense=past    2       7       atr     atr     Y       hacer.a2        arg2-atr        _"));
        tokens.add(new CoNLL09Token("7       .       .       .       f       f       punct=period    punct=period    2       1       f       f       _       _       _       _"));
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
            
        return sent;
    }
    
    public static CoNLL09Sentence getDogConll09Sentence() {
        List<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();
        //tokens.add(new CoNLL09Token(id, form, lemma, plemma, pos, ppos, feat, pfeat, head, phead, deprel, pdeprel, fillpred, pred, apreds));
        tokens.add(new CoNLL09Token(1, "the", "_", "GoldDet", "Det", "_", Lists.getList("feat"), Lists.getList("feat") , 2, 3, "det", "_", false, "_", Lists.getList("_")));
        tokens.add(new CoNLL09Token(2, "dog", "_", "GoldN", "N", "_", Lists.getList("feat"), Lists.getList("feat") , 3, 1, "subj", "_", false, "_", Lists.getList("arg0")));
        tokens.add(new CoNLL09Token(3, "ate", "_", "GoldV", "V", "_", Lists.getList("feat"), Lists.getList("feat") , 0, 0, "v", "_", true, "ate.1", Lists.getList("_")));
        tokens.add(new CoNLL09Token(4, "food", "_", "GoldN", "N", "_", Lists.getList("feat"), Lists.getList("feat") , 3, 3, "obj", "_", false, "_", Lists.getList("arg1")));
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
        
        return sent;
    }
    
}
