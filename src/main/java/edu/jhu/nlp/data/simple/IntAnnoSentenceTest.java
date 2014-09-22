package edu.jhu.nlp.data.simple;

import static org.junit.Assert.fail;

import org.junit.Test;

import edu.jhu.nlp.data.simple.AnnoSentenceReader.DatasetType;
import edu.jhu.nlp.data.simple.IntAnnoSentence.AlphabetStore;

public class IntAnnoSentenceTest {

    @Test
    public void testOnRealCorpus() {
        AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.read(AnnoSentenceReaderSpeedTest.czTrain, DatasetType.CONLL_X);
        AlphabetStore store = new AlphabetStore(sents);
        for (AnnoSentence sent : sents) {
            new IntAnnoSentence(sent, store);
        }
    }

}
