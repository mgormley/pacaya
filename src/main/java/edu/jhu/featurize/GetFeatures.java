package edu.jhu.featurize;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.Set;

import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

//import com.google.common.collect.Maps;

import edu.berkeley.nlp.PCFGLA.smoothing.*;

import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.srl.MutableInt;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.featurize.ProcessSentence;
import edu.jhu.util.Alphabet;
import edu.jhu.data.Label;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureExtractor;
import edu.jhu.gm.FeatureVector;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.Var;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;
import edu.jhu.gm.CrfObjectiveTest.LogLinearExDesc;
import edu.jhu.gm.Var.VarType;

/**
 * Featurize sentence with CoNLL annotations.
 * @author mmitchell
 * 
 */
public class GetFeatures {
    
    private static final Logger log = Logger.getLogger(GetFeatures.class);
    
    @Opt(name="train", hasArg=true, required=true, description="Training file.")
    public static String trainingFile="/home/hltcoe/mgormley/working/parsing/exp/vem-conll_001/dmv_conll09-sp-dev_20_28800_True/train-parses.txt";
    @Opt(name="test", hasArg=true, required=true, description="Testing file.")
    public static String testingFile="/home/hltcoe/mgormley/working/parsing/exp/vem-conll_001/dmv_conll09-sp-dev_20_28800_True/test-parses.txt";
    @Opt(name="language", hasArg=true, description="Language (en or es).")
    public static String language = "es";
    @Opt(name="out-dir", hasArg=true, description="Output directory.")
    public static String outDir="train_test/";
    @Opt(name="note", hasArg=true, description="Note to append to files.")
    public static String note="";
    @Opt(name="brown", hasArg=true, description="--brown [FILE] = Use Brown Clusters from [FILE] rather than POS tags at cut = 5.")
    public static String brown="";
    @Opt(name="cutoff", hasArg=true, description="Cutoff for OOV words.")
    public static int cutoff=3;
    @Opt(name="use-PHEAD", hasArg=false, description="Use Predicted HEAD rather than Gold HEAD.")
    public static boolean goldHead = true;
    @Opt(name="predsGiven", hasArg=false, description="Semantic preds are given during training/testing.")
    public static boolean predsGiven = false;
    @Opt(name="dep-features", hasArg=false, description="Use dependency parse as features.")
    public static boolean depFeatures = false;
    @Opt(name="fast-crf", hasArg=false, description="Whether to use our fast CRF framework instead of ERMA")
    public static boolean crfFeatures = false;
    
    public Set<String> allFeatures = new HashSet<String>();
    public Set<String> knownWords = new HashSet<String>();
    public Set<String> knownUnks = new HashSet<String>();
    // Currently not using this (will it matter?);
    public Set<Set<String>> knownBigrams = new HashSet<Set<String>>();
    public Set<String> knownPostags = new HashSet<String>();
    public Set<String> knownRoles = new HashSet<String>();
    public Set<String> knownLinks = new HashSet<String>();
    public Integer maxSentLength = 0;
    
    private static Map<String,String> stringMap;
    private Map<String,MutableInt> words = new HashMap<String,MutableInt>();
    private Map<String,MutableInt> unks = new HashMap<String,MutableInt>();
    private Map<Set<String>,MutableInt> bigrams = new HashMap<Set<String>,MutableInt>();
    
    private static Alphabet<Label> lexAlphabet = new Alphabet<Label>();
    private static BerkeleySignatureBuilder sig = new BerkeleySignatureBuilder(lexAlphabet);
    
    private String trainingOut = new String();
    private String testingOut = new String();
    private String templateOut = new String();
        
    public static void main(String[] args) throws IOException {
        ArgParser parser = new ArgParser(GetFeatures.class);
        parser.addClass(GetFeatures.class);
        parser.addClass(CoNLL09Sentence.class);
        try {
            parser.parseArgs(args);
        } catch (ParseException e) {
            log.error(e.getMessage());
            parser.printUsage();
            System.exit(1);
        }
        setChars();
        GetFeatures modelSpec = new GetFeatures();
        modelSpec.run();
    }
    
    public void run() throws IOException {
        System.out.println(predsGiven);
        preprocess();
        knownWords = setDictionary(words);
        knownUnks = setDictionary(unks);
        // Currently not actually using bigram dictionary.
        //knownBigrams = setBigramDictionary(bigrams);
        process();
    }
    
    public void preprocess() throws IOException {
            makeOutFiles();
            // Store the variable states we have seen before so 
            // we know what our vocabulary of possible states are for
            // the Link variable.  Applies to knownLinks, knownRoles.
            knownLinks.add("True");
            knownLinks.add("False");
            knownUnks.add("UNK");
            CoNLL09FileReader cr = new CoNLL09FileReader(new File(trainingFile));
            for (CoNLL09Sentence sent : cr) {
                // Need to know max sent length because distance features
                // use these values explicitly; an unknown sentence length in
                // test data will result in an unknown feature.
                if (sent.size() > maxSentLength) {
                    maxSentLength = sent.size();
                }
                for (CoNLL09Token word : sent) {
                    for (int j = 0; j< word.getApreds().size(); j++) {
                        String[] splitRole = word.getApreds().get(j).split("-");
                        String role = splitRole[0].toLowerCase();
                        knownRoles.add(role);
                    }
                    String wordForm = word.getForm();
                    String cleanWord = normalize.clean(wordForm);
                    String unkWord = sig.getSignature(wordForm, word.getId(), language);
                    unkWord = normalize.escape(unkWord);
                    words = addWord(words, cleanWord);
                    unks = addWord(unks, unkWord);
                    // Learn what Postags are in our vocabulary
                    // Later, can then back off to NONE if we haven't seen it before.
                    if (!goldHead) {
                        knownPostags.add(word.getPpos());
                    } else {
                        knownPostags.add(word.getPos());                        
                    }
                    for (CoNLL09Token word2 : sent) {
                        String wordForm2 = word2.getForm();
                        String cleanWord2 = normalize.clean(wordForm2);
                        String unkWord2 = sig.getSignature(wordForm2, word2.getId(), language);
                        // TBD:  Actually use the seen/unseen bigrams to shrink the feature space.
                        addBigrams(cleanWord, cleanWord2);
                        addBigrams(unkWord, unkWord2);
                        addBigrams(cleanWord, unkWord2);
                        addBigrams(unkWord, cleanWord2);                        
                    }
                }
            }
    }
    
    public void makeOutFiles() {
        if (!note.equals("")) {
            note = "." + note;
        }
        trainingOut = "train" + note;
        templateOut = "template" + note;
        testingOut = "test" + note;        
    }
    
    public void process() throws IOException {
        File trainOut = new File(trainingOut);
        if (!trainOut.exists()) {
            trainOut.createNewFile();
        }
        FileWriter trainFW = new FileWriter(trainOut.getAbsoluteFile());
        BufferedWriter trainBW = new BufferedWriter(trainFW);
        
        File template = new File(templateOut);
        if (!template.exists()) {
            template.createNewFile();
        }
        FileWriter templateFW = new FileWriter(template.getAbsoluteFile());
        BufferedWriter templateBW = new BufferedWriter(templateFW);
        
        File testOut = new File(testingOut);
        if (!testOut.exists()) {
            testOut.createNewFile();
        }
        FileWriter testFW = new FileWriter(testOut.getAbsoluteFile());
        BufferedWriter testBW = new BufferedWriter(testFW);
        
        featuresToPrint(trainingFile, trainBW, true);
        trainBW.close();
        printTemplate(templateBW);
        templateBW.close();
        featuresToPrint(testingFile, testBW, false);
        testBW.close();
    }
        
    public void featuresToPrint(String inFile, BufferedWriter bw, boolean isTrain) throws IOException {
        CoNLL09FileReader cr = new CoNLL09FileReader(new File(inFile));
        int example = 0;
        boolean hasPred;
        for (CoNLL09Sentence sent : cr) {
            ProcessSentence ps = new ProcessSentence();
            FgExample fg = ps.getFGExample(sent, isTrain, predsGiven, goldHead, language, knownLinks, knownRoles, new Alphabet());
            example++;
            if(ps.getHasPred()) {
                printOut(ps.getVarConfig(), ps.getFeatures(), example, bw);
                //setData(variables, features, example, bw);
            }

        }
    }
    
    
    
    public void printOut(Set<String> sentenceVariables, Set<String> sentenceFeatures, int example, BufferedWriter bw) throws IOException {
        bw.write("//example " + Integer.toString(example));
        bw.newLine();
        bw.write("example:");
        bw.newLine();
        for (String var : sentenceVariables) {
            bw.write(var);
            bw.newLine();
        }
        bw.write("features:");
        bw.newLine();
        for (String feat : sentenceFeatures) {
            bw.write(feat);
            bw.newLine();
        }
    }
    

    
    public void printTemplate(BufferedWriter tw) throws IOException {
        StringBuilder sb = new StringBuilder();
        String delim = "";
        tw.write("types:");
        tw.newLine();
        sb.append("ROLE:=[");
        for (String role : knownRoles) {
            sb.append(delim).append(role);
            delim = ",";
        }
        sb.append("]");
        tw.write(sb.toString());
        sb = new StringBuilder();
        sb.append("LINK:=[");
        for (String link : knownLinks) {
            sb.append(delim).append(link);
            delim = ",";
        }
        sb.append("]");
        tw.write(sb.toString());
        tw.newLine();
        tw.newLine();
        tw.write("features:");
        tw.newLine();
        for (String feature : allFeatures) {
            tw.write(feature + "_role(ROLE):=[*]");
            tw.write(feature + "_link_role(LINK,ROLE):=[*]");
            tw.newLine();
        }
    }
    
    // ------------------- private ------------------- //
    private static void setChars() {
        // Really this would be nicer using guava...
        stringMap = new HashMap<String,String>();
        stringMap.put("1","2");
        stringMap.put(".","_P_");
        stringMap.put(",","_C_");
        stringMap.put("'","_A_");
        stringMap.put("%","_PCT_");
        stringMap.put("-","_DASH_");
        stringMap.put("$","_DOL_");
        stringMap.put("&","_AMP_");
        stringMap.put(":","_COL_");
        stringMap.put(";","_SCOL_");
        stringMap.put("\\/","_BSL_");
        stringMap.put("/","_SL_");
        stringMap.put("`","_QT_"); 
        stringMap.put("?","_Q_");
        stringMap.put("¿","_QQ_"); 
        stringMap.put("=","_EQ_"); 
        stringMap.put("*","_ST_");
        stringMap.put("!","_E_"); 
        stringMap.put("¡","_EE_"); 
        stringMap.put("#","_HSH_"); 
        stringMap.put("@","_AT_"); 
        stringMap.put("(","_LBR_"); 
        stringMap.put(")","_RBR_");
        stringMap.put("\"","_QT1_"); 
        stringMap.put("Á","_A_ACNT_"); 
        stringMap.put("É","_E_ACNT_"); 
        stringMap.put("Í","_I_ACNT_"); 
        stringMap.put("Ó","_O_ACNT_"); 
        stringMap.put("Ú","_U_ACNT_");
        stringMap.put("Ü","_U_ACNT2_"); 
        stringMap.put("Ñ","_N_ACNT_"); 
        stringMap.put("á","_a_ACNT_"); 
        stringMap.put("é","_e_ACNT_"); 
        stringMap.put("í","_i_ACNT_"); 
        stringMap.put("ó","_o_ACNT_"); 
        stringMap.put("ú","_u_ACNT_"); 
        stringMap.put("ü","_u_ACNT2_"); 
        stringMap.put("ñ","_n_ACNT_"); 
        stringMap.put("º","_deg_ACNT_");
        
    }

    private void addBigrams(String w1, String w2) {
        HashSet<String> pair = new HashSet<String>(Arrays.asList(w1, w2));
        MutableInt count = bigrams.get(pair);
        if (count == null) {
            bigrams.put(pair, new MutableInt());
        } else {
            count.increment();
        }
    }
    
    private Map<String, MutableInt> addWord(Map<String,MutableInt> inputHash, String w) {
        MutableInt count = inputHash.get(w);
        if (count == null) {
            inputHash.put(w,  new MutableInt());
        } else {
            count.increment();
        }
        return inputHash;
    }
    
    private Set<Set<String>> setBigramDictionary(Map<Set<String>,MutableInt> inputHash) {
        // Version of below, for bigrams.
        Set<Set<String>> knownHash =  new HashSet<Set<String>>();
        Iterator<Entry<Set<String>, MutableInt>> it = inputHash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            int count = ((MutableInt) pairs.getValue()).get();
            if (count > cutoff) {
                knownHash.add((Set<String>) pairs.getKey());
            } 
        }
        return knownHash;
    }
    
    private Set<String> setDictionary(Map<String,MutableInt> inputHash) {
        Set<String> knownHash =  new HashSet<String>();
        Iterator<Entry<String, MutableInt>> it = inputHash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            int count = ((MutableInt) pairs.getValue()).get();
            if (count > cutoff) {
                knownHash.add((String) pairs.getKey());
            } 
        }
        return knownHash;
    }

    
    
 public static class normalize {
     private static final Pattern digits = Pattern.compile("\\d+");
     private static final Pattern punct = Pattern.compile("[^A-Za-z0-9_ÁÉÍÓÚÜÑáéíóúüñ]");    

     public static String escape(String s) {
        Iterator<Entry<String, String>> it = stringMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String)entry.getKey();
            String val = (String)entry.getValue();
            s.replace(key,val);
        }        
        return punct.matcher(s).replaceAll("_");
     }
    
     public static String norm_digits(String s) {
        return digits.matcher(s).replaceAll("0");
     }
    
     public static String clean(String s) {
        s = escape(s);
        s = norm_digits(s.toLowerCase());
        return s;
     }
 }
    
}