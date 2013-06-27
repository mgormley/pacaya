package edu.jhu.gm.data;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import data.DataSample;
import data.FeatureFile;
import dataParser.DataParser;
import featParser.FeatureFileParser;

/**
 * Reads input files in ERMA format.
 * 
 * @author mgormley
 * 
 */
public class ErmaReader {

    private static final Logger log = Logger.getLogger(ErmaReader.class);
    private ArrayList<DataSample> examples;
    private FeatureFile ff;

    /**
     * Reads a feature file containing templates of features and a data file
     * containing a list of examples.
     * 
     * @param featureTemplate
     *            The path to the feature file.
     * @param dataFile
     *            The path to the data file.
     */
    public void read(String featureTemplate, String dataFile) {
        FeatureFileParser fp;
        log.info("Reading features from " + featureTemplate);
        try {
            fp = FeatureFileParser.createParser(featureTemplate);
            ff = fp.parseFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("Reading data from " + dataFile);
        DataParser dp;
        try {
            dp = DataParser.createParser(dataFile, ff);
            examples = dp.parseFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the feature templates read from the feature file.
     * 
     * @return An object representing the feature file.
     */
    public FeatureFile getFeatureFile() {
        return ff;
    }

    /**
     * Gets the data read from the data file.
     * 
     * @return A list of data samples.
     */
    public ArrayList<DataSample> getData() {
        return examples;
    }

}
