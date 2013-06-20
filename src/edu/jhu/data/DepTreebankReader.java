package edu.jhu.hltcoe.data;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.util.Alphabet;
import edu.jhu.hltcoe.util.cli.Opt;

public class DepTreebankReader {

    public enum DatasetType { SYNTHETIC, PTB, CONLL_X, CONLL_2009 };

    private static final Logger log = Logger.getLogger(DepTreebankReader.class);

    @Opt(hasArg = true, description = "Maximum sentence length for train data.")
    public static int maxSentenceLength = Integer.MAX_VALUE;
    @Opt(hasArg = true, description = "Maximum number of sentences/trees to include in training data.")
    public static int maxNumSentences = Integer.MAX_VALUE; 
    @Opt(hasArg = true, description = "Whether each sentence must contain a verb.")
    public static boolean mustContainVerb = false;
    @Opt(hasArg = true, description = "Type or file indicating tag mapping")
    public static String reduceTags = "none";
        
    public static DepTreebank getTreebank(String trainPath, DatasetType trainType, Alphabet<Label> alphabet) throws IOException {
        return getTreebank(trainPath, trainType, maxSentenceLength, alphabet);
    }
    
    public static DepTreebank getTreebank(String trainPath, DatasetType trainType, int maxSentenceLength, Alphabet<Label> alphabet) throws IOException {
        DepTreebank trainTreebank;

        // Create the original trainTreebank with a throw-away alphabet.
        trainTreebank = new DepTreebank(maxSentenceLength, maxNumSentences, new Alphabet<Label>());
        if (mustContainVerb) {
            trainTreebank.setTreeFilter(new VerbTreeFilter());
        }
        
        if (trainType == DatasetType.PTB) {
            trainTreebank.loadPtbPath(trainPath);
        } else if (trainType == DatasetType.CONLL_X) {
            trainTreebank.loadCoNLLXPath(trainPath);
        } else if (trainType == DatasetType.CONLL_2009) {
            trainTreebank.loadCoNLL09Path(trainPath);
        } else {
            throw new RuntimeException("Unhandled dataset type: " + trainType);
        }
        
        if ("45to17".equals(reduceTags)) {
            log.info("Reducing PTB from 45 to 17 tags");
            (new Ptb45To17TagReducer()).reduceTags(trainTreebank);
        } else if (!"none".equals(reduceTags)) {
            log.info("Reducing tags with file map: " + reduceTags);
            (new FileMapTagReducer(new File(reduceTags))).reduceTags(trainTreebank);
        }

        // After reducing tags we create an entirely new treebank that uses the alphabet we care about.
        DepTreebank tmpTreebank = new DepTreebank(alphabet);
        for (DepTree tree : trainTreebank) {
            tmpTreebank.add(tree);
        }
        trainTreebank = tmpTreebank;
        return trainTreebank;
    }

}
