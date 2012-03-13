package edu.jhu.hltcoe.parse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.util.Quadruple;
import edu.jhu.hltcoe.util.Utilities;

public class IlpViterbiParserWithDeltas extends IlpViterbiParser implements ViterbiParser {

    private static Logger log = Logger.getLogger(IlpViterbiParserWithDeltas.class);
    private DeltaGenerator deltaGen;
        
    public IlpViterbiParserWithDeltas(IlpFormulation formulation, IlpSolverFactory ilpSolverFactory, DeltaGenerator deltaGen) {
        super(formulation, ilpSolverFactory);
        this.deltaGen = deltaGen;
    }

    //TODO: consolidate this with IlpViterbiParser (only differences are "-delta")
    @Override
    protected File createZimplFile(File tempDir) throws FileNotFoundException {
        File zimplFile = new File(tempDir, "parse.zpl");
        PrintWriter zimplWriter;
        zimplWriter = new PrintWriter(zimplFile);
        zimplWriter.write(getCodeSnippet("setup"));
        zimplWriter.write(getCodeSnippet("weights-delta"));
        zimplWriter.write(getCodeSnippet("sos-delta"));
        if (formulation != IlpFormulation.MFLOW_NONPROJ && formulation != IlpFormulation.MFLOW_PROJ) {
            zimplWriter.write(getCodeSnippet("deptree-general"));
        }
        if (formulation == IlpFormulation.FLOW_PROJ) {
            zimplWriter.write(getCodeSnippet(IlpFormulation.FLOW_NONPROJ));
        } else if (formulation == IlpFormulation.MFLOW_PROJ) {
            zimplWriter.write(getCodeSnippet(IlpFormulation.MFLOW_NONPROJ));
        }
        zimplWriter.write(getCodeSnippet(formulation));
        zimplWriter.write(getCodeSnippet("dmv-objective-support"));
        // Here we use the -modelparam version of the objective so that DIP has
        // one variable per model parameter in the objective.
        zimplWriter.write(getCodeSnippet("dmv-objective-delta-modelparam"));
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
        Map<Quadruple<Label,String,Label,String>,Double> chooseWeights = deltaGen.getCWDeltas(dmv);
        for (Entry<Quadruple<Label,String,Label,String>,Double> entry : chooseWeights.entrySet()) {
            Label parent = entry.getKey().get1();
            String lr = entry.getKey().get2();
            Label child = entry.getKey().get3();
            String deltaId = entry.getKey().get4();
            double weight = entry.getValue();
            double logWeight = logForIlp(weight);
            chooseWeightsWriter.format("\"%s\" \"%s\" \"%s\" \"%s\" %E %E\n", parent.getLabel(), lr, child.getLabel(), deltaId, weight, logWeight);
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
                    Utilities.increment(cwDeltaCounts, deltaId, 1);
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
