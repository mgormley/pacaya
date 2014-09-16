package edu.jhu.nlp.data.concrete;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import edu.jhu.nlp.data.concrete.ConcreteReader.ConcreteReaderPrm;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.CloseableIterable;


public class ConcreteIterable implements CloseableIterable<AnnoSentence> {
    
    private AnnoSentenceCollection sents;

    public ConcreteIterable(ConcreteReaderPrm prm, File dataFile) {
        ConcreteReader reader = new ConcreteReader(prm);
        try {
            sents = reader.toSentences(dataFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterator<AnnoSentence> iterator() {
        return sents.iterator();
    }
    
    @Override
    public void close() throws IOException {
        // No-op.
    }

}
