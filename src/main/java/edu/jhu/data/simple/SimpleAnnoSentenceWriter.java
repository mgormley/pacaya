package edu.jhu.data.simple;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import edu.jhu.data.conll.CoNLL08Sentence;
import edu.jhu.data.conll.CoNLL08Writer;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Writer;
import edu.jhu.data.conll.CoNLLXSentence;
import edu.jhu.data.conll.CoNLLXWriter;
import edu.jhu.data.simple.SimpleAnnoSentenceReader.DatasetType;

public class SimpleAnnoSentenceWriter {

    public static class SimpleAnnoSentenceWriterPrm {
        public String name = "";
    }

    private static final Logger log = Logger.getLogger(SimpleAnnoSentenceWriter.class);

    private SimpleAnnoSentenceWriterPrm prm;
    
    public SimpleAnnoSentenceWriter(SimpleAnnoSentenceWriterPrm prm) {
        this.prm = prm;
    }
    
    public void write(File out, DatasetType type, SimpleAnnoSentenceCollection sents) throws IOException {
        log.info("Writing sentences for " + prm.name + " data of type " + type + " to " + out);
        if (type == DatasetType.CONLL_2009) {
            CoNLL09Writer cw = new CoNLL09Writer(out);
            for (SimpleAnnoSentence sent : sents) {
                CoNLL09Sentence conllSent = CoNLL09Sentence.fromSimpleAnnoSentence(sent);
                cw.write(conllSent);
            }
            cw.close();
        } else if (type == DatasetType.CONLL_2008) {
            CoNLL08Writer cw = new CoNLL08Writer(out);
            for (SimpleAnnoSentence sent : sents) {
                CoNLL08Sentence conllSent = CoNLL08Sentence.fromSimpleAnnoSentence(sent);
                cw.write(conllSent);
            }
            cw.close();
        } else if (type == DatasetType.CONLL_X) {
            CoNLLXWriter cw = new CoNLLXWriter(out);
            for (SimpleAnnoSentence sent : sents) {
                CoNLLXSentence conllSent = CoNLLXSentence.fromSimpleAnnoSentence(sent);
                cw.write(conllSent);
            }
            cw.close();
        } else {
            throw new IllegalStateException("Unsupported data type: " + type);
        }
    }
    
}
