package edu.jhu.featurize;

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
    // Cache of observation features for sense features (separate model).
    private ArrayList[] obsSenseSolo;

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
        if (obsFeatsPair[parent][child] == null) {
            // Lazily construct the observation features.
            obsFeatsPair[parent][child] = sentFeatExt.createFeatureSet(parent, child);
        }
        return obsFeatsPair[parent][child];
    }
    
    public ArrayList<String> fastGetObsSenseFeats(int child) {
        if (obsSenseSolo[child] == null) {
            // Lazily construct the observation features.
            obsSenseSolo[child] = sentFeatExt.createSenseFeatureSet(child);
        }
        return obsSenseSolo[child];
    }

    public void clear() {
        obsFeatsSolo = new ArrayList[sentFeatExt.getSentSize()];
        obsFeatsPair = new ArrayList[sentFeatExt.getSentSize()][sentFeatExt.getSentSize()];
        obsSenseSolo = new ArrayList[sentFeatExt.getSentSize()];
    }

    
}