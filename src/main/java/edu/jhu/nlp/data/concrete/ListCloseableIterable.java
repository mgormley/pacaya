package edu.jhu.nlp.data.concrete;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.CloseableIterable;


public class ListCloseableIterable implements CloseableIterable<AnnoSentence> {
    
    private List<AnnoSentence> sents;

    public ListCloseableIterable(List<AnnoSentence> sents) {
        this.sents = sents;
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
