package edu.jhu.featurize;

import static edu.jhu.util.Utilities.getList;
import static org.junit.Assert.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.BinaryStrFVBuilder;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Pair;
import edu.jhu.util.Utilities;

public class SentFeatureExtractorTest {

    @Test
    public void testGetParentsAndUseGoldSyntax() {
        CoNLL09Sentence sent = getDogConll09Sentence();
        Alphabet<String> alphabet = new Alphabet<String>();
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        {
            // Test with gold syntax.
            CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
            csPrm.useGoldSyntax = true;
            CorpusStatistics cs = new CorpusStatistics(csPrm);
            cs.init(Utilities.getList(sent));
            SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, sent, cs);
            int[] goldParents = fe.getParents(sent);
            assertArrayEquals(new int[] { 1, 2, -1, 2 }, goldParents);
        }
        {
            // Test without gold syntax.
            CorpusStatisticsPrm prm = new CorpusStatisticsPrm();
            prm.useGoldSyntax = false;
            CorpusStatistics cs = new CorpusStatistics(prm);
            cs.init(Utilities.getList(sent));
            SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, sent, cs);
            int[] predParents = fe.getParents(sent);
            assertArrayEquals(new int[] { 2, 0, -1, 2 }, predParents);
        }
    }
    
    @Test
    public void testAddZhaoFeatures() {
        CoNLL09Sentence sent = getSpanishConll09Sentence1();
        Alphabet<String> alphabet = new Alphabet<String>();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(Utilities.getList(sent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, sent, cs);
        BinaryStrFVBuilder feats;
        BinaryStrFVBuilder allFeats = new BinaryStrFVBuilder(alphabet);
        for (int i = 0; i < sent.size(); i++) {
            for (int j = 0; j < sent.size(); j++) {
                fe.addZhaoPairFeatures(i, j, allFeats);
            }
        }
        for (String f : allFeats) {
            System.out.println(f);
        }
        //Check that POS is not gold POS
    }
    
    @Test
    public void testZhaoPathFeatures() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        Alphabet<String> alphabet = new Alphabet<String>();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(Utilities.getList(sent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, sent, cs);
        int[] parents = fe.getParents(sent);
        ZhaoObject zhaoPred = new ZhaoObject(1, parents, sent, cs, "v");
        ZhaoObject zhaoArg = new ZhaoObject(0, parents, sent, cs, "n");
        ZhaoObject zhaoLink = new ZhaoObject(1, 0, zhaoPred, zhaoArg, parents);
        List<Pair<Integer, Dir>> desiredDpPathShare = new ArrayList<Pair<Integer, Dir>>();
        desiredDpPathShare.add(new Pair<Integer, Dir>(1,Dir.UP));
        List<Pair<Integer, Dir>> observedDpPathShare = zhaoLink.getDpPathShare();
        System.out.println(observedDpPathShare);
        assertEquals(desiredDpPathShare,observedDpPathShare);
    }
    
    @Test
    public void testZhaoObjectPos() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        Alphabet<String> alphabet = new Alphabet<String>();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(Utilities.getList(sent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, sent, cs);
        int[] parents = fe.getParents(sent);
        ZhaoObject zhaoPred = new ZhaoObject(3, parents, sent, cs, "v");
        ZhaoObject zhaoArg = new ZhaoObject(4, parents, sent, cs, "n");

        String predPos = zhaoPred.getPos();
        String argPos = zhaoArg.getPos();
        assertEquals(predPos,argPos,"p");
        
        csPrm.useGoldSyntax = false;
        cs = new CorpusStatistics(csPrm);
        cs.init(Utilities.getList(sent));
        fePrm = new SentFeatureExtractorPrm();
        fe = new SentFeatureExtractor(fePrm, sent, cs);
        parents = fe.getParents(sent);
        zhaoPred = new ZhaoObject(3, parents, sent, cs, "v");
        zhaoArg = new ZhaoObject(4, parents, sent, cs, "n");
        
        predPos = zhaoPred.getPos();
        argPos = zhaoArg.getPos();
        
        assertEquals(predPos,"p");
        assertEquals(argPos,"WRONG");
    }
    
    @Test
    public void testZhaoObjectFeat() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        Alphabet<String> alphabet = new Alphabet<String>();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(Utilities.getList(sent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, sent, cs);
        int[] parents = fe.getParents(sent);
        ZhaoObject zhaoPred = new ZhaoObject(3, parents, sent, cs, "v");
        ZhaoObject zhaoArg = new ZhaoObject(4, parents, sent, cs, "n");
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
    public void testZhaoObjectPath() {
        CoNLL09Sentence sent = getSpanishConll09Sentence2();
        Alphabet<String> alphabet = new Alphabet<String>();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(Utilities.getList(sent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, sent, cs);
        int[] parents = fe.getParents(sent);
        ZhaoObject zhaoPred = new ZhaoObject(3, parents, sent, cs, "v");
        ZhaoObject zhaoArg = new ZhaoObject(4, parents, sent, cs, "n");
        ZhaoObject zhaoLink = new ZhaoObject(3, 4, zhaoPred, zhaoArg, parents);

        ArrayList<Pair<Integer, Dir>> expectedPath = new ArrayList<Pair<Integer, Dir>>();
        expectedPath.add(new Pair<Integer, Dir>(3, Dir.UP));
        expectedPath.add(new Pair<Integer, Dir>(5, Dir.DOWN));
        List<Pair<Integer, Dir>> seenPath = zhaoLink.getBetweenPath();
        assertEquals(expectedPath,seenPath);
        List<Pair<Integer, Dir>> seenPredDpPath = zhaoArg.getDpPathPred();
        List<Pair<Integer, Dir>> seenArgDpPath = zhaoArg.getDpPathArg();
        assertTrue(seenPredDpPath == null);
        assertTrue(seenArgDpPath == null);
        //System.out.println(zhaoLink.getDpPathShare());
        
        /*
        getDpPathShare();
        getLinePath();*/
    
    }

        /* TBD:
        zhaoPred.getParent();
        zhaoPred.getChildren();
        zhaoPred.getFarLeftChild();
        zhaoPred.getFarRightChild();
        zhaoPred.getNearLeftChild();
        zhaoPred.getNearRightChild();
        zhaoPred.getHighSupport();
        zhaoPred.getLowSupport();*/
    
    @Test
    public void testAddNaradowskyFeatures() {
        CoNLL09Sentence sent = getSpanishConll09Sentence1();
        Alphabet<String> alphabet = new Alphabet<String>();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(Utilities.getList(sent));
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, sent, cs);

        
        BinaryStrFVBuilder allFeats = new BinaryStrFVBuilder(alphabet);
        for (int i = 0; i < sent.size(); i++) {
            for (int j = 0; j < sent.size(); j++) {
                fe.addNaradowskyPairFeatures(i, j, allFeats);
            }
        }
        for (String f : allFeats) {
            System.out.println(f);
        }
        //Check that POS is not gold POS
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
        tokens.add(new CoNLL09Token(1, "the", "_", "GoldDet", "Det", "_", getList("feat"), getList("feat") , 2, 3, "det", "_", false, "_", getList("_")));
        tokens.add(new CoNLL09Token(2, "dog", "_", "GoldN", "N", "_", getList("feat"), getList("feat") , 3, 1, "subj", "_", false, "_", getList("arg0")));
        tokens.add(new CoNLL09Token(3, "ate", "_", "GoldV", "V", "_", getList("feat"), getList("feat") , 0, 0, "v", "_", true, "ate.1", getList("_")));
        tokens.add(new CoNLL09Token(4, "food", "_", "GoldN", "N", "_", getList("feat"), getList("feat") , 3, 3, "obj", "_", false, "_", getList("arg1")));
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
        
        return sent;
    }
    
}
