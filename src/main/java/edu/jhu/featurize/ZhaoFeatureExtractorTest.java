package edu.jhu.featurize;

import static edu.jhu.util.Utilities.getList;
import static org.junit.Assert.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.junit.Test;

import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.concrete.SimpleAnnoSentence;
import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.BinaryStrFVBuilder;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureTemplateList;
import edu.jhu.gm.FgExamples;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.srl.SrlFgModel;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.SrlRunner;
import edu.jhu.srl.SrlRunner.DatasetType;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Pair;
import edu.jhu.util.Timer;
import edu.jhu.util.Utilities;

public class ZhaoFeatureExtractorTest {
    
    private static final Logger log = Logger.getLogger(SrlRunner.class);

    public static void main(String[] args) {
        try {
            testZhaoFeatureBuilding();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static void testZhaoFeatureBuilding() throws ParseException, IOException {
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.withSupervision = true;
        fePrm.useNaradFeats = false;
        fePrm.useDepPathFeats = false;
        fePrm.useSimpleFeats = false;
        fePrm.useZhaoFeats = true;
        File train = new File("data/conll/CoNLL2009-ST-Spanish-trial.csv");
        CoNLL09FileReader reader = new CoNLL09FileReader(train);
        Timer timer = new Timer();
        timer.start();
        for (int n = 0; n < 49; n++) {
            CoNLL09Sentence sent = reader.next();
            SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(cs);
            log.info("Initializing sentence...");
            SentFeatureExtractor fe = new SentFeatureExtractor(fePrm, simpleSent, cs);
            log.info("Processing sentence...");
            for (int i = 0; i < sent.size(); i++) {
                for (int j = 0; j < sent.size(); j++) {
                    fe.createFeatureSet(i, j);
                }
            }
            timer.split();
            log.info("Done.");
        }
        timer.stop();
                
        log.info("Average ms per sent: " + timer.avgMs());
    }
    
}
