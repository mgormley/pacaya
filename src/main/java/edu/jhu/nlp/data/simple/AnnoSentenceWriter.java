package edu.jhu.nlp.data.simple;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.data.concrete.ConcreteWriter;
import edu.jhu.nlp.data.concrete.ConcreteWriter.ConcreteWriterPrm;
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

public class AnnoSentenceWriter {

    public static class AnnoSentenceWriterPrm {
        public String name = "";
        public boolean concreteSrlIsSyntax = false;
    }

    private static final Logger log = LoggerFactory.getLogger(AnnoSentenceWriter.class);

    private AnnoSentenceWriterPrm prm;
    
    public AnnoSentenceWriter(AnnoSentenceWriterPrm prm) {
        this.prm = prm;
    }
    
    public void write(File out, DatasetType type, AnnoSentenceCollection sents, Collection<AT> addAnnoTypes) throws IOException {
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
            try {
                int i = 0;
                for (AnnoSentence sent : sents) {
                    // Write one SemEval-2010 sentence for each pair of entities.
                    if (!sent.hasAt(AT.NE_PAIRS)) {
                        throw new RuntimeException("Sentence missing required annotation: " + AT.NE_PAIRS);
                    }
                    if (sent.getNePairs().size() > 0) {
                        if (!sent.hasAt(AT.REL_LABELS)) {
                            //throw new RuntimeException("Sentence missing required annotation: " + AT.REL_LABELS);
                            log.warn("Sentence missing required annotation: " + AT.REL_LABELS);
                        } else {
                            List<SemEval2010Sentence> seSents = SemEval2010Sentence.fromAnnoSentence(sent, i++);
                            for (SemEval2010Sentence seSent : seSents) {                        
                                sw.write(seSent);
                            }
                        }
                    }                
                }
            } finally {
                sw.close();
            }
        } else if (type == DatasetType.DEP_EDGE_MASK) {
            DepEdgeMaskWriter cw = new DepEdgeMaskWriter(out);
            for (AnnoSentence sent : sents) {
                cw.write(sent);
            }
            cw.close();
        } else if (type == DatasetType.CONCRETE) {
            ConcreteWriterPrm cwPrm = new ConcreteWriterPrm();
            cwPrm.srlIsSyntax = prm.concreteSrlIsSyntax;
            cwPrm.addAnnoTypes(addAnnoTypes);
            ConcreteWriter w = new ConcreteWriter(cwPrm);
            w.write(sents, out);
        } else {
            throw new IllegalStateException("Unsupported data type: " + type);
        }
    }
    
}
