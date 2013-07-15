package edu.jhu.featurize;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import org.apache.log4j.Logger;

//import com.google.common.collect.Maps;

import edu.berkeley.nlp.PCFGLA.smoothing.*;

import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.util.Alphabet;
import edu.jhu.data.Label;

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
    public static String note=null;
    @Opt(name="brown", hasArg=true, description="--brown [FILE] = Use Brown Clusters from [FILE] rather than POS tags at cut = 5.")
    public static String brown="";
    @Opt(name="cutoff", hasArg=true, description="Cutoff for OOV words.")
    public static int cutoff=3;
    @Opt(name="use-PHEAD", hasArg=false, description="Use Predicted HEAD rather than Gold HEAD.")
    public static boolean goldHead = true;
    @Opt(name="no-link-factor", hasArg=false, description="Do not factor model on input dependency links.")
    public static boolean linkFactor = false;
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
        preprocess();
        knownWords = setDictionary(words);
        knownUnks = setDictionary(unks);
        // Currently not actually using bigram dictionary.
        knownBigrams = setBigramDictionary(bigrams);
        process();
    }
    
    public void preprocess() throws IOException {
            makeOutFiles();
            knownUnks.add("UNK");
            CoNLL09FileReader cr = new CoNLL09FileReader(new File(trainingFile));
            for (CoNLL09Sentence sent : cr) {
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
                    if (!goldHead) {
                        knownPostags.add(word.getPpos());
                    } else {
                        knownPostags.add(word.getPos());                        
                    }
                    for (CoNLL09Token word2 : sent) {
                        String wordForm2 = word2.getForm();
                        String cleanWord2 = normalize.clean(wordForm2);
                        String unkWord2 = sig.getSignature(wordForm2, word2.getId(), language);
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
        for (CoNLL09Sentence sent : cr) {
            System.out.println(example);
            example++;
            Map<Set<Integer>,String> truePreds = new HashMap<Set<Integer>,String>();
            List<SrlEdge> srlEdges = sent.getSrlGraph().getEdges();
            for (SrlEdge e : srlEdges) {
                Set<Integer> key = new HashSet<Integer>();
                key.add(e.getPred().getId());
                key.add(e.getArg().getId());
                truePreds.put(key, e.getLabel().toLowerCase());
            }
            Set<String> features = new HashSet<String>();
            Set<String> variables = new HashSet<String>();
            for (int i = 0;i < sent.size();i++) {
                // Get words for annotated sentence
                for (int j = 0; j < sent.size(); j++) {
                    //if (Math.abs(i-j) <= maxSentLength) {
                        Set<String> suffixes = new HashSet<String>();
                        suffixes = getSuffixes(i, j, sent);
                        features = getArgumentFeatures(i, j, suffixes, sent, features, isTrain);
                        variables = getVariables(i, j, sent, truePreds, variables);
                    //} 
                }
            }
            printOut(variables, features, example, bw);
        }
    }
    
    // Naradowsky argument features.
    public Set<String> getArgumentFeatures(int aidx, int pidx, Set<String> suffixes, CoNLL09Sentence sent, Set<String> feats, boolean isTrain) {
        CoNLL09Token pred = sent.get(pidx);
        CoNLL09Token arg = sent.get(aidx);
        String predForm = decideForm(pred.getForm(), pidx);
        String argForm = decideForm(arg.getForm(), aidx);
        String predPos = pred.getPos();
        String argPos = arg.getPos();
        String dir;
        int dist = Math.abs(aidx - pidx);
        if (aidx > pidx) 
            dir = "RIGHT";
        else if (aidx < pidx) 
            dir = "LEFT";
        else 
            dir = "SAME";

        Set<String> instFeats = new HashSet<String>();
        instFeats.add("head_" + predForm + "dep_" + argForm + "_word");
        instFeats.add("head_" + predPos + "_dep_" + argPos + "_pos");
        instFeats.add("head_" + predForm + "_dep_" + argPos + "_wordpos");
        instFeats.add("head_" + predPos + "_dep_" + argForm + "_posword");
        instFeats.add("head_" + predForm + "_dep_" + argForm + "_head_" + predPos + "_dep_" + argPos + "_wordwordpospos");

        instFeats.add("head_" + predPos + "_dep_" + argPos + "_dist_" + dist + "_posdist");
        instFeats.add("head_" + predPos + "_dep_" + argPos + "_dir_" + dir + "_posdir");
        instFeats.add("head_" + predPos + "_dist_" + dist + "_dir_" + dir + "_posdistdir");
        instFeats.add("head_" + argPos + "_dist_" + dist + "_dir_" + dir + "_posdistdir");

        instFeats.add("slen_" + sent.size());
        instFeats.add("dir_" + dir);
        instFeats.add("dist_" + dist);
        instFeats.add("dir_dist_" + dir + dist);

        instFeats.add("head_" + predForm + "_word");
        instFeats.add("head_" + predPos + "_tag");
        instFeats.add("arg_" + argForm + "_word");
        instFeats.add("arg_" + argPos + "_tag");
        for (String feat : instFeats) {
            if (isTrain || allFeatures.contains(feat)) {
                if (isTrain) {
                    if (!allFeatures.contains(feat)) {
                        allFeatures.add(feat);
                    }
                }
                for (String suf : suffixes) {
                    feats.add(feat + suf);
                }
            }
        }
        return feats;
    }
        
    private String decideForm(String wordForm, int idx) {
        if (!knownWords.contains(wordForm)) {
            wordForm = sig.getSignature(wordForm, idx, language);
            if (!knownUnks.contains(wordForm)) {
                wordForm = "UNK";
                return wordForm;
            }
        }
        Iterator<Entry<String, String>> it = stringMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            wordForm = wordForm.replace((String) pairs.getKey(), (String) pairs.getValue());
        }
        return wordForm;
    }

    public Set<String> getVariables(int i, int j, CoNLL09Sentence sent, Map truePreds, Set<String> variables) {
        Set<Integer> key = new HashSet<Integer>();
        key.add(i);
        key.add(j);
        String variable;
        if (truePreds.containsKey(key)) {
            String label = (String) truePreds.get(key);
            // Will Matt's implementation break if I don't handle this case? if (knownRoles.contains(label)) {
            variable = "ROLE Role_" + Integer.toString(i) + "_" + Integer.toString(j) + "=" + label + ";";
            //} 
        } else {
            variable = "ROLE Role_" + Integer.toString(i) + "_" + Integer.toString(j) + ";";
        }
        variables.add(variable);
        return variables;
    }
    
    public Set<String> getSuffixes(int i, int j, CoNLL09Sentence sent) {
        Set<String> suffixes = new HashSet<String>();
        suffixes.add("_role(Role_" + Integer.toString(i) + "_" + Integer.toString(j) + ");");
        return suffixes;
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
        tw.newLine();
        tw.newLine();
        tw.write("features:");
        tw.newLine();
        for (String feature : allFeatures) {
            tw.write(feature + "_role(ROLE):=[*]");
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