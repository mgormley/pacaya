package edu.jhu.featurize;

import static edu.jhu.util.Utilities.getList;
import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.BinaryStrFVBuilder;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Utilities;

public class SentFeatureExtractorTest {

    @Test
    public void testGetParentsAndUseGoldSyntax() {
        CoNLL09Sentence sent = getDogConll09Sentence();
        Alphabet<String> alphabet = new Alphabet<String>();
        {
            // Test with gold syntax.
            SentFeatureExtractorPrm prm = new SentFeatureExtractorPrm();
            prm.useGoldSyntax = true;
            CorpusStatistics cs = new CorpusStatistics(prm);
            cs.init(Utilities.getList(sent));
            SentFeatureExtractor fe = new SentFeatureExtractor(prm, sent, cs, alphabet);
            int[] goldParents = fe.getParents(sent);
            assertArrayEquals(new int[] { 1, 2, -1, 2 }, goldParents);
        }
        {
            // Test without gold syntax.
            SentFeatureExtractorPrm prm = new SentFeatureExtractorPrm();
            prm.useGoldSyntax = false;
            CorpusStatistics cs = new CorpusStatistics(prm);
            cs.init(Utilities.getList(sent));
            SentFeatureExtractor fe = new SentFeatureExtractor(prm, sent, cs, alphabet);
            int[] predParents = fe.getParents(sent);
            assertArrayEquals(new int[] { 2, 0, -1, 2 }, predParents);
        }
    }
    
    @Test
    public void testAddZhaoFeatures() {
        CoNLL09Sentence sent = getSpanishConll09Sentence();
        Alphabet<String> alphabet = new Alphabet<String>();
        SentFeatureExtractorPrm prm = new SentFeatureExtractorPrm();
        prm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(prm);
        cs.init(Utilities.getList(sent));
        SentFeatureExtractor fe = new SentFeatureExtractor(prm, sent, cs, alphabet);
        fe.createFeatureSet(0, 1);
        
        //Check that POS is not gold POS
    }
    
    public static CoNLL09Sentence getSpanishConll09Sentence() {
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
