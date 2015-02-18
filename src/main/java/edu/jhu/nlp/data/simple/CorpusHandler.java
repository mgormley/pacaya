package edu.jhu.nlp.data.simple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.data.simple.AnnoSentenceReader.AnnoSentenceReaderPrm;
import edu.jhu.nlp.data.simple.AnnoSentenceReader.DatasetType;
import edu.jhu.nlp.data.simple.AnnoSentenceWriter.AnnoSentenceWriterPrm;
import edu.jhu.nlp.depparse.Projectivizer;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.prim.sample.Sample;
import edu.jhu.util.cli.Opt;
import edu.jhu.util.collections.Lists;

public class CorpusHandler {
    private static final Logger log = LoggerFactory.getLogger(CorpusHandler.class);

    // Options for train data
    @Opt(hasArg = true, description = "Training data input file or directory.")
    public static File train = null;
    @Opt(hasArg = true, description = "Type of training data input.")
    public static DatasetType trainType = null;
    @Opt(hasArg = true, description = "Training data predictions output file.")
    public static File trainPredOut = null;
    @Opt(hasArg = true, description = "Training data gold output file.")
    public static File trainGoldOut = null;
    @Opt(hasArg = true, description = "Type of training data ouput (predicted and gold). null implies same as trainType.")
    public static DatasetType trainTypeOut = null;
    @Opt(hasArg = true, description = "Maximum sentence length for train.")
    public static int trainMaxSentenceLength = Integer.MAX_VALUE;
    @Opt(hasArg = true, description = "Minimum sentence length for train.")
    public static int trainMinSentenceLength = 0;
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
    @Opt(hasArg = true, description = "Type of dev data ouput (predicted and gold). null implies same as trainType.")
    public static DatasetType devTypeOut = null;
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
    @Opt(hasArg = true, description = "Type of test data ouput (predicted and gold). null implies same as trainType.")
    public static DatasetType testTypeOut = null;
    @Opt(hasArg = true, description = "Maximum sentence length for test.")
    public static int testMaxSentenceLength = Integer.MAX_VALUE;
    @Opt(hasArg = true, description = "Maximum number of sentences to include in test.")
    public static int testMaxNumSentences = Integer.MAX_VALUE; 

    // Options for train/dev/test data
    @Opt(hasArg = true, description = "Random proportion of train data to allocate as dev data.")
    public static double propTrainAsDev = 0.0;

    // Options for SRL data munging.
    @Opt(hasArg = true, description = "SRL language.")
    public static String language = "es";

    // Options for dependency parse pre-processing.
    @Opt(hasArg = true, description = "Whether to projectivize the training depedendency parses")
    public static boolean trainProjectivize = false;
    
    // Options for data munging.
    @Opt(hasArg = true, description = "Whether to use gold POS tags.")
    public static boolean useGoldSyntax = false;    
    @Opt(hasArg=true, description="Whether to normalize the role names (i.e. lowercase and remove themes).")
    public static boolean normalizeRoleNames = false;    
    @Opt(hasArg = true, description = "Comma separated list of annotation types for restricting features/data.")
    public static String removeAts = null;
    @Opt(hasArg = true, description = "Comma separated list of annotation types for predicted annotations.")
    public static String predAts = null;
    @Opt(hasArg = true, description = "Comma separated list of annotation types for latent annotations.")
    public static String latAts = null;
    
    // Reader-Specific Options
    @Opt(hasArg = true, description = "CoNLL-X: whether to use the P(rojective)HEAD column for parents.")
    public static boolean trainUseCoNLLXPhead = false;
    @Opt(hasArg = true, description = "Tool name of dependency parse for ConcreteReader.")
    public static String concreteDepParseTool = "basic-deps";
    
    ////// TODO: use these options... /////
    // @Opt(hasArg=true, description="Whether to normalize and clean words.")
    // public static boolean normalizeWords = false;
    ///////////////////////////////////////
    
    private AnnoSentenceCollection trainGoldSents;
    private AnnoSentenceCollection trainInputSents;
    private AnnoSentenceCollection devGoldSents;
    private AnnoSentenceCollection devInputSents;
    private AnnoSentenceCollection testGoldSents;
    private AnnoSentenceCollection testInputSents;
    
    private AnnoSentenceCollection trainAsDevSents;

    // -------------------- Train data --------------------------
    
    public boolean hasTrain() {
        return train != null && trainType != null;
    }
    
    public AnnoSentenceCollection getTrainGold() throws IOException {
        if (trainGoldSents == null) {
            loadTrain();
        }
        return trainGoldSents;
    }
    
    public AnnoSentenceCollection getTrainInput() throws IOException {
        if (trainInputSents == null) {
            loadTrain();
        }
        return trainInputSents;
    }
    
    public void clearTrainCache() {
        trainGoldSents = null;
        trainInputSents = null;
    }
    
    public void writeTrainPreds(AnnoSentenceCollection trainPredSents) throws IOException {
        if (trainPredOut != null) {
            AnnoSentenceWriterPrm wPrm = new AnnoSentenceWriterPrm();
            wPrm.name = "predicted train";
            AnnoSentenceWriter writer = new AnnoSentenceWriter(wPrm);
            writer.write(trainPredOut, getTrainTypeOut(), trainPredSents, getPredAts());
        }
    }

    public DatasetType getTrainTypeOut() {
        return (trainTypeOut != null) ? trainTypeOut : trainType;
    }
    
    private void loadTrain() throws IOException {
        // Read train data.
        AnnoSentenceReaderPrm prm = getDefaultReaderPrm();
        prm.name = "train";
        prm.maxNumSentences = trainMaxNumSentences;
        prm.maxSentenceLength = trainMaxSentenceLength;
        prm.minSentenceLength = trainMinSentenceLength;
        prm.useCoNLLXPhead = trainUseCoNLLXPhead;
        AnnoSentenceReader reader = new AnnoSentenceReader(prm);
        reader.loadSents(train, trainType);
         
        // Cache gold train data.
        trainGoldSents = reader.getData();
        trainGoldSents = trainGoldSents.getWithAtsRemoved(getRemoveAts());
        
        if (hasTrain() && propTrainAsDev > 0) {
            // Split into train and dev.
            trainAsDevSents = new AnnoSentenceCollection();
            AnnoSentenceCollection tmp = new AnnoSentenceCollection();
            sample(trainGoldSents, propTrainAsDev, trainAsDevSents, tmp);
            trainGoldSents = tmp;
        }
        
        // TODO: Maybe move into a pre-processing pipeline.
        if (trainProjectivize) {
            log.info("Projectivizing training trees");
            new Projectivizer().projectivize(trainGoldSents);
        }
                
        // Cache input train data.
        trainInputSents = trainGoldSents.getWithAtsRemoved(getGoldOnlyAts());
    }

    public void writeTrainGold() throws IOException {
        if (trainGoldOut != null) {
            // Write gold train data.
            AnnoSentenceWriterPrm wPrm = new AnnoSentenceWriterPrm();
            wPrm.name = "gold train";
            AnnoSentenceWriter writer = new AnnoSentenceWriter(wPrm);
            writer.write(trainGoldOut, getTrainTypeOut(), trainGoldSents, new HashSet<AT>());
        }
    }
    
    /**
     * Splits inList into two other lists.
     * 
     * @param inList 
     * @param prop The proportion of inList to sample into outList1.
     * @param outList1 The sample.
     * @param outList2 The remaining (not sampled) entries.
     */
    public static <T> void sample(List<T> inList, double prop, List<T> outList1, List<T> outList2) {
        if (prop < 0 || 1 < prop) {
            throw new IllegalStateException("Invalid proportion: " + prop);
        }
        int numDev = (int) Math.ceil(prop * inList.size());
        log.info("Num train-as-dev examples: " + numDev);
        boolean[] isDev = Sample.sampleWithoutReplacementBooleans(numDev, inList.size());
        for (int i=0; i<inList.size(); i++) {
            if (isDev[i]) {
                outList1.add(inList.get(i));
            } else {
                outList2.add(inList.get(i));
            }
        }
    }

    // -------------------- Dev data --------------------------

    public boolean hasDev() {
        return (dev != null && devType != null) || (hasTrain() && propTrainAsDev > 0);
    }
    
    public AnnoSentenceCollection getDevGold() throws IOException {
        if (devGoldSents == null) {
            loadDev();
        }
        return devGoldSents;
    }
    
    public AnnoSentenceCollection getDevInput() throws IOException {
        if (devInputSents == null) {
            loadDev();
        }
        return devInputSents;
    }
    
    public void clearDevCache() {
        devGoldSents = null;
        devInputSents = null;
    }
    
    public void writeDevPreds(AnnoSentenceCollection devPredSents) throws IOException {
        if (devPredOut != null) {
            AnnoSentenceWriterPrm wPrm = new AnnoSentenceWriterPrm();
            wPrm.name = "predicted dev";
            AnnoSentenceWriter writer = new AnnoSentenceWriter(wPrm);
            writer.write(devPredOut, getDevTypeOut(), devPredSents, getPredAts());
        }
    }

    public DatasetType getDevTypeOut() {
        return (devTypeOut != null) ? devTypeOut : devType;
    }
    
    private void loadDev() throws IOException {
        if (dev != null && devType != null) {
            readDev();
        }
        if (hasTrain() && propTrainAsDev > 0) {
            loadTrainAsDev();
        }        
    }

    public void writeDevGold() throws IOException {
        if (devGoldSents != null && devGoldOut != null) {
            // Write gold dev data.
            AnnoSentenceWriterPrm wPrm = new AnnoSentenceWriterPrm();
            wPrm.name = "gold dev";
            AnnoSentenceWriter writer = new AnnoSentenceWriter(wPrm);
            writer.write(devGoldOut, getDevTypeOut(), devGoldSents, new HashSet<AT>());
        }
    }    
    
    private void readDev() throws IOException {        
        // Read dev data.
        AnnoSentenceReaderPrm prm = getDefaultReaderPrm();  
        prm.name = "dev";
        prm.maxNumSentences = devMaxNumSentences;
        prm.maxSentenceLength = devMaxSentenceLength;        
        AnnoSentenceReader reader = new AnnoSentenceReader(prm);
        reader.loadSents(dev, devType);
         
        // Cache gold dev data.
        devGoldSents = reader.getData();
        devGoldSents = devGoldSents.getWithAtsRemoved(getRemoveAts());

        // Cache input dev data.
        devInputSents = devGoldSents.getWithAtsRemoved(getGoldOnlyAts());
    }
    
    private void loadTrainAsDev() throws IOException {
        if (trainAsDevSents == null) {
            // Ensure that trainAsDevSents is loaded.
            loadTrain();
        }
        if (devGoldSents == null) {
            devGoldSents = new AnnoSentenceCollection();
        }
        for (AnnoSentence sent : trainAsDevSents) {
            devGoldSents.add(sent);
        }
        devInputSents = devGoldSents.getWithAtsRemoved(getGoldOnlyAts());
    }
    
    // -------------------- Test data --------------------------

    public boolean hasTest() {
        return test != null && testType != null;
    }
    
    public AnnoSentenceCollection getTestGold() throws IOException {
        if (testGoldSents == null) {
            loadTest();
        }
        return testGoldSents;
    }
    
    public AnnoSentenceCollection getTestInput() throws IOException {
        if (testInputSents == null) {
            loadTest();
        }
        return testInputSents;
    }
    
    public void clearTestCache() {
        testGoldSents = null;
        testInputSents = null;
    }
    
    public void writeTestPreds(AnnoSentenceCollection testPredSents) throws IOException {
        if (testPredOut != null) {
            AnnoSentenceWriterPrm wPrm = new AnnoSentenceWriterPrm();
            wPrm.name = "predicted test";
            AnnoSentenceWriter writer = new AnnoSentenceWriter(wPrm);
            writer.write(testPredOut, getTestTypeOut(), testPredSents, getPredAts());
        }
    }

    public DatasetType getTestTypeOut() {
        return (testTypeOut != null) ? testTypeOut : testType;
    }
    
    private void loadTest() throws IOException {
        // Read test data.
        AnnoSentenceReaderPrm prm = getDefaultReaderPrm();        
        prm.name = "test";
        prm.maxNumSentences = testMaxNumSentences;
        prm.maxSentenceLength = testMaxSentenceLength;        
        AnnoSentenceReader reader = new AnnoSentenceReader(prm);
        reader.loadSents(test, testType);
         
        // Cache gold test data.
        testGoldSents = reader.getData();
        testGoldSents = testGoldSents.getWithAtsRemoved(getRemoveAts());
        
        // Cache input test data.
        testInputSents = testGoldSents.getWithAtsRemoved(getGoldOnlyAts());
    }

    public void writeTestGold() throws IOException {
        if (testGoldSents != null && testGoldOut != null) {
            // Write gold test data.
            AnnoSentenceWriterPrm wPrm = new AnnoSentenceWriterPrm();
            wPrm.name = "gold test";
            AnnoSentenceWriter writer = new AnnoSentenceWriter(wPrm);
            writer.write(testGoldOut, getTestTypeOut(), testGoldSents, new HashSet<AT>());
        }
    }
    
    private AnnoSentenceReaderPrm getDefaultReaderPrm() {
        AnnoSentenceReaderPrm prm = new AnnoSentenceReaderPrm();
        prm.normalizeRoleNames = normalizeRoleNames;
        prm.useGoldSyntax = useGoldSyntax;
        prm.rePrm.depParseTool = concreteDepParseTool;
        return prm;
    }
    
    /** Gets predicated annotations (included only in the gold data). */
    public static List<AT> getPredAts() {
        return getAts(predAts);
    }

    /** Gets latent annotations (included only in the gold data). */
    public static List<AT> getLatAts() {
        return getAts(latAts);
    }

    /** Gets the annotations removed from both gold and input data. */
    public static List<AT> getRemoveAts() {
        return getAts(removeAts);
    }

    /** Gets predicated and latent annotations (included only in the gold data). */
    public static List<AT> getGoldOnlyAts() {
        return Lists.union(getPredAts(), getLatAts());
    }
    
    public static List<AT> getAts(String atsStr) {
        if (atsStr == null) {
            return Collections.emptyList();
        }       
        String[] splits = atsStr.split(",");
        ArrayList<AT> ats = new ArrayList<AT>();
        for (String s : splits) {
            ats.add(AT.valueOf(s));
        }
        return ats;
    }
    
}
