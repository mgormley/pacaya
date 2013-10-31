package edu.jhu.parse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.jhu.data.DepTreebank;
import edu.jhu.data.Label;
import edu.jhu.data.SentenceCollection;
import edu.jhu.data.WallDepTreeNode;
import edu.jhu.ilp.IlpSolverFactory;
import edu.jhu.model.Model;
import edu.jhu.model.dmv.DmvModel;
import edu.jhu.model.dmv.DmvModel.Lr;
import edu.jhu.parse.IdentityDeltaGenerator.Delta;
import edu.jhu.util.collections.Maps;
import edu.jhu.util.math.FastMath;

public class IlpDepParserWithDeltas extends IlpDepParser implements DepParser {

    private static final Logger log = Logger.getLogger(IlpDepParserWithDeltas.class);
    private DeltaGenerator deltaGen;
        
    public IlpDepParserWithDeltas(IlpFormulation formulation, IlpSolverFactory ilpSolverFactory, DeltaGenerator deltaGen) {
        super(formulation, ilpSolverFactory);
        this.deltaGen = deltaGen;
    }

    //TODO: consolidate this with IlpViterbiParser (only differences are "-delta")
    @Override
    protected File createZimplFile(File tempDir) throws FileNotFoundException {
        File zimplFile = new File(tempDir, "parse.zpl");
        PrintWriter zimplWriter;
        zimplWriter = new PrintWriter(zimplFile);
        zimplWriter.write(codeMap.getCodeSnippet("setup"));
        zimplWriter.write(codeMap.getCodeSnippet("weights-delta"));
        zimplWriter.write(codeMap.getCodeSnippet("sos-delta"));
        if (formulation != IlpFormulation.MFLOW_NONPROJ && formulation != IlpFormulation.MFLOW_PROJ) {
            zimplWriter.write(codeMap.getCodeSnippet("deptree-general"));
        }
        if (formulation == IlpFormulation.FLOW_PROJ) {
            zimplWriter.write(codeMap.getCodeSnippet(IlpFormulation.FLOW_NONPROJ));
        } else if (formulation == IlpFormulation.MFLOW_PROJ) {
            zimplWriter.write(codeMap.getCodeSnippet(IlpFormulation.MFLOW_NONPROJ));
        }
        zimplWriter.write(codeMap.getCodeSnippet(formulation));
        zimplWriter.write(codeMap.getCodeSnippet("dmv-objective-support"));
        // Here we use the -modelparam version of the objective so that DIP has
        // one variable per model parameter in the objective.
        zimplWriter.write(codeMap.getCodeSnippet("dmv-objective-delta-modelparam"));
        zimplWriter.close();
        return zimplFile;
    }

    @Override
    protected void encodeModel(File tempDir, Model model, SentenceCollection sentences) throws FileNotFoundException {
        //TODO: handle more than just the DMV
        DmvModel dmv = (DmvModel)model;
        encodeDmvWithDeltas(tempDir, dmv);
    }

    private void encodeDmvWithDeltas(File tempDir, DmvModel dmv) throws FileNotFoundException {
        encodeStopWeights(tempDir, dmv);
        encodeChooseWeightsWithDeltas(tempDir, dmv);
    }
    
    private void encodeChooseWeightsWithDeltas(File tempDir, DmvModel dmv) throws FileNotFoundException {        
        File chooseWeightsFile = new File(tempDir, "input.chooseweights.delta");
        PrintWriter chooseWeightsWriter = new PrintWriter(chooseWeightsFile);
        // Root
        for (Label child : dmv.getVocab()) {
            double origWeight = FastMath.exp(dmv.getRootWeight(child));
            for (Delta delta : deltaGen.getDeltas(origWeight)) {
                String deltaId = delta.getId();
                double weight = delta.getWeight();
                double logWeight = FastMath.logForIlp(weight);
                chooseWeightsWriter.format("\"%s\" \"%s\" \"%s\" \"%s\" %.13E %.13E\n", 
                        WallDepTreeNode.WALL_LABEL.getLabel(), Lr.RIGHT.toString(), child.getLabel(), deltaId, weight, logWeight);
                // TODO: Remove this once we've updated the zimpl code never use these weights. This is a dummy weight. 
                chooseWeightsWriter.format("\"%s\" \"%s\" \"%s\" \"%s\" %.13E %.13E\n", 
                        WallDepTreeNode.WALL_LABEL.getLabel(), Lr.LEFT.toString(), child.getLabel(), deltaId, weight, logWeight);
            }
        }
        // Child
        for (Label child : dmv.getVocab()) {
            for (Label parent : dmv.getVocab()) {
                for (Lr lr : Lr.values()) {
                    double origWeight = FastMath.exp(dmv.getChildWeight(parent, lr, child));
                    for (Delta delta : deltaGen.getDeltas(origWeight)) {
                        String deltaId = delta.getId();
                        double weight = delta.getWeight();
                        double logWeight = FastMath.logForIlp(weight);
                        chooseWeightsWriter.format("\"%s\" \"%s\" \"%s\" \"%s\" %.13E %.13E\n", 
                                parent.getLabel(), lr, child.getLabel(), deltaId, weight, logWeight);
                    }
                }
            }
        }
        chooseWeightsWriter.close();
    }
    
    @Override
    protected DepTreebank decode(SentenceCollection sentences, Map<String,Double> result) {
        // Print stats about deltas used
        Map<String, Integer> cwDeltaCounts = new HashMap<String, Integer>();

        for (Entry<String,Double> entry : result.entrySet()) {
            String zimplVar = entry.getKey();
            Double value = entry.getValue();
            String[] splits = zimplVarRegex.split(zimplVar);
            String varType = splits[0];
            if (varType.equals("cwDelta")) {
                // Note: splitting to get the parent/child words with the 
                // above regex doesn't work because some of the POS tags
                // contain the $. Currently, this doesn't matter because we
                // don't need the words, but if that changes then we'll need
                // a better solution here.
                
                // cwDelta[parentw,lr,childw,d]
                //String parentWord = splits[1];
                //String lr = splits[2];
                //String childWord = splits[3];
                
                String deltaId = splits[splits.length-1];
                long longVal = Math.round(value);
                if (longVal == 1) {
                    Maps.increment(cwDeltaCounts, deltaId, 1);
                }
            }
        }
        
        log.debug("Delta Histogram:");
        for (String deltaId : cwDeltaCounts.keySet()) {
            // TODO: fix problem with $WALL$ which has the $ split character
            log.debug("\t" + deltaId + " " + cwDeltaCounts.get(deltaId));
        }
        
        // Decode
        return super.decode(sentences, result);
    }
    
}
