package edu.jhu.hltcoe.parse.cky;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import edu.jhu.hltcoe.util.Alphabet;

public class CnfGrammarBuilder {

    private int rootSymbol;
    private ArrayList<Rule> allRules;
    private ArrayList<Rule> lexRules;
    private ArrayList<Rule> unaryRules;
    private ArrayList<Rule> binaryRules;

    private Alphabet<String> lexAlphabet;
    private Alphabet<String> ntAlphabet;
    
    public CnfGrammarBuilder() {
        allRules = new ArrayList<Rule>();
        lexRules = new ArrayList<Rule>();
        unaryRules = new ArrayList<Rule>();
        binaryRules = new ArrayList<Rule>();
        
        lexAlphabet = new Alphabet<String>();
        ntAlphabet = new Alphabet<String>();
    }
    
    public CnfGrammar getGrammar() {
        return new CnfGrammar(allRules, rootSymbol, lexAlphabet, ntAlphabet);
    }
    
    public void loadFromFile(String filename) throws IOException {
        InputStream inputStream = new FileInputStream(filename);
        if (filename.endsWith(".gz")) {
            inputStream = new GZIPInputStream(inputStream);
        }
        loadFromInputStream(inputStream);
    }

    public void loadFromResource(String resourceName) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(
                resourceName);
        if (inputStream == null) {
            throw new RuntimeException("Unable to find resource: "
                    + resourceName);
        }
        loadFromInputStream(inputStream);
        inputStream.close();
    }
    
    private enum GrReaderState {
        RootSymbol,
        NonTerminalRules,
        LexicalRules,
    }

    public void loadFromInputStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        GrReaderState state = GrReaderState.RootSymbol;
        
        Pattern spaceRegex = Pattern.compile("\\s+");
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#") || line.equals("")) {
                // Skip comments and empty lines.
                continue;
            }
            
            if (line.startsWith("===")) {
                // Change states to reading lexical rules.
                state = GrReaderState.LexicalRules;
                continue;
            }
            
            if (state == GrReaderState.RootSymbol) {
                if (line.contains(" ")) {
                    // Read a bubs-parser style line with the root symbol in start=Root
                    rootSymbol = ntAlphabet.lookupIndex("ROOT");
                } else {
                    // Read the root symbol.
                    rootSymbol = ntAlphabet.lookupIndex(line);
                }
                state = GrReaderState.NonTerminalRules;
                continue;
            }
            
            // Read either a unary, binary, or lexical rule.
            String[] splits = spaceRegex.split(line);
            Rule r;
            if (splits.length == 4) {
                // Unary or lexical rule.
                // splits[1] is the separator "-->"
                String parent = splits[0];
                String child = splits[2];
                double logProb = Double.parseDouble(splits[3]);
                if (state == GrReaderState.LexicalRules) {
                    r = getLexicalRule(parent, child, logProb);
                    lexRules.add(r);
                } else {
                    r = getUnaryRule(parent, child, logProb);
                    unaryRules.add(r);
                }
            } else if (splits.length == 5 && state == GrReaderState.NonTerminalRules) {
                // Binary rule.
                // splits[1] is the separator "-->"
                String parent = splits[0];
                String leftChild = splits[2];
                String rightChild = splits[3];
                double logProb = Double.parseDouble(splits[4]);
                r = getBinaryRule(parent, leftChild, rightChild, logProb);
                binaryRules.add(r);
            } else {
                throw new RuntimeException("Invalid line: " + line);
            }
            allRules.add(r);
        }
        lexAlphabet.stopGrowth();
        ntAlphabet.stopGrowth();
    }

    private Rule getLexicalRule(String parentStr, String childStr, double logProb) {
        int parent = ntAlphabet.lookupIndex(parentStr);
        int child = lexAlphabet.lookupIndex(childStr);
        return new Rule(parent, child, Rule.LEXICAL_RULE, logProb, ntAlphabet, lexAlphabet);
    }

    private Rule getUnaryRule(String parentStr, String childStr, double logProb) {
        int parent = ntAlphabet.lookupIndex(parentStr);
        int child = ntAlphabet.lookupIndex(childStr);
        return new Rule(parent, child, Rule.UNARY_RULE, logProb, ntAlphabet, lexAlphabet);
    }

    private Rule getBinaryRule(String parentStr, String leftChildStr, String rightChildStr, double logProb) {
        int parent = ntAlphabet.lookupIndex(parentStr);
        int leftChild = ntAlphabet.lookupIndex(leftChildStr);
        int rightChild = ntAlphabet.lookupIndex(rightChildStr);
        return new Rule(parent, leftChild, rightChild, logProb, ntAlphabet, lexAlphabet);
    }


}
