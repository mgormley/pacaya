package edu.jhu.featurize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.data.simple.AnnoSentenceCollection;
import edu.jhu.data.simple.AnnoSentenceReader;
import edu.jhu.data.simple.AnnoSentenceReader.DatasetType;
import edu.jhu.data.simple.AnnoSentenceReader.AnnoSentenceReaderPrm;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Timer;
import edu.jhu.util.hash.MurmurHash3;

public class FeatureExtractorSpeedTest {
    
    private static final Logger log = Logger.getLogger(FeatureExtractorSpeedTest.class);
    private static final int featureHashMod = 1000000;
    
    //@Test
    public void testSpeed() throws ParseException, IOException {
        List<FeatTemplate> tpls = TemplateSets.getFromResource(TemplateSets.mcdonaldDepFeatsResource);
        File train = new File("data/conllx/CoNLL-X/test/data/english/ptb/test/english_ptb_test.conll");

        AnnoSentenceReaderPrm prm = new AnnoSentenceReaderPrm();
        //prm.maxNumSentences = 100;
        prm.name = train.getName();
        AnnoSentenceReader reader = new AnnoSentenceReader(prm );
        reader.loadSents(train, DatasetType.CONLL_X);
        AnnoSentenceCollection sents = reader.getData();
        
        int trials = 3;
        
        Alphabet<Object> alphabet = new Alphabet<Object>();
        
        Timer timer = new Timer();
        timer.start();
        int n=0;
        for (int trial = 0; trial < trials; trial++) {
            for (AnnoSentence sent : sents) {
                TemplateFeatureExtractor ext = new TemplateFeatureExtractor(sent, null);
                for (int i = -1; i < sent.size(); i++) {
                    for (int j = 0; j < sent.size(); j++) {
                        LocalObservations local = LocalObservations.newPidxCidx(i, j);
                        ArrayList<String> feats = new ArrayList<String>();
                        ext.addFeatures(tpls, local, feats );
                        
                        FeatureVector fv = new FeatureVector();
                        addFeatures(feats, alphabet, "1_", fv, false);
                    }
                }
                timer.stop();
                n += sent.size();
                if (n % 100 == 0) {
                    log.info("n=" + n + " Toks / sec: " + (n / timer.totSec())); 
                }
                timer.start();
            }
        }
        timer.stop();
        
        log.info("Average ms per sent: " + (timer.totMs() / sents.size() / trials));
        log.info("Toks / sec: " + (sents.getNumTokens() * trials / timer.totSec())); 
        log.info("Alphabet.size(): " + alphabet.size());
    }
 
    private void addFeatures(ArrayList<String> obsFeats, Alphabet<Object> alphabet, String prefix, FeatureVector fv, boolean isBiasFeat) {
        if (featureHashMod <= 0) {
            // Just use the features as-is.
            for (String obsFeat : obsFeats) {
                String fname = prefix + obsFeat;
                int fidx = alphabet.lookupIndex(new Feature(fname, isBiasFeat));
                if (fidx != -1) {
                    fv.add(fidx, 1.0);
                }
            }
        } else {
            // Apply the feature-hashing trick.
            for (String obsFeat : obsFeats) {
                String fname = prefix + obsFeat;
                int hash = MurmurHash3.murmurhash3_x86_32(fname, 0, fname.length(), 123456789);
                hash = FastMath.mod(hash, featureHashMod);
                int fidx = alphabet.lookupIndex(new Feature(hash, isBiasFeat));
                if (fidx != -1) {
                    int revHash = reverseHashCode(fname);
                    if (revHash < 0) {
                        fv.add(fidx, -1.0);
                    } else {
                        fv.add(fidx, 1.0);
                    }
                }
            }
        }
    }

    /**
     * Returns the hash code of the reverse of this string.
     */
    private int reverseHashCode(String fname) {
        int hash = 0;
        int n = fname.length();
        for (int i=n-1; i>=0; i--) {
            hash += 31 * hash + fname.charAt(i);
        }
        return hash;
    }
    
    public static void main(String[] args) throws ParseException, IOException {
        (new FeatureExtractorSpeedTest()).testSpeed();
    }
    
}
