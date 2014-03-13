package edu.jhu.data.simple;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import com.google.common.io.Files;

import edu.jhu.data.concrete.ConcreteWriter;
import edu.jhu.data.conll.CoNLL08Sentence;
import edu.jhu.data.conll.CoNLL08Writer;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Writer;
import edu.jhu.data.conll.CoNLLXSentence;
import edu.jhu.data.conll.CoNLLXWriter;
import edu.jhu.data.simple.SimpleAnnoSentenceReader.DatasetType;
import edu.jhu.hlt.concrete.Communication;

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
        } else if (type == DatasetType.CONCRETE) {
            Communication comm = (Communication) sents.getSourceSents();
            ConcreteWriter w = new ConcreteWriter(false);
            w.addDependencyParse(sents, comm);
            try {
                byte[] bytez = new TSerializer(new TBinaryProtocol.Factory()).serialize(comm);
                Files.write(bytez, out);
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalStateException("Unsupported data type: " + type);
        }
    }
    
}
