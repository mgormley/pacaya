package edu.jhu.hltcoe.parse.relax;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDepTreebank;
import edu.jhu.hltcoe.lp.CplexFactory;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.RelaxedParser;
import edu.jhu.hltcoe.train.DmvTrainCorpus;

public class LpDmvRelaxedParser implements RelaxedParser {

    static Logger log = Logger.getLogger(LpDmvRelaxedParser.class);

    private IloCplex cplex;
    private File tempDir;
    private CplexFactory cplexFactory;
    private IlpFormulation formulation;
    private double lastParseWeight;

    public LpDmvRelaxedParser(CplexFactory cplexFactory, IlpFormulation formulation) {
        this.cplexFactory = cplexFactory;
        if (!formulation.isLpRelaxation()) {
            throw new IllegalStateException("must be LP relaxation");
        }
        this.formulation = formulation;
    }

    @Override
    public RelaxedDepTreebank getRelaxedParse(DmvTrainCorpus corpus, Model model) {
        try {
            this.cplex = cplexFactory.getInstance();
            buildProgram(corpus, model);

            if (tempDir != null) {
                cplex.exportModel(new File(tempDir, "lpParser.lp").getAbsolutePath());
            }
            cplex.solve();

            lastParseWeight = cplex.getObjValue();
            RelaxedDepTreebank relaxTreebank = extractSolution();
            return relaxTreebank;
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ParsingProblem {
        // --- Variables ---

        // Arc variables: root and child.
        IloNumVar[][] arcRoot;
        IloNumVar[][][] arcChild;
        // Single commodity flow variables.
        IloNumVar[][] flowRoot;
        IloNumVar[][][] flowChild;
        // The DMV Objective support variables.
        IloNumVar[][][] numToSide;
        IloNumVar[][][] genAdj;
        IloNumVar[][][] numNonAdj;
        
        // --- Constraints ---
        
        IloRange[] oneArcPerWall;
        
    }
    
    private void buildProgram(DmvTrainCorpus corpus, Model model) throws IloException {
        ParsingProblem pp = new ParsingProblem();
        
        // Construct the arc and flow variables.
        pp.arcRoot = new IloNumVar[corpus.size()][];
        pp.arcChild = new IloNumVar[corpus.size()][][];
        pp.flowRoot = new IloNumVar[corpus.size()][];
        pp.flowChild = new IloNumVar[corpus.size()][][];
        for (int s=0; s<corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            pp.arcRoot[s] = new IloNumVar[sent.size()];
            pp.arcChild[s] = new IloNumVar[sent.size()][sent.size()];
            pp.flowRoot[s] = new IloNumVar[sent.size()];
            pp.flowChild[s] = new IloNumVar[sent.size()][sent.size()];
            for (int c = 0; c < sent.size(); c++) {
                pp.arcRoot[s][c] = cplex.numVar(0, 1, String.format("arcChild_{%d,%d}", s, c));
                pp.flowRoot[s][c] = cplex.numVar(0, sent.size(), String.format("flowChild_{%d,%d}", s, c));
            }
            for (int p = 0; p < sent.size(); p++) {
                for (int c = 0; c < sent.size(); c++) {
                    pp.arcChild[s][p][c] = cplex.numVar(0, 1, String.format("arcChild_{%d,%d,%d}", s, p, c));
                    pp.flowChild[s][p][c] = cplex.numVar(0, sent.size(), String.format("flowChild_{%d,%d,%d}", s, p, c));
                }
            }
        }
                
        // Construct the DMV Objective support variables.
        pp.numToSide = new IloNumVar[corpus.size()][][];
        pp.genAdj = new IloNumVar[corpus.size()][][];
        pp.numNonAdj = new IloNumVar[corpus.size()][][];
        for (int s=0; s<corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            pp.numToSide[s] = new IloNumVar[sent.size()][2];
            pp.genAdj[s] = new IloNumVar[sent.size()][2];
            pp.numNonAdj[s] = new IloNumVar[sent.size()][2];
            for (int i = 0; i < sent.size(); i++) {
                for (int lr = 0; lr < sent.size(); lr++) {
                    pp.numToSide[s][i][lr] = cplex.numVar(0, sent.size(), String.format("numToSide_{%d,%d,%d}", s, i, lr));
                    pp.genAdj[s][i][lr] = cplex.numVar(0, 1, String.format("genAdj_{%d,%d,%d}", s, i, lr));
                    pp.numNonAdj[s][i][lr] = cplex.numVar(0, sent.size() - 1, String.format("numNonAdj_{%d,%d,%d}", s, i, lr));
                }
            }
        }
        
        // Add extra tree constraints.
        
        //        # This constraint is optional, but we include it for all formulations    
        //        # The wall has one outgoing arc
        //        subto one_child_for_wall:
        //            forall <s> in Sents:
        //               1 == sum <j> in { 1 to Length[s] }: arc[s,0,j];
        pp.oneArcPerWall = new IloRange[corpus.size()];
        for (int s=0; s<corpus.size(); s++) {
            double[] ones = new double[pp.arcRoot[s].length];
            Arrays.fill(ones, 1.0);
            IloLinearNumExpr expr = cplex.scalProd(ones, pp.arcRoot[s]);
            pp.oneArcPerWall[s] = cplex.eq(expr, 1.0);
        }
        
        //        # Other tree constraints
        //        # Each node should have a parent (except the wall)
        //        subto one_incoming_arc:
        //            forall <s> in Sents:
        //                forall <j> in { 1 to Length[s] }:
        //                sum <i> in { 0 to Length[s] } with i != j: arc[s,i,j] == 1;

        
        // Add flow constraints.
        
        //    # ==================================================
        //    # ==== Option 2: Non-projective parsing ====
        //    # Flow Constraints for: B is connected
        //    var flow[<s,i,j> in AllArcs] real >= 0 <= Length[s]; # No priority because this is a real
        //
        //    subto flow_sum: 
        //        forall <s> in Sents:
        //            Length[s] == sum <j> in { 1 to Length[s] }: flow[s,0,j];
        //
        //    subto flow_diff: 
        //        forall <s> in Sents:
        //            forall <i> in { 1 to Length[s] }:
        //            1 == (sum <j> in {0 to Length[s] } with i != j: flow[s,j,i])
        //                 - (sum <j> in { 0 to Length[s] } with i != j: flow[s,i,j]);
        //
        //    subto flow_bound:
        //        forall <s,i,j> in AllArcs:
        //            flow[s,i,j] <= Length[s] * arc[s,i,j];
        //    # ==================================================


        // Add projectivity constraint.

        //    # ==================================================
        //    # ==== Option 4: Projective parsing (also requires constraints from Option 2) ====
        //    # This constraint ensures that descendents of Word[s,i] are not parents of nodes outside the range [i,j]
        //    # and that the parents of those nodes are not outside the range[i,j]
        //    subto proj_parse_no_illegal_parents:
        //        forall <s,i,j> in AllArcs with abs(i-j) > 1:
        //            (sum <k,l> in Arcs[s] with (k > min(i,j) and k < max(i,j)) and (l < min(i,j) or l > max(i,j)): (arc[s,k,l] + arc[s,l,k])) <= Length[s] * (1 - arc[s,i,j]); 
        //    # ==================================================
        
        // Add DMV objective support constraints.
        
        // Add DMV objective.
    }

    private RelaxedDepTreebank extractSolution() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getLastParseWeight() {
        return lastParseWeight;
    }

}
