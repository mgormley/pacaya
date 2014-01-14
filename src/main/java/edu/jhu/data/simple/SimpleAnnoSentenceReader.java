package edu.jhu.data.simple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Writer;
import edu.jhu.data.conll.CoNLLXDepTree;
import edu.jhu.data.conll.CoNLLXDirReader;
import edu.jhu.data.conll.CoNLLXSentence;
import edu.jhu.data.deptree.DepTreebankReader;
import edu.jhu.srl.SrlRunner.DatasetType;
import edu.jhu.tag.BrownClusterTagger;
import edu.jhu.tag.BrownClusterTagger.BrownClusterTaggerPrm;

/** 
 * TODO: This class is only partly implemented.
 */
public class SimpleAnnoSentenceReader {

    public static class SimpleAnnoSentenceReaderPrm {
        public BrownClusterTaggerPrm bcPrm = new BrownClusterTaggerPrm();
        public boolean useGoldSyntax = false;
        //public DatasetType dataType = null; 
        public int maxNumSentences = Integer.MAX_VALUE; 
        public int maxSentenceLength = Integer.MAX_VALUE; 
        public SentFilter filter = null;
        public File brownClusters = null;
        /** Whether to normalize role names in SRL data (e.g. CoNLL-2009). */
        public boolean normalizeRoleNames = false;
        
        //TODO: Maybe remove parameters below this line.
        public File goldFile;  
        public String name;
    }
    
    public enum DatasetType { SYNTHETIC, PTB, CONLL_X, CONLL_2009 };
    
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
        SASReader reader = null;
        if (type == DatasetType.CONLL_2009) {
            //reader = new CoNLL092SimpleAnno(new CoNLL09FileReader(dataFile));
        } else if (type == DatasetType.CONLL_X) {
            //reader = new CoNLLX2SimpleAnno(new CoNLLXFileReader(dataFile));
        } else if (type == DatasetType.PTB) {
            //reader = new Ptb2SimpleAnno(new PtbFileReader(dataFile));
        } else {
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
                    sents.add(sent);
                }
            }
        }
    }
    
    // TODO: Remove this method.
    @Deprecated
    private SimpleAnnoSentenceCollection readSentences(boolean useGoldSyntax, DatasetType dataType, File dataFile,
            File goldFile, int maxNumSentences, int maxSentenceLength, String name) throws IOException, ParseException {
        log.info("Reading " + name + " data of type " + dataType + " from " + dataFile);
        SimpleAnnoSentenceCollection sents;
        int numTokens = 0;
        
        // Read the data and (optionally) write it to the gold file.
        if (dataType == DatasetType.CONLL_2009) {
            List<CoNLL09Sentence> conllSents = new ArrayList<CoNLL09Sentence>();
            CoNLL09FileReader reader = new CoNLL09FileReader(dataFile);
            for (CoNLL09Sentence sent : reader) {
                if (conllSents.size() >= maxNumSentences) {
                    break;
                }
                if (sent.size() <= maxSentenceLength) {
                    sent.intern();
                    conllSents.add(sent);
                    numTokens += sent.size();
                }
            }
            reader.close();     

            if (prm.normalizeRoleNames) {
                log.info("Normalizing role names");
                for (CoNLL09Sentence conllSent : conllSents) {
                    conllSent.normalizeRoleNames();
                }
            }
            
            if (prm.goldFile != null) {
                log.info("Writing gold data to file: " + goldFile);
                CoNLL09Writer cw = new CoNLL09Writer(goldFile);
                for (CoNLL09Sentence sent : conllSents) {
                    cw.write(sent);
                }
                cw.close();
            }
                        
            // TODO: We should clearly differentiate between the gold sentences and the input sentence.
            // Convert CoNLL sentences to SimpleAnnoSentences.
            sents = new SimpleAnnoSentenceCollection();
            for (CoNLL09Sentence conllSent : conllSents) {
                sents.add(conllSent.toSimpleAnnoSentence(useGoldSyntax));
            }
        } else {
            throw new ParseException("Unsupported data type: " + dataType);
        }
        
        log.info("Num " + name + " sentences: " + sents.size());   
        log.info("Num " + name + " tokens: " + numTokens);

        if (prm.brownClusters != null) {
            log.info("Adding Brown clusters.");
            BrownClusterTagger bct = new BrownClusterTagger(prm.bcPrm);
            bct.read(prm.brownClusters);
            bct.addClusters(sents);
            log.info("Brown cluster hit rate: " + bct.getHitRate());
        } else {
            log.warn("No Brown cluster file specified.");            
        }
        return sents;
    }
    
}
