package edu.jhu.data.simple;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

import edu.jhu.data.conll.CoNLL08FileReader;
import edu.jhu.data.conll.CoNLL08Sentence;
import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLLXFileReader;
import edu.jhu.data.conll.CoNLLXSentence;
import edu.jhu.tag.BrownClusterTagger;
import edu.jhu.tag.BrownClusterTagger.BrownClusterTaggerPrm;

/**
 * Generic reader of SimpleAnnoSentence objects from many different corpora. 
 * 
 */
public class SimpleAnnoSentenceReader {

    public static class SimpleAnnoSentenceReaderPrm {
        public BrownClusterTaggerPrm bcPrm = new BrownClusterTaggerPrm();
        public boolean useGoldSyntax = false;
        public int maxNumSentences = Integer.MAX_VALUE; 
        public int maxSentenceLength = Integer.MAX_VALUE; 
        public SentFilter filter = null;
        public File brownClusters = null;        
        public String name = "";
        
        // Parameters specific to data set type.
        /** CoNLL-2009 / CoNLL-2008: Whether to normalize role names in SRL data. */
        public boolean normalizeRoleNames = false;
        /** CoNLL-2008: Whether to use split word forms. */
        public boolean useSplitForms = true;
        /** CoNLL-X: whether to use the P(rojective)HEAD column for parents. */
        public boolean useCoNLLXPhead = false;
    }
    
    public enum DatasetType { SYNTHETIC, PTB, CONLL_X, CONLL_2008, CONLL_2009 };
    
    public interface SASReader extends Iterable<SimpleAnnoSentence> {
        public void close();        
    }
    
    private static final Logger log = Logger.getLogger(SimpleAnnoSentenceReader.class);

    private SimpleAnnoSentenceReaderPrm prm;
    private SimpleAnnoSentenceCollection sents;
    
    public SimpleAnnoSentenceReader(SimpleAnnoSentenceReaderPrm prm) {
        this.prm = prm;
        this.sents = new SimpleAnnoSentenceCollection();
    }
    
    public SimpleAnnoSentenceCollection getData() {
        return sents;
    }
    
    public void loadSents(File dataFile, DatasetType type) throws IOException {
        log.info("Reading " + prm.name + " data of type " + type + " from " + dataFile);
        loadSents(new FileInputStream(dataFile), type);
    }

    public void loadSents(InputStream fis, DatasetType type) throws UnsupportedEncodingException, IOException {
        if (prm.normalizeRoleNames) {
            if (type == DatasetType.CONLL_2008 || type == DatasetType.CONLL_2009) {
                log.info("Normalizing role names");
            }
        }
        
        CloseableIterable<SimpleAnnoSentence> reader = null;
        if (type == DatasetType.CONLL_2009) {
            reader = ConvCloseableIterable.getInstance(new CoNLL09FileReader(fis), new CoNLL092SimpleAnno());
        } else if (type == DatasetType.CONLL_2008) {
            reader = ConvCloseableIterable.getInstance(new CoNLL08FileReader(fis), new CoNLL082SimpleAnno());
        } else if (type == DatasetType.CONLL_X) {
            reader = ConvCloseableIterable.getInstance(new CoNLLXFileReader(fis), new CoNLLX2SimpleAnno());
        //} else if (type == DatasetType.PTB) {
            //reader = new Ptb2SimpleAnno(new PtbFileReader(dataFile));
        } else {
            fis.close();
            throw new IllegalStateException("Unsupported data type: " + type);
        }
        
        loadSents(reader);
        
        log.info("Num " + prm.name + " sentences: " + sents.size());   
        log.info("Num " + prm.name + " tokens: " + sents.getNumTokens());

        if (prm.brownClusters != null) {            
            log.info("Adding Brown clusters.");
            BrownClusterTagger bct = new BrownClusterTagger(prm.bcPrm);
            bct.read(prm.brownClusters);
            bct.addClusters(sents);
            log.info("Brown cluster hit rate: " + bct.getHitRate());
        } else {
            log.warn("No Brown cluster file specified.");            
        }
        
        reader.close();
    }
    
    public void loadSents(Iterable<SimpleAnnoSentence> reader) {
        for (SimpleAnnoSentence sent : reader) {
            if (sents.size() >= prm.maxNumSentences) {
                break;
            }
            if (sent.size() <= prm.maxSentenceLength) {
                if (prm.filter == null || prm.filter.accept(sent)) {
                    sent.intern();
                    sents.add(sent);
                }
            }
        }
    }
    
    public class CoNLL092SimpleAnno implements Converter<CoNLL09Sentence, SimpleAnnoSentence> {

        @Override
        public SimpleAnnoSentence convert(CoNLL09Sentence x) {
            if (prm.normalizeRoleNames) {
                x.normalizeRoleNames();
            }
            return x.toSimpleAnnoSentence(prm.useGoldSyntax);
        }        
        
    }
    
    public class CoNLL082SimpleAnno implements Converter<CoNLL08Sentence, SimpleAnnoSentence> {

        @Override
        public SimpleAnnoSentence convert(CoNLL08Sentence x) {
            if (prm.normalizeRoleNames) {
                x.normalizeRoleNames();
            }
            return x.toSimpleAnnoSentence(prm.useGoldSyntax, prm.useSplitForms );
        }
        
    }
    
    public class CoNLLX2SimpleAnno implements Converter<CoNLLXSentence, SimpleAnnoSentence> {

        @Override
        public SimpleAnnoSentence convert(CoNLLXSentence x) {
            return x.toSimpleAnnoSentence(prm.useCoNLLXPhead);
        }
        
    }
    
}
