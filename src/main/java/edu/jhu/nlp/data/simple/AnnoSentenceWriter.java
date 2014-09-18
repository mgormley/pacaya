package edu.jhu.nlp.data.simple;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.concrete.ConcreteWriter;
import edu.jhu.nlp.data.conll.CoNLL08Sentence;
import edu.jhu.nlp.data.conll.CoNLL08Writer;
import edu.jhu.nlp.data.conll.CoNLL09Sentence;
import edu.jhu.nlp.data.conll.CoNLL09Writer;
import edu.jhu.nlp.data.conll.CoNLLXSentence;
import edu.jhu.nlp.data.conll.CoNLLXWriter;
import edu.jhu.nlp.data.semeval.SemEval2010Sentence;
import edu.jhu.nlp.data.semeval.SemEval2010Writer;
import edu.jhu.nlp.data.simple.AnnoSentenceReader.DatasetType;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.prim.tuple.Pair;

public class AnnoSentenceWriter {

    public static class AnnoSentenceWriterPrm {
        public String name = "";
    }

    private static final Logger log = Logger.getLogger(AnnoSentenceWriter.class);

    private AnnoSentenceWriterPrm prm;
    
    public AnnoSentenceWriter(AnnoSentenceWriterPrm prm) {
        this.prm = prm;
    }
    
    public void write(File out, DatasetType type, AnnoSentenceCollection sents) throws IOException {
        log.info("Writing sentences for " + prm.name + " data of type " + type + " to " + out);
        if (type == DatasetType.CONLL_2009) {
            CoNLL09Writer cw = new CoNLL09Writer(out);
            for (AnnoSentence sent : sents) {
                CoNLL09Sentence conllSent = CoNLL09Sentence.fromAnnoSentence(sent);
                cw.write(conllSent);
            }
            cw.close();
        } else if (type == DatasetType.CONLL_2008) {
            CoNLL08Writer cw = new CoNLL08Writer(out);
            for (AnnoSentence sent : sents) {
                CoNLL08Sentence conllSent = CoNLL08Sentence.fromAnnoSentence(sent);
                cw.write(conllSent);
            }
            cw.close();
        } else if (type == DatasetType.CONLL_X) {
            CoNLLXWriter cw = new CoNLLXWriter(out);
            for (AnnoSentence sent : sents) {
                CoNLLXSentence conllSent = CoNLLXSentence.fromAnnoSentence(sent);
                cw.write(conllSent);
            }
            cw.close();
        } else if (type == DatasetType.SEMEVAL_2010) {
            SemEval2010Writer sw = new SemEval2010Writer(out);
            int i = 0;
            for (AnnoSentence sent : sents) {
                // Write one SemEval-2010 sentence for each pair of entities.
                if (!sent.hasAt(AT.NE_PAIRS)) {
                    throw new RuntimeException("Sentence missing required annotation: " + AT.NE_PAIRS);
                }
                for (Pair<NerMention, NerMention> pair : sent.getNePairs()) {
                    if (!sent.hasAt(AT.RELATIONS)) {
                        throw new RuntimeException("Sentence missing required annotation: " + AT.RELATIONS);
                    }
                    SemEval2010Sentence seSent = SemEval2010Sentence.fromAnnoSentence(sent, i++, pair.get1(), pair.get2());
                    sw.write(seSent);
                }
            }
            sw.close();
        } else if (type == DatasetType.CONCRETE) {
            Communication comm = (Communication) sents.getSourceSents();
            ConcreteWriter w = new ConcreteWriter(false);
            w.addDependencyParse(sents, comm);
            try {
                byte[] bytez = new TSerializer(new TBinaryProtocol.Factory()).serialize(comm);
                Files.write(Paths.get(out.getAbsolutePath()), bytez);
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalStateException("Unsupported data type: " + type);
        }
    }
    
}
