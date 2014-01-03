package edu.jhu.data;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import edu.jhu.tag.BrownClusterTagger;
import edu.jhu.tag.FileMapTagReducer;
import edu.jhu.tag.OovTagReducer;
import edu.jhu.tag.Ptb45To17TagReducer;
import edu.jhu.util.Alphabet;
import edu.jhu.util.cli.Opt;

public class DepTreebankReader {

    private static final String OOV_WORD_OR_TAG = "UNK";

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
    @Opt(hasArg = true, description = "Whether to use predicted POS tags (if available).")
    public static boolean usePredictedPosTags = false;
    @Opt(hasArg = true, description = "Brown cluster file")
    public static File brownClusters = null;
    @Opt(name = "maxTagLength", hasArg = true, description = "Maximum length for brown cluster tag.")
    public static int maxTagLength = Integer.MAX_VALUE;
    
    public static DepTreebank getTreebank(File trainPath, DatasetType trainType, Alphabet<Label> alphabet) throws IOException {
        return getTreebank(trainPath, trainType, maxSentenceLength, alphabet);
    }
    
    public static DepTreebank getTreebank(File trainPath, DatasetType trainType, int maxSentenceLength, Alphabet<Label> alphabet) throws IOException {
        // Create the original trainTreebank with a throw-away alphabet.
        DepTreebank trainTreebank = new DepTreebank(new Alphabet<Label>());
        DepTreebankLoader loader = new DepTreebankLoader(maxSentenceLength, maxNumSentences);
        if (mustContainVerb) {
            if (trainType != DatasetType.PTB) {
                throw new IllegalStateException("mustContainVerb option only compatible with English PTB input.");
            }
            loader.setTreeFilter(new VerbTreeFilter());
        }
        
        if (trainType == DatasetType.PTB) {
            loader.loadPtbPath(trainTreebank, trainPath);
        } else if (trainType == DatasetType.CONLL_X) {
            loader.loadCoNLLXPath(trainTreebank, trainPath);
        } else if (trainType == DatasetType.CONLL_2009) {
            loader.loadCoNLL09Path(trainTreebank, trainPath);
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
        
        if (brownClusters != null) {
            log.info("Adding Brown clusters.");
            BrownClusterTagger bct = new BrownClusterTagger(maxTagLength);
            bct.read(brownClusters);
            for (DepTree tree : trainTreebank) {
                for (DepTreeNode node : tree) {
                    if (node.getLabel() instanceof TaggedWord) {
                        TaggedWord tw = (TaggedWord) node.getLabel();
                        tw.setTag(bct.getCluster(tw.getWord()));
                    }
                }
            }
            log.info("Brown cluster miss rate: " + bct.getMissRate());
        }

        // Always add the OOV tag to the alphabet.
        TaggedWord unk = new TaggedWord(OOV_WORD_OR_TAG, OOV_WORD_OR_TAG);
        alphabet.lookupIndex(unk);
        (new OovTagReducer(alphabet, OOV_WORD_OR_TAG)).reduceTags(trainTreebank);
        
        // After reducing tags we create an entirely new treebank that uses the alphabet we care about.        
        DepTreebank tmpTreebank = new DepTreebank(alphabet);
        for (DepTree tree : trainTreebank) {
            tmpTreebank.add(tree);
        }
        trainTreebank = tmpTreebank;
        return trainTreebank;
    }

}
