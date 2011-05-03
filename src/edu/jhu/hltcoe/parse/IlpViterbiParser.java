package edu.jhu.hltcoe.parse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.ilp.ClGurobiIlpSolver;
import edu.jhu.hltcoe.ilp.ZimplSolver;
import edu.jhu.hltcoe.model.DmvModel;
import edu.jhu.hltcoe.model.DmvModelFactory;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.DmvModelFactory.WeightGenerator;
import edu.jhu.hltcoe.util.Command;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;

public class IlpViterbiParser implements ViterbiParser {

    private static Logger log = Logger.getLogger(IlpViterbiParser.class);
    
    private static final String ZIMPL_CODE_XML = "/edu/jhu/hltcoe/parse/zimpl_dep_parse.xml";
    private static final int ZIMPL_WALL_POSITION = 0;
    private Map<String,String> codeMap;
    private final Pattern zimplVarRegex = Pattern.compile("[#$]");
    private IlpFormulation formulation;
    private File workspace;
    
    public enum IlpFormulation {
        DP_PROJ("deptree-dp-proj", true),
        EXPLICIT_PROJ("deptree-explicit-proj", true),
        FLOW_NONPROJ("deptree-flow-nonproj", false),
        FLOW_PROJ("deptree-flow-proj", true),
        MFLOW_NONPROJ("deptree-multiflow-nonproj", false),
        MFLOW_PROJ("deptree-multiflow-proj", true);
        
        private String id;
        private boolean isProjective;
        
        private IlpFormulation(String id, boolean isProjective) {
            this.id = id;
            this.isProjective = isProjective;
        }
        
        @Override
        public String toString() {
            return id;
        }
        
        public static IlpFormulation getById(String id) {
            for (IlpFormulation f : values()) {
                if (f.id.equals(id)) {
                    return f;
                }
            }
            throw new IllegalArgumentException("Unrecognized IlpFormulation id: " + id);
        }
        
        public boolean isProjective() {
            return isProjective;
        }
    }
    
    public IlpViterbiParser(IlpFormulation formulation) {
        this.formulation = formulation;
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
        File zimplFile = encode(tempDir, sentence, model);
        
        // Run zimpl and then ILP solver
        ZimplSolver solver = new ZimplSolver(tempDir, new ClGurobiIlpSolver(tempDir));
        solver.solve(zimplFile);
        Map<String,Double> result = solver.getResult();
        
        // Decode parses
        DepTree tree = decode(sentence, result);
        return tree;
    }

    private File encode(File tempDir, Sentence sentence, Model model) {
        try {
            SentenceCollection sentences = new SentenceCollection();
            sentences.add(sentence);
            
            // Encode sentence
            encodeSentences(tempDir, sentences);
            
            // Encode model 
            //TODO: handle more than just the DMV
            DmvModel dmv = (DmvModel)model;
            WeightCopier weightCopier = new WeightCopier(dmv);
            DmvModel filteredDmv = (DmvModel)(new DmvModelFactory(weightCopier)).getInstance(sentences);
            encodeDmv(tempDir, filteredDmv);
            
            // Create .zpl file
            File zimplFile = createZimplFile(tempDir);
            
            return zimplFile;

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private File createZimplFile(File tempDir) throws FileNotFoundException {
        File zimplFile = new File(tempDir, "parse.zpl");
        PrintWriter zimplWriter;
        zimplWriter = new PrintWriter(zimplFile);
        zimplWriter.write(getCodeSnippet("setup"));
        if (formulation != IlpFormulation.MFLOW_NONPROJ && formulation != IlpFormulation.MFLOW_PROJ) {
            zimplWriter.write(getCodeSnippet("deptree-general"));
        }
        if (formulation == IlpFormulation.FLOW_PROJ) {
            zimplWriter.write(getCodeSnippet(IlpFormulation.FLOW_NONPROJ));
        } else if (formulation == IlpFormulation.MFLOW_PROJ) {
            zimplWriter.write(getCodeSnippet(IlpFormulation.MFLOW_NONPROJ));
        }
        zimplWriter.write(getCodeSnippet(formulation));
        zimplWriter.write(getCodeSnippet("dmv-objective"));
        zimplWriter.close();
        return zimplFile;
    }
    
    private String getCodeSnippet(Object id) {
        if (id instanceof IlpFormulation) {
            return codeMap.get(id.toString());
        } 
        return codeMap.get(id);
    }

    private void encodeSentences(File tempDir, SentenceCollection sentences) throws FileNotFoundException {
        File sentFile = new File(tempDir, "input.sent");
        PrintWriter sentWriter = new PrintWriter(sentFile);
        sentWriter.format("0 %d %s\n", ZIMPL_WALL_POSITION, WallDepTreeNode.WALL_ID);
        for(int s=0; s<sentences.size(); s++) {
            Sentence sentence = sentences.get(s);
            for (int i=0; i<sentence.size(); i++) {
                Label label = sentence.get(i); 
                // Must add one to each word position
                sentWriter.format("%d %d \"%s\"\n", s, i+1, label.getLabel());
            }
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
    
    private DepTree decode(Sentence sentence, Map<String,Double> result) {
        int[] parents = new int[sentence.size()];
        for (Entry<String,Double> entry : result.entrySet()) {
            String zimplVar = entry.getKey();
            Double value = entry.getValue();
            String[] splits = zimplVarRegex.split(zimplVar);
            String varType = splits[0];
            if (varType.equals("arc")) {
                // Unused: int sentId = Integer.parseInt(splits[1]);
                int parent = Integer.parseInt(splits[2]);
                int child = Integer.parseInt(splits[3]);
                long longVal = Math.round(value);
                if (longVal == 1) {
                    // Must subtract one from each position
                    parents[child-1] = parent-1;
                }
            }
        }
        DepTree tree = new DepTree(sentence, parents, formulation.isProjective());
        return tree;
    }

    public static void main(String[] args) {
        new IlpViterbiParser(IlpFormulation.DP_PROJ);
    }
    
    private static class WeightCopier implements WeightGenerator {

        private DmvModel dmv;
        
        public WeightCopier(DmvModel dmv) {
            this.dmv = dmv;
        }

        @Override
        public double[] getChooseMulti(Label parent, List<Label> children) {
            Map<Pair<Label, Label>, Double> cw_map = dmv.getChooseWeights();
            double[] mult = new double[children.size()];
            for (int i=0; i<mult.length; i++) {
                mult[i] = cw_map.get(new Pair<Label,Label>(parent, children.get(i)));
            }
            // Do NOT normalize the multinomial
            return mult;
        }

        @Override
        public double getStopWeight(Triple<Label, String, Boolean> triple) {
            return dmv.getStopWeights().get(triple);
        }
        
    }
}
