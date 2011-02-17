package edu.jhu.hltcoe.parse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.ilp.GurobiIlpSolver;
import edu.jhu.hltcoe.ilp.IlpSolver;
import edu.jhu.hltcoe.model.DmvModel;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.util.Command;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;

public class IlpViterbiParser implements ViterbiParser {

    private static final String ZIMPL_CODE_XML = "/edu/jhu/hltcoe/parse/zimpl_dep_parse.xml";
    private static final String WALL_ID = "$";
    private static final int WALL_POSITION = 0;
    private Map<String,String> codeMap;
    private final Pattern zimplVarRegex = Pattern.compile("[#$]");
    private boolean projective;
    private File workspace;
    
    public IlpViterbiParser(boolean projective) {
        XmlCodeContainerReader reader = new XmlCodeContainerReader();
        reader.loadZimplCodeFromResource(ZIMPL_CODE_XML);
        codeMap = reader.getCodeMap();
        workspace = Command.createTempDir("workspace", new File("."));
    }
    
    @Override
    public DepTree getViterbiParse(Sentence sentence, Model model) {
        // Create workspace
        File tempDir = Command.createTempDir("ilp_parse", workspace);
        
        // Encode sentences and model
        String zimplFile = encode(tempDir, sentence, model);
        
        // Run zimpl
        // Run ILP solver
        IlpSolver ilpSolver = new GurobiIlpSolver(zimplFile);
        ilpSolver.solve();
        Map<String,String> result = ilpSolver.getResult();
        
        // Decode parses
        DepTree tree = decode(sentence, result);
        return tree;
    }

    private String encode(File tempDir, Sentence sentence, Model model) {
        try {
            // Encode sentence
            encodeSentence(tempDir, sentence);
            
            // Encode model 
            //TODO: handle more than just the DMV
            DmvModel dmv = (DmvModel)model;
            encodeDmv(tempDir, dmv);
            
            // Create .zpl file
            File zimplFile = createZimplFile(tempDir);
            
            return zimplFile.getAbsolutePath();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private File createZimplFile(File tempDir) throws FileNotFoundException {
        File zimplFile = new File(tempDir, "parse.zpl");
        PrintWriter zimplWriter;
        zimplWriter = new PrintWriter(zimplFile);
        zimplWriter.write(codeMap.get("setup"));
        zimplWriter.write(codeMap.get("deptree-general"));
        if (projective) {
            zimplWriter.write(codeMap.get("deptree-proj"));
        } else {
            zimplWriter.write(codeMap.get("deptree-nonproj"));
        }
        zimplWriter.write(codeMap.get("dmv-objective"));
        zimplWriter.close();
        return zimplFile;
    }

    private void encodeSentence(File tempDir, Sentence sentence) throws FileNotFoundException {
        File sentFile = new File(tempDir, "input.sent");
        PrintWriter sentWriter = new PrintWriter(sentFile);
        sentWriter.format("0 %d %s\n", WALL_POSITION, WALL_ID);
        for (int i=0; i<sentence.size(); i++) {
            Label label = sentence.get(i); 
            // Must add one to each word position
            sentWriter.format("0 %d \"%s\"\n", i+1, label.getLabel());
        }
        sentWriter.close();
    }

    private void encodeDmv(File tempDir, DmvModel dmv) throws FileNotFoundException {
        File stopWeightsFile = new File(tempDir, "input.stopweights");
        PrintWriter stopWeightsWriter = new PrintWriter(stopWeightsFile);
        Map<Triple<Label,String,Boolean>,Double> stopWeights = dmv.getStopWeights();
        for (Entry<Triple<Label,String,Boolean>,Double> entry : stopWeights.entrySet()) {
            Label label = entry.getKey().get1();
            String leftRight = entry.getKey().get2();
            int adjacent = entry.getKey().get3() ? 1 : 0;
            double weight = entry.getValue();
            stopWeightsWriter.format("\"%s\" %s %d %f\n", label.getLabel(), leftRight, adjacent, weight);
        }
        stopWeightsWriter.close();
        
        File chooseWeightsFile = new File(tempDir, "input.chooseweights");
        PrintWriter chooseWeightsWriter = new PrintWriter(chooseWeightsFile);
        Map<Pair<Label,Label>,Double> chooseWeights = dmv.getChooseWeights();
        for (Entry<Pair<Label,Label>,Double> entry : chooseWeights.entrySet()) {
            Label parent = entry.getKey().get1();
            Label child = entry.getKey().get2();
            double weight = entry.getValue();
            chooseWeightsWriter.format("\"%s\" \"%s\" %f\n", parent.getLabel(), child.getLabel(), weight);
        }
        chooseWeightsWriter.close();
    }
    
    private DepTree decode(Sentence sentence, Map<String, String> result) {
        int[] parents = new int[sentence.size()];
        for (Entry<String,String> entry : result.entrySet()) {
            String zimplVar = entry.getKey();
            String value = entry.getValue();
            String[] splits = zimplVarRegex.split(zimplVar);
            String varType = splits[0];
            if (varType.equals("arc")) {
                // Unused: int sentId = Integer.parseInt(splits[1]);
                int parent = Integer.parseInt(splits[2]);
                int child = Integer.parseInt(splits[3]);
                if (value.equals("1")) {
                    // Must subtract one from each position
                    parents[child-1] = parent-1;
                }
            }
        }
        DepTree tree = new DepTree(sentence, parents);
        return tree;
    }

    public static void main(String[] args) {
        new IlpViterbiParser(true);
    }
}
