package edu.jhu.hltcoe.parse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.ZimplSolver;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModel.ChooseRhs;
import edu.jhu.hltcoe.model.dmv.DmvModel.StopRhs;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.DelayedDeleter;
import edu.jhu.hltcoe.util.Files;
import edu.jhu.hltcoe.util.Time;
import edu.jhu.hltcoe.util.Utilities;

public class IlpViterbiParser implements ViterbiParser {

    private static Logger log = Logger.getLogger(IlpViterbiParser.class);
    
    public static final String ZIMPL_CODE_XML = "/edu/jhu/hltcoe/parse/zimpl_dep_parse.xml";
    private static final int ZIMPL_WALL_POSITION = 0;
    protected ZimplXmlCodeContainerReader codeMap;
    public static final Pattern zimplVarRegex = Pattern.compile("[#$]");
    protected IlpFormulation formulation;
    protected File workspace;
    private DelayedDeleter deleter;
    protected IlpSolverFactory ilpSolverFactory;
    protected double parseWeight;
    
    public IlpViterbiParser(IlpFormulation formulation, IlpSolverFactory ilpSolverFactory) {
        this.formulation = formulation;
        this.ilpSolverFactory = ilpSolverFactory;
        codeMap = new ZimplXmlCodeContainerReader(formulation);
        codeMap.loadZimplCodeFromResource(ZIMPL_CODE_XML);
        workspace = Files.createTempDir("workspace", new File("."));
        deleter = new DelayedDeleter(Integer.MAX_VALUE);
    }

    @Override
    public DepTreebank getViterbiParse(DmvTrainCorpus corpus, Model genericModel) {
        if (corpus.getNumLabeled() > 0) {
            throw new RuntimeException("not implemented");
        } else {
            return getViterbiParse(corpus.getSentences(), genericModel);
        }
    }
    
    @Override
    public DepTreebank getViterbiParse(SentenceCollection sentences, Model model) {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        
        // Encode the model/sentences as an ILP and solve
        Map<String, Double> result = solve(sentences, model);
        
        // Decode parses
        DepTreebank depTreebank = decode(sentences, result);
        
        stopwatch.stop();
        log.debug(String.format("Avg parse time: %.3f", 
                Time.totMs(stopwatch) / sentences.size()));
        log.debug(String.format("Tot parse time: %.3f", 
                Time.totMs(stopwatch)));
        
        return depTreebank;
    }

    /**
     * Encode the model/sentences as an ILP and solve.
     * 
     * Side effect: stores the parseWeight
     * 
     * @return an optimal ILP solution
     */
    protected Map<String, Double> solve(SentenceCollection sentences, Model model) {
        // Create workspace
        File tempDir = Files.createTempDir("ilp_parse", workspace);
        
        // Encode sentences and model
        File zimplFile = encode(tempDir, sentences, model);
        
        // Run zimpl and then ILP solver
        ZimplSolver solver = getZimplSolver(tempDir);
        solver.solve(zimplFile);
        Map<String,Double> result = solver.getResult();
        parseWeight = solver.getObjective();
        
        deleter.delayedDelete(tempDir);
        return result;
    }

    /**
     * Extracted method for override in InitializedIlpViterbiParserWithDeltas
     */
    protected ZimplSolver getZimplSolver(File tempDir) {
        ZimplSolver solver = new ZimplSolver(tempDir, ilpSolverFactory.getInstance(tempDir));
        return solver;
    }
    
    public double getLastParseWeight() {
        return parseWeight;
    }
    
    public File getWorkspace() {
        return workspace;
    }

    protected File encode(File tempDir, SentenceCollection sentences, Model model) {
        try {
            // Encode sentence
            encodeSentences(tempDir, sentences);
            
            // Encode model 
            encodeModel(tempDir, model, sentences);
            
            // Create .zpl file
            File zimplFile = createZimplFile(tempDir);
            
            return zimplFile;

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected File createZimplFile(File tempDir) throws FileNotFoundException {
        File zimplFile = new File(tempDir, "parse.zpl");
        PrintWriter zimplWriter;
        zimplWriter = new PrintWriter(zimplFile);
        zimplWriter.write(codeMap.getCodeSnippet("setup"));
        zimplWriter.write(codeMap.getCodeSnippet("weights"));
        if (formulation != IlpFormulation.MFLOW_NONPROJ && formulation != IlpFormulation.MFLOW_PROJ) {
            zimplWriter.write(codeMap.getCodeSnippet("deptree-general"));
        }
        if (formulation == IlpFormulation.FLOW_PROJ) {
            zimplWriter.write(codeMap.getCodeSnippet(IlpFormulation.FLOW_NONPROJ));
        } else if (formulation == IlpFormulation.MFLOW_PROJ) {
            zimplWriter.write(codeMap.getCodeSnippet(IlpFormulation.MFLOW_NONPROJ));
        }
        String formulationId = formulation.toString().replace("-lprelax","");
        zimplWriter.write(codeMap.getCodeSnippet(formulationId));
        zimplWriter.write(codeMap.getCodeSnippet("dmv-objective-support"));
        // The -modelparam version is slightly slower but is correct on zero stop probabilities
        zimplWriter.write(codeMap.getCodeSnippet("dmv-objective-modelparam"));
        zimplWriter.close();
        return zimplFile;
    }

    private void encodeSentences(File tempDir, SentenceCollection sentences) throws FileNotFoundException {
        File sentFile = new File(tempDir, "input.sent");
        PrintWriter sentWriter = new PrintWriter(sentFile);
        for(int s=0; s<sentences.size(); s++) {
            Sentence sentence = sentences.get(s);
            sentWriter.format("%d %d %s\n", s, ZIMPL_WALL_POSITION, WallDepTreeNode.WALL_ID);
            for (int i=0; i<sentence.size(); i++) {
                Label label = sentence.get(i); 
                // Must add one to each word position
                sentWriter.format("%d %d \"%s\"\n", s, i+1, label.getLabel());
            }
        }
        sentWriter.close();
    }

    protected void encodeModel(File tempDir, Model model, SentenceCollection sentences) throws FileNotFoundException {
        //TODO: handle more than just the DMV
        DmvModel dmv = (DmvModel)model;
        encodeDmv(tempDir, dmv);
    }

    protected void encodeDmv(File tempDir, DmvModel dmv) throws FileNotFoundException {
        encodeStopWeights(tempDir, dmv);
        encodeChooseWeights(tempDir, dmv);
    }

    protected void encodeStopWeights(File tempDir, DmvModel dmv) throws FileNotFoundException {
        File stopWeightsFile = new File(tempDir, "input.stopweights");
        PrintWriter stopWeightsWriter = new PrintWriter(stopWeightsFile);
        Map<StopRhs,Double> stopWeights = dmv.getStopWeights();
        for (Entry<StopRhs,Double> entry : stopWeights.entrySet()) {
            Label label = entry.getKey().get1();
            String leftRight = entry.getKey().get2();
            int adjacent = entry.getKey().get3() ? 1 : 0;
            double weight = entry.getValue();
            double logWeightStop = Utilities.logForIlp(weight);
            double logWeightNotStop = Utilities.logForIlp(1.0 - weight);
            stopWeightsWriter.format("\"%s\" %s %d %.13E %.13E %.13E\n", label.getLabel(), leftRight, adjacent, weight, logWeightStop, logWeightNotStop);
        }
        stopWeightsWriter.close();
    }
        
    private void encodeChooseWeights(File tempDir, DmvModel dmv) throws FileNotFoundException {
        File chooseWeightsFile = new File(tempDir, "input.chooseweights");
        PrintWriter chooseWeightsWriter = new PrintWriter(chooseWeightsFile);
        Map<ChooseRhs, LabeledMultinomial<Label>> chooseWeights = dmv.getChooseWeights();
        for (Entry<ChooseRhs, LabeledMultinomial<Label>> entry : chooseWeights.entrySet()) {
            Label parent = entry.getKey().get1();
            String lr = entry.getKey().get2();
            for (Entry<Label,Double> subEntry : entry.getValue().entrySet()) {
                Label child = subEntry.getKey();
                double weight = subEntry.getValue();
                double logWeight = Utilities.logForIlp(weight);
                chooseWeightsWriter.format("\"%s\" \"%s\" \"%s\" %.13E %.13E\n", parent.getLabel(), lr, child.getLabel(), weight, logWeight);
            }
        }
        chooseWeightsWriter.close();
    }

    protected DepTreebank decode(SentenceCollection sentences, Map<String,Double> result) {
        DepTreebank depTreebank = new DepTreebank();
        
        int[][] parents = new int[sentences.size()][];
        for (int i=0; i<sentences.size(); i++) {
            parents[i] = new int[sentences.get(i).size()];
            Arrays.fill(parents[i], DepTree.EMPTY_POSITION);
        }
        
        for (Entry<String,Double> entry : result.entrySet()) {
            String zimplVar = entry.getKey();
            Double value = entry.getValue();
            String[] splits = zimplVarRegex.split(zimplVar);
            String varType = splits[0];
            if (varType.equals("arc")) {
                int sentId = Integer.parseInt(splits[1]);
                int parent = Integer.parseInt(splits[2]);
                int child = Integer.parseInt(splits[3]);
                long longVal = Math.round(value);
                if (longVal == 1) {
                    // Must subtract one from each position
                    parents[sentId][child-1] = parent-1;
                }
            }
        }
        
        for (int i=0; i<sentences.size(); i++) {
            DepTree tree = new DepTree(sentences.get(i), parents[i], formulation.isProjective());
            depTreebank.add(tree);
        }
        
        return depTreebank;
    }
    
}
