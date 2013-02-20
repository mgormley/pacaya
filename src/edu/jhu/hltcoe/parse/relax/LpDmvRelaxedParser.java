package edu.jhu.hltcoe.parse.relax;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.io.File;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDepTreebank;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.parse.relax.DmvParseLpBuilder.DmvParseLpBuilderPrm;
import edu.jhu.hltcoe.parse.relax.DmvParseLpBuilder.DmvParsingProgram;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.cplex.CplexPrm;
import edu.jhu.hltcoe.util.cplex.CplexUtils;

public class LpDmvRelaxedParser implements RelaxedParser {

    public static class LpDmvRelaxedParserPrm {
        public File tempDir = null;
        public CplexPrm cplexPrm = new CplexPrm();
        public DmvParseLpBuilderPrm parsePrm = new DmvParseLpBuilderPrm();
    }
    
    private static Logger log = Logger.getLogger(LpDmvRelaxedParser.class);

    private LpDmvRelaxedParserPrm prm;
    private IloCplex cplex;
    private double lastParseWeight;
        
    public LpDmvRelaxedParser(LpDmvRelaxedParserPrm prm) {
        this.prm = prm;
    }

    @Override
    public RelaxedDepTreebank getRelaxedParse(DmvTrainCorpus corpus, Model model) {
        DmvModel dmv = (DmvModel) model;
        try {
            this.cplex = prm.cplexPrm.getIloCplexInstance();
            DmvParseLpBuilder builder = new DmvParseLpBuilder(prm.parsePrm, cplex);

            DmvParsingProgram pp = builder.buildParsingProgram(corpus, dmv);

            cplex.add(pp.mat);
            cplex.add(pp.obj);

            if (prm.tempDir != null) {
                cplex.exportModel(new File(prm.tempDir, "lpParser.lp").getAbsolutePath());
            }
            if (!cplex.solve()) {
                throw new RuntimeException("unable to parse");
            }

            lastParseWeight = cplex.getObjValue();
            return extractSolution(corpus, pp);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    private RelaxedDepTreebank extractSolution(DmvTrainCorpus corpus, DmvParsingProgram pp) throws IloException {
        RelaxedDepTreebank relaxTreebank = new RelaxedDepTreebank(corpus);
        relaxTreebank.setFracRoots(CplexUtils.getValues(cplex, pp.arcRoot));
        relaxTreebank.setFracChildren(CplexUtils.getValues(cplex, pp.arcChild));
        return relaxTreebank;
    }

    @Override
    public double getLastParseWeight() {
        return lastParseWeight;
    }

}
