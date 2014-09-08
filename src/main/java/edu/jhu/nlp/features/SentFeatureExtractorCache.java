package edu.jhu.nlp.features;

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * Cache for SentFeatureExtractor.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class SentFeatureExtractorCache {

    private static final Logger log = Logger.getLogger(SentFeatureExtractorCache.class); 

    // Cache of observation features for each single positions in the sentence.
    private ArrayList[] obsFeatsSolo;
    // Cache of observation features for each pair of positions in the sentence.
    private ArrayList[][] obsFeatsPair;
    // Cache of observation features for each pair of root, child positions.
    private ArrayList[] obsFeatsPairForRoot;

    private SentFeatureExtractor sentFeatExt;
    
    public SentFeatureExtractorCache(SentFeatureExtractor sentFeatExt) {
        this.sentFeatExt = sentFeatExt;
        clear();
    }
    
    public ArrayList<String> fastGetObsFeats(int child) {
        if (obsFeatsSolo[child] == null) {
            // Lazily construct the observation features.
            obsFeatsSolo[child] = sentFeatExt.createFeatureSet(child);
        }
        return obsFeatsSolo[child];
    }
    
    public ArrayList<String> fastGetObsFeats(int parent, int child) {
        if (parent == -1) {
            return fastGetObsFeatsForRoot(child);
        }
        if (obsFeatsPair[parent][child] == null) {
            // Lazily construct the observation features.
            obsFeatsPair[parent][child] = sentFeatExt.createFeatureSet(parent, child);
        }
        return obsFeatsPair[parent][child];
    }
    
    private ArrayList<String> fastGetObsFeatsForRoot(int child) {
        if (obsFeatsPairForRoot[child] == null) {
            // Lazily construct the observation features.
            obsFeatsPairForRoot[child] = sentFeatExt.createFeatureSet(-1, child);
        }
        return obsFeatsPairForRoot[child];
    }

    public void clear() {
        obsFeatsSolo = new ArrayList[sentFeatExt.getSentSize()];
        obsFeatsPair = new ArrayList[sentFeatExt.getSentSize()][sentFeatExt.getSentSize()];
        obsFeatsPairForRoot = new ArrayList[sentFeatExt.getSentSize()];
    }

    
}