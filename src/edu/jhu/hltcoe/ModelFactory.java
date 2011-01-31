package edu.jhu.hltcoe;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


public class ModelFactory {

    public static void addOptions(Options options) {
        options.addOption("a", "algorithm", true, "Clustering algorithm (e.g. kmeans, constrained-kmeans, etc.).");
    }

    public static Model getModel(CommandLine cmd) throws ParseException  {

        Model clusterer = null;
        final String algorithm = cmd.hasOption("algorithm") ? 
        		cmd.getOptionValue("algorithm") : "constrained-kmeans";
        
        if (algorithm.equals("kmeans") || algorithm.equals("constrained-kmeans")) {
        
        } else {
            throw new ParseException("Algorithm not supported: " + algorithm);
        }
        
        return clusterer;
    }

}
