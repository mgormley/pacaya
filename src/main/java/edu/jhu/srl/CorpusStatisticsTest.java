package edu.jhu.srl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.concrete.SimpleAnnoSentence;
import edu.jhu.data.concrete.SimpleAnnoSentenceCollection;
import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;

/**
 * Unit tests for {@link CorpusStatisticsTest}.
 * @author mgormley
 * @author mmitchell
 */
public class CorpusStatisticsTest {
    
    String expectedCsToString = "CorpusStatistics [\n"
            + "     knownWords=[de],\n"
            + "     knownUnks=[UNK-LC-s, UNK, UNK-LC],\n"
            + "     knownPostags=[f, v, d, s, r, p, a, n, z],\n"
            + "     linkStateNames=[True, False],\n"
            + "     roleStateNames=[argUNK, arg2, arg1, arg0, _, argm],\n"
            + "     maxSentLength=30]";
    
    @Test
    // TODO: This is a hacky way to test for correctness, but fine for a quick check.
    public void testCreation() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        List<CoNLL09Sentence> sents = cr.readSents(2);
        SimpleAnnoSentenceCollection simpleSents = new SimpleAnnoSentenceCollection();
        for (CoNLL09Sentence sent : sents) {
            sent.normalizeRoleNames();
            SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm.useGoldSyntax);
            simpleSents.add(simpleSent);
        }
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(simpleSents);

        System.out.println(expectedCsToString);
        System.out.println();
        System.out.println(cs);
        
        assertEquals(expectedCsToString, cs.toString());
    }

}
