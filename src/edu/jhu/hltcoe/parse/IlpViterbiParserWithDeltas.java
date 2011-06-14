package edu.jhu.hltcoe.parse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.DmvModel;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.util.Quadruple;

public class IlpViterbiParserWithDeltas extends IlpViterbiParser implements ViterbiParser {

    private static Logger log = Logger.getLogger(IlpViterbiParserWithDeltas.class);
    private DeltaGenerator deltaGen;
        
    public IlpViterbiParserWithDeltas(IlpFormulation formulation, int numThreads, DeltaGenerator deltaGen) {
        super(formulation, numThreads);
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
        zimplWriter.write(getCodeSnippet("dmv-objective-delta"));
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
        Map<Quadruple<Label,String,Label,String>,Double> chooseWeights = deltaGen.getCWDeltas(dmv.getChooseWeights());
        for (Entry<Quadruple<Label,String,Label,String>,Double> entry : chooseWeights.entrySet()) {
            Label parent = entry.getKey().get1();
            String lr = entry.getKey().get2();
            Label child = entry.getKey().get3();
            String deltaId = entry.getKey().get4();
            double weight = entry.getValue();
            chooseWeightsWriter.format("\"%s\" \"%s\" \"%s\" \"%s\" %g\n", parent.getLabel(), lr, child.getLabel(), deltaId, weight);
        }
        chooseWeightsWriter.close();
    }
    
}
