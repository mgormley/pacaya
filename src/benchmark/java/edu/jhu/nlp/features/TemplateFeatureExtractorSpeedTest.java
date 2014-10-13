package edu.jhu.nlp.features;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReaderSpeedTest;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate;
import edu.jhu.nlp.relations.FeatureUtils;
import edu.jhu.nlp.words.PrefixAnnotator;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.Timer;

public class TemplateFeatureExtractorSpeedTest {
    
    private static final Logger log = Logger.getLogger(TemplateFeatureExtractorSpeedTest.class);
    private static final int featureHashMod = 1000000;
    
    /**
     * Speed test results.
     * 
     * Gilim:
     *    w/hash s=800 n=19560 Toks / sec: 238.92
     *    w/o    s=800 n=19560 Toks / sec: 560.39
     *    
     * Shasta:
     * 	  w/hash s=800 n=19560 Toks / sec: 375.66 (was 338.00 w/Feature object)
     * 	  w/o    s=800 n=19560 Toks / sec: 723.21
     */
    //@Test
    public void testSpeed() throws ParseException, IOException {
        List<FeatTemplate> tpls = TemplateSets.getFromResource(TemplateSets.mcdonaldDepFeatsResource);
        AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.readPtbYmConllx();
        PrefixAnnotator.addPrefixes(sents);
        
        int trials = 3;
        
        FeatureNames alphabet = new FeatureNames();
        
        Timer timer = new Timer();
        timer.start();
        int n=0;
        for (int trial = 0; trial < trials; trial++) {
            for (int s=0; s<sents.size(); s++) {
                AnnoSentence sent = sents.get(s);
                TemplateFeatureExtractor ext = new TemplateFeatureExtractor(sent, null);
                for (int i = -1; i < sent.size(); i++) {
                    for (int j = 0; j < sent.size(); j++) {
                        LocalObservations local = LocalObservations.newPidxCidx(i, j);
                        ArrayList<String> feats = new ArrayList<String>();
                        ext.addFeatures(tpls, local, feats );
                        
                        //FeatureVector fv = new FeatureVector();
                        //FeatureUtils.addFeatures(feats, alphabet, fv, false, featureHashMod);
                    }
                }
                timer.stop();
                n += sent.size();
                if (s % 100 == 0) {
                    log.info("s="+s+" n=" + n + " Toks / sec: " + (n / timer.totSec())); 
                }
                timer.start();
            }
        }
        timer.stop();
        
        log.info("Average ms per sent: " + (timer.totMs() / sents.size() / trials));
        log.info("Toks / sec: " + (sents.getNumTokens() * trials / timer.totSec())); 
        log.info("Alphabet.size(): " + alphabet.size());
    }
    
    public static void main(String[] args) throws ParseException, IOException {
        (new TemplateFeatureExtractorSpeedTest()).testSpeed();
    }
    
}
