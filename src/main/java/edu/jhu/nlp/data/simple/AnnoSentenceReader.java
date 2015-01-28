package edu.jhu.nlp.data.simple;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.data.concrete.ConcreteReader;
import edu.jhu.nlp.data.concrete.ConcreteReader.ConcreteReaderPrm;
import edu.jhu.nlp.data.concrete.ListCloseableIterable;
import edu.jhu.nlp.data.conll.CoNLL08FileReader;
import edu.jhu.nlp.data.conll.CoNLL08Sentence;
import edu.jhu.nlp.data.conll.CoNLL09FileReader;
import edu.jhu.nlp.data.conll.CoNLL09Sentence;
import edu.jhu.nlp.data.conll.CoNLLXFileReader;
import edu.jhu.nlp.data.conll.CoNLLXSentence;
import edu.jhu.nlp.data.semeval.SemEval2010Reader;
import edu.jhu.nlp.data.semeval.SemEval2010Sentence;

/**
 * Generic reader of AnnoSentence objects from many different corpora. 
 * 
 */
public class AnnoSentenceReader {

    public static class AnnoSentenceReaderPrm {
        public boolean useGoldSyntax = false;
        public int maxNumSentences = Integer.MAX_VALUE; 
        public int maxSentenceLength = Integer.MAX_VALUE; 
        public int minSentenceLength = 0;
        public SentFilter filter = null;
        public String name = "";
        
        // Parameters specific to data set type.
        /** CoNLL-2009 / CoNLL-2008: Whether to normalize role names in SRL data. */
        public boolean normalizeRoleNames = false;
        /** CoNLL-2008: Whether to use split word forms. */
        public boolean useSplitForms = true;
        /** CoNLL-X: whether to use the P(rojective)HEAD column for parents. */
        public boolean useCoNLLXPhead = false;
        /** Concrete options. */
        public ConcreteReaderPrm rePrm = new ConcreteReaderPrm();        
    }
    
    public enum DatasetType { SYNTHETIC, PTB, CONLL_X, CONLL_2008, CONLL_2009, CONCRETE, SEMEVAL_2010 };
    
    public interface SASReader extends Iterable<AnnoSentence> {
        public void close();        
    }
    
    private static final Logger log = LoggerFactory.getLogger(AnnoSentenceReader.class);

    private AnnoSentenceReaderPrm prm;
    private AnnoSentenceCollection sents;
    
    public AnnoSentenceReader(AnnoSentenceReaderPrm prm) {
        this.prm = prm;
        this.sents = new AnnoSentenceCollection();
    }
    
    public AnnoSentenceCollection getData() {
        return sents;
    }
    
    public void loadSents(File dataFile, DatasetType type) throws IOException {
        log.info("Reading " + prm.name + " data of type " + type + " from " + dataFile);

        if (prm.normalizeRoleNames) {
            if (type == DatasetType.CONLL_2008 || type == DatasetType.CONLL_2009) {
                log.info("Normalizing role names");
            }
        }
        
        CloseableIterable<AnnoSentence> reader = null;
        Object sourceSents = null;
        if (type == DatasetType.CONCRETE) {
            ConcreteReader cr = new ConcreteReader(prm.rePrm);
            AnnoSentenceCollection csents = cr.toSentences(dataFile);
            sourceSents = csents.getSourceSents();
            reader = new ListCloseableIterable(csents);
        } else {
            InputStream fis = new FileInputStream(dataFile);
            if (type == DatasetType.CONLL_2009) {
                reader = ConvCloseableIterable.getInstance(new CoNLL09FileReader(fis), new CoNLL092Anno());
            } else if (type == DatasetType.CONLL_2008) {
                reader = ConvCloseableIterable.getInstance(new CoNLL08FileReader(fis), new CoNLL082Anno());
            } else if (type == DatasetType.CONLL_X) {
                reader = ConvCloseableIterable.getInstance(new CoNLLXFileReader(fis), new CoNLLX2Anno());
            } else if (type == DatasetType.SEMEVAL_2010) {
                reader = ConvCloseableIterable.getInstance(new SemEval2010Reader(fis), new SemEval20102Anno());
            //} else if (type == DatasetType.PTB) {
                //reader = new Ptb2Anno(new PtbFileReader(dataFile));
            } else {
                fis.close();
                throw new IllegalStateException("Unsupported data type: " + type);
            }
        }
        
        loadSents(reader);
        sents.setSourceSents(sourceSents);
        
        log.info("Num " + prm.name + " sentences: " + sents.size());   
        log.info("Num " + prm.name + " tokens: " + sents.getNumTokens());
        log.info("Longest sentence: " + sents.getMaxLength());
        log.info("Average sentence length: " + sents.getAvgLength());
        reader.close();
    }
    
    public void loadSents(Iterable<AnnoSentence> reader) {
        for (AnnoSentence sent : reader) {
            if (sents.size() >= prm.maxNumSentences) {
                break;
            }
            if (sent.size() <= prm.maxSentenceLength && prm.minSentenceLength <= sent.size()) {
                if (prm.filter == null || prm.filter.accept(sent)) {
                    sent.intern();
                    sents.add(sent);
                }
            }
        }
    }
    
    public class CoNLL092Anno implements Converter<CoNLL09Sentence, AnnoSentence> {

        @Override
        public AnnoSentence convert(CoNLL09Sentence x) {
            if (prm.normalizeRoleNames) {
                x.normalizeRoleNames();
            }
            return x.toAnnoSentence(prm.useGoldSyntax);
        }        
        
    }
    
    public class CoNLL082Anno implements Converter<CoNLL08Sentence, AnnoSentence> {

        @Override
        public AnnoSentence convert(CoNLL08Sentence x) {
            if (prm.normalizeRoleNames) {
                x.normalizeRoleNames();
            }
            return x.toAnnoSentence(prm.useGoldSyntax, prm.useSplitForms );
        }
        
    }
    
    public class CoNLLX2Anno implements Converter<CoNLLXSentence, AnnoSentence> {

        @Override
        public AnnoSentence convert(CoNLLXSentence x) {
            return x.toAnnoSentence(prm.useCoNLLXPhead);
        }
        
    }
    
    public class SemEval20102Anno implements Converter<SemEval2010Sentence, AnnoSentence> {

        @Override
        public AnnoSentence convert(SemEval2010Sentence x) {
            return x.toAnnoSentence();
        }
        
    }
    
}
