package edu.jhu.featurize;

import static edu.jhu.util.Utilities.getList;
import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
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
