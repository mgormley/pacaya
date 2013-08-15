package edu.jhu.srl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;

/**
 * Unit tests for {@link CorpusStatisticsTest}.
 * @author mgormley
 */
public class CorpusStatisticsTest {

    String expectedCsToString = "CorpusStatistics [\n"
            + "     knownWords=[de],\n"
            + "     knownUnks=[UNK-LC-s, UNK, UNK-LC],\n"
            + "     knownPostags=[f, v, d, s, r, p, a, n, z],\n"
            + "     linkStateNames=[True, False],\n"
            + "     roleStateNames=[arg2, arg1, arg0, _, argm],\n"
            + "     knownRoles=[arg2, arg1, arg0, _, argm],\n"
            + "     knownLinks=[True, False],\n"
            + "     maxSentLength=30,\n"
            + "     words={han=1, primers=1, dies=1, algunes=1, no=1, temps=1, suposa=1, any=2, Fundació_Privada_Fira_de_Manresa=1, obert=1, activitat=1, d'=2, fet=1, Palau_Firal=1, format=1, .=3, ,=3, els=1, ha=2, una=1, 40_per_cent=1, quals=1, recinte=1, acollit=1, a_banda_dels=1, Això=1, diverses=1, Palau=1, 13=1, activitats=1, de=5, utilització=1, En=1, gran=1, mesos=1, del=2, cinc=1, les=1, durant=1, s'=1, públic=1, un=1, firals=1, aquest=3, al=1, l'=2, el=1, certàmens=1, balanç=1},\n"
            + "     unks={UNK-INITC=2, UNK-LC-NUM=1, UNK-LC-s=13, UNK-CAPS=3, UNK=6, UNK-NUM=1, UNK-LC-VERB=1, UNK-LC=37}]";
    
    @Test
    // TODO: This is a hacky way to test for correctness, but fine for a quick check.
    public void testCreation() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(2);
        CorpusStatistics.normalizeRoleNames(sents);
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(sents);

        System.out.println(expectedCsToString);
        System.out.println();
        System.out.println(cs);
        
        assertEquals(expectedCsToString, cs.toString());
    }

}
