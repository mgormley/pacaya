package edu.jhu.hltcoe.parse.relax;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.io.File;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDepTreebank;
import edu.jhu.hltcoe.lp.CplexPrm;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.relax.DmvParseLpBuilder.DmvParsingProgram;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.cplex.CplexUtils;

public class LpDmvRelaxedParser implements RelaxedParser {

    static Logger log = Logger.getLogger(LpDmvRelaxedParser.class);

    private IloCplex cplex;
    private File tempDir;
    private CplexPrm cplexFactory;
    private IlpFormulation formulation;
    private double lastParseWeight;

    public LpDmvRelaxedParser(CplexPrm cplexFactory, IlpFormulation formulation) {
        this.cplexFactory = cplexFactory;
        this.formulation = formulation;
    }

    @Override
    public RelaxedDepTreebank getRelaxedParse(DmvTrainCorpus corpus, Model model) {
        DmvModel dmv = (DmvModel) model;
        try {
            this.cplex = cplexFactory.getIloCplexInstance();
            DmvParseLpBuilder builder = new DmvParseLpBuilder(cplex, formulation);

            DmvParsingProgram pp = builder.buildParsingProgram(corpus, dmv);

            cplex.add(pp.mat);
            cplex.add(pp.obj);

            if (tempDir != null) {
                cplex.exportModel(new File(tempDir, "lpParser.lp").getAbsolutePath());
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

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

}
