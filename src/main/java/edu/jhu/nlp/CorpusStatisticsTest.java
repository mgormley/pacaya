package edu.jhu.nlp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;

/**
 * Unit tests for {@link CorpusStatisticsTest}.
 * @author mgormley
 * @author mmitchell
 */
public class CorpusStatisticsTest {
    
    String expectedCsToString = "CorpusStatistics [\n"
            + "     knownWords=[de, ., ,],\n"
            + "     topNWords=[de],\n"
            + "     knownUnks=[UNK-LC-s, UNK-CAPS, UNK, UNK-LC],\n"
            + "     knownPostags=[f, v, d, s, c, r, p, a, n, z],\n"
            + "     linkStateNames=[True, False],\n"
            + "     roleStateNames=[argUNK, arg2, arg1, arg0, _, argm],\n"
            + "     maxSentLength=30]";
    
    @Test
    // TODO: This is a hacky way to test for correctness, but fine for a quick check.
    public void testCreation() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.topN = 1;
        List<CoNLL09Sentence> sents = cr.readSents(4);
        AnnoSentenceCollection simpleSents = new AnnoSentenceCollection();
        for (CoNLL09Sentence sent : sents) {
            sent.normalizeRoleNames();
            AnnoSentence simpleSent = sent.toAnnoSentence(csPrm.useGoldSyntax);
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
