package edu.jhu.hltcoe.parse;

import java.io.File;
import java.util.Map;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.ilp.CopyingMstFileUpdater;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.ZimplSolver;
import edu.jhu.hltcoe.model.Model;

public class InitializedIlpDepParserWithDeltas extends IlpDepParserWithDeltas implements DepParser {
    
    private IlpDepParserWithDeltas initParser;
    private Map<String,Double> mipStart;
    
    public InitializedIlpDepParserWithDeltas(IlpFormulation formulation, IlpSolverFactory ilpSolverFactory, DeltaGenerator deltaGen, IlpSolverFactory initIlpSolverFactory) {
        // TODO: this should be initialized by a fast dynamic programming parser 
        super(formulation, ilpSolverFactory, deltaGen);
        initParser = new IlpDepParserWithDeltas(formulation, initIlpSolverFactory, new IdentityDeltaGenerator());
    }

    @Override
    public DepTreebank getViterbiParse(SentenceCollection sentences, Model model) {
        mipStart = initParser.solve(sentences, model);
        return super.getViterbiParse(sentences, model);
    }
    
    @Override
    protected ZimplSolver getZimplSolver(File tempDir) {
        ZimplSolver solver = super.getZimplSolver(tempDir);
        solver.setMstFileUpdater(new CopyingMstFileUpdater(mipStart));
        return solver;
    }
    
    public File getInitWorkspace() {
        return initParser.getWorkspace();
    }
        
}
