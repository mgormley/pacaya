package edu.jhu.data.simple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.data.simple.SimpleAnnoSentenceReader.DatasetType;
import edu.jhu.data.simple.SimpleAnnoSentenceReader.SimpleAnnoSentenceReaderPrm;
import edu.jhu.data.simple.SimpleAnnoSentenceWriter.SimpleAnnoSentenceWriterPrm;
import edu.jhu.featurize.TemplateLanguage.AT;
import edu.jhu.util.cli.Opt;

public class CorpusHandler {

    // Options for train data
    @Opt(hasArg = true, description = "Training data input file or directory.")
    public static File train = null;
    @Opt(hasArg = true, description = "Type of training data.")
    public static DatasetType trainType = DatasetType.CONLL_2009;
    @Opt(hasArg = true, description = "Training data predictions output file.")
    public static File trainPredOut = null;
    @Opt(hasArg = true, description = "Training data gold output file.")
    public static File trainGoldOut = null;
    @Opt(hasArg = true, description = "Maximum sentence length for train.")
    public static int trainMaxSentenceLength = Integer.MAX_VALUE;
    @Opt(hasArg = true, description = "Maximum number of sentences to include in train.")
    public static int trainMaxNumSentences = Integer.MAX_VALUE; 
    
    // Options for dev data
    @Opt(hasArg = true, description = "Testing data input file or directory.")
    public static File dev = null;
    @Opt(hasArg = true, description = "Type of dev data.")
    public static DatasetType devType = DatasetType.CONLL_2009;
    @Opt(hasArg = true, description = "Testing data predictions output file.")
    public static File devPredOut = null;
    @Opt(hasArg = true, description = "Testing data gold output file.")
    public static File devGoldOut = null;
    @Opt(hasArg = true, description = "Maximum sentence length for dev.")
    public static int devMaxSentenceLength = Integer.MAX_VALUE;
    @Opt(hasArg = true, description = "Maximum number of sentences to include in dev.")
    public static int devMaxNumSentences = Integer.MAX_VALUE; 
    
    // Options for test data
    @Opt(hasArg = true, description = "Testing data input file or directory.")
    public static File test = null;
    @Opt(hasArg = true, description = "Type of testing data.")
    public static DatasetType testType = DatasetType.CONLL_2009;
    @Opt(hasArg = true, description = "Testing data predictions output file.")
    public static File testPredOut = null;
    @Opt(hasArg = true, description = "Testing data gold output file.")
    public static File testGoldOut = null;
    @Opt(hasArg = true, description = "Maximum sentence length for test.")
    public static int testMaxSentenceLength = Integer.MAX_VALUE;
    @Opt(hasArg = true, description = "Maximum number of sentences to include in test.")
    public static int testMaxNumSentences = Integer.MAX_VALUE; 

    // Options for train/dev/test data
    @Opt(hasArg = true, description = "Brown cluster file")
    public static File brownClusters = null;    

    // Options for SRL data munging.
    @Opt(hasArg = true, description = "SRL language.")
    public static String language = "es";
    
    // Options for data munging.
    @Opt(hasArg = true, description = "Whether to use gold POS tags.")
    public static boolean useGoldSyntax = false;    
    @Opt(hasArg=true, description="Whether to normalize the role names (i.e. lowercase and remove themes).")
    public static boolean normalizeRoleNames = false;    
    @Opt(hasArg = true, description = "Comma separated list of annotation types for restricting features/data.")
    public static String removeAts = null;
    
    ////// TODO: use these options... /////
    // @Opt(hasArg=true, description="Whether to normalize and clean words.")
    // public static boolean normalizeWords = false;
    ///////////////////////////////////////
    
    private SimpleAnnoSentenceCollection trainGoldSents;
    private SimpleAnnoSentenceCollection trainInputSents;
    private SimpleAnnoSentenceCollection devGoldSents;
    private SimpleAnnoSentenceCollection devInputSents;
    private SimpleAnnoSentenceCollection testGoldSents;
    private SimpleAnnoSentenceCollection testInputSents;

    // -------------------- Train data --------------------------
    
    public SimpleAnnoSentenceCollection getTrainGold() throws IOException {
        if (trainGoldSents == null) {
            loadTrain();
        }
        return trainGoldSents;
    }
    
    public SimpleAnnoSentenceCollection getTrainInput() throws IOException {
        if (trainInputSents == null) {
            loadTrain();
        }
        return trainInputSents;
    }
    
    public void clearTrainCache() {
        trainGoldSents = null;
        trainInputSents = null;
    }
    
    public void writeTrainPreds(SimpleAnnoSentenceCollection trainPredSents) throws IOException {
        if (trainPredOut != null) {
            SimpleAnnoSentenceWriterPrm wPrm = new SimpleAnnoSentenceWriterPrm();
            wPrm.name = "predicted train";
            SimpleAnnoSentenceWriter writer = new SimpleAnnoSentenceWriter(wPrm);
            writer.write(trainPredOut, trainType, trainPredSents);
        }
    }
    
    private void loadTrain() throws IOException {
        // Read train data.
        SimpleAnnoSentenceReaderPrm prm = getDefaultReaderPrm();
        prm.name = "train";
        prm.maxNumSentences = trainMaxNumSentences;
        prm.maxSentenceLength = trainMaxSentenceLength;        
        SimpleAnnoSentenceReader reader = new SimpleAnnoSentenceReader(prm);
        reader.loadSents(train, trainType);
         
        // Cache gold train data.
        trainGoldSents = reader.getData();
        
        if (trainGoldOut != null) {
            // Write gold train data.
            SimpleAnnoSentenceWriterPrm wPrm = new SimpleAnnoSentenceWriterPrm();
            wPrm.name = "gold train";
            SimpleAnnoSentenceWriter writer = new SimpleAnnoSentenceWriter(wPrm);
            writer.write(trainGoldOut, trainType, trainGoldSents);
        }
        
        // Cache input train data.
        trainInputSents = trainGoldSents.getWithAtsRemoved(getRemoveAts());
    }

    // -------------------- Dev data --------------------------

    public SimpleAnnoSentenceCollection getDevGold() throws IOException {
        if (devGoldSents == null) {
            loadDev();
        }
        return devGoldSents;
    }
    
    public SimpleAnnoSentenceCollection getDevInput() throws IOException {
        if (devInputSents == null) {
            loadDev();
        }
        return devInputSents;
    }
    
    public void clearDevCache() {
        devGoldSents = null;
        devInputSents = null;
    }
    
    public void writeDevPreds(SimpleAnnoSentenceCollection devPredSents) throws IOException {
        if (devPredOut != null) {
            SimpleAnnoSentenceWriterPrm wPrm = new SimpleAnnoSentenceWriterPrm();
            wPrm.name = "predicted dev";
            SimpleAnnoSentenceWriter writer = new SimpleAnnoSentenceWriter(wPrm);
            writer.write(devPredOut, devType, devPredSents);
        }
    }
    
    private void loadDev() throws IOException {
        // Read dev data.
        SimpleAnnoSentenceReaderPrm prm = getDefaultReaderPrm();  
        prm.name = "dev";
        prm.maxNumSentences = devMaxNumSentences;
        prm.maxSentenceLength = devMaxSentenceLength;        
        SimpleAnnoSentenceReader reader = new SimpleAnnoSentenceReader(prm);
        reader.loadSents(dev, devType);
         
        // Cache gold dev data.
        devGoldSents = reader.getData();
        
        if (devGoldOut != null) {
            // Write gold dev data.
            SimpleAnnoSentenceWriterPrm wPrm = new SimpleAnnoSentenceWriterPrm();
            wPrm.name = "gold dev";
            SimpleAnnoSentenceWriter writer = new SimpleAnnoSentenceWriter(wPrm);
            writer.write(devGoldOut, devType, devGoldSents);
        }
        
        // Cache input dev data.
        devInputSents = devGoldSents.getWithAtsRemoved(getRemoveAts());
    }
    
    // -------------------- Test data --------------------------

    public SimpleAnnoSentenceCollection getTestGold() throws IOException {
        if (testGoldSents == null) {
            loadTest();
        }
        return testGoldSents;
    }
    
    public SimpleAnnoSentenceCollection getTestInput() throws IOException {
        if (testInputSents == null) {
            loadTest();
        }
        return testInputSents;
    }
    
    public void clearTestCache() {
        testGoldSents = null;
        testInputSents = null;
    }
    
    public void writeTestPreds(SimpleAnnoSentenceCollection testPredSents) throws IOException {
        if (testPredOut != null) {
            SimpleAnnoSentenceWriterPrm wPrm = new SimpleAnnoSentenceWriterPrm();
            wPrm.name = "predicted test";
            SimpleAnnoSentenceWriter writer = new SimpleAnnoSentenceWriter(wPrm);
            writer.write(testPredOut, testType, testPredSents);
        }
    }
    
    private void loadTest() throws IOException {
        // Read test data.
        SimpleAnnoSentenceReaderPrm prm = getDefaultReaderPrm();        
        prm.name = "test";
        prm.maxNumSentences = testMaxNumSentences;
        prm.maxSentenceLength = testMaxSentenceLength;        
        SimpleAnnoSentenceReader reader = new SimpleAnnoSentenceReader(prm);
        reader.loadSents(test, testType);
         
        // Cache gold test data.
        testGoldSents = reader.getData();
        
        if (testGoldOut != null) {
            // Write gold test data.
            SimpleAnnoSentenceWriterPrm wPrm = new SimpleAnnoSentenceWriterPrm();
            wPrm.name = "gold test";
            SimpleAnnoSentenceWriter writer = new SimpleAnnoSentenceWriter(wPrm);
            writer.write(testGoldOut, testType, testGoldSents);
        }
        
        // Cache input test data.
        testInputSents = testGoldSents.getWithAtsRemoved(getRemoveAts());
    }

    
    private SimpleAnnoSentenceReaderPrm getDefaultReaderPrm() {
        SimpleAnnoSentenceReaderPrm prm = new SimpleAnnoSentenceReaderPrm();
        // TODO: prm.bcPrm.maxTagLength
        prm.bcPrm.language = language;
        prm.brownClusters = brownClusters;
        prm.normalizeRoleNames = normalizeRoleNames;
        prm.useGoldSyntax = useGoldSyntax;
        return prm;
    }
    

    private static List<AT> getRemoveAts() {
        if (removeAts == null) {
            return Collections.emptyList();
        }        
        String[] splits = removeAts.split(",");
        ArrayList<AT> ats = new ArrayList<AT>();
        for (String s : splits) {
            ats.add(AT.valueOf(s));
        }
        return ats;
    }
    
}
