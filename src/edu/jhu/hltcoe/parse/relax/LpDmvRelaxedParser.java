package edu.jhu.hltcoe.parse.relax;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDepTreebank;
import edu.jhu.hltcoe.lp.CplexFactory;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.RelaxedParser;
import edu.jhu.hltcoe.parse.cky.DepSentenceDist;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.CplexUtils;

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
        } else if (formulation != IlpFormulation.FLOW_NONPROJ_LPRELAX
                && formulation != IlpFormulation.FLOW_PROJ_LPRELAX) {
            throw new IllegalStateException("Formulation not implemented: " + formulation);
        }
        this.formulation = formulation;
    }

    @Override
    public RelaxedDepTreebank getRelaxedParse(DmvTrainCorpus corpus, Model model) {
        DmvModel dmv = (DmvModel) model;
        try {
            this.cplex = cplexFactory.getInstance();
            ParsingProblem pp = buildProgram(corpus, dmv);

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
        IloNumVar[][][] stopAdj;
        IloNumVar[][][] numNonAdj;

        // --- Constraints ---

        IloRange[] oneArcPerWall;
        public IloRange[][] oneParent;
        public IloRange[] rootFlowIsSentLength;
        public IloRange[][] flowDiff;
        public IloRange[][] flowBoundRoot;
        public IloRange[][][] flowBoundChild;
        public IloRange[][] projectiveRoot;
        public IloRange[][][] projectiveChild;
        public IloRange[][][] numToSideCons;
        public IloRange[][][] genAdjCons;
        public IloRange[][][] numNonAdjCons;
        public IloLPMatrix mat;
        public IloObjective obj;
        public IloRange[][][] stopAdjCons;

    }

    private ParsingProblem buildProgram(DmvTrainCorpus corpus, DmvModel model) throws IloException {
        ParsingProblem pp = new ParsingProblem();

        // Construct the arc and flow variables.
        pp.arcRoot = new IloNumVar[corpus.size()][];
        pp.arcChild = new IloNumVar[corpus.size()][][];
        pp.flowRoot = new IloNumVar[corpus.size()][];
        pp.flowChild = new IloNumVar[corpus.size()][][];
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            pp.arcRoot[s] = new IloNumVar[sent.size()];
            pp.arcChild[s] = new IloNumVar[sent.size()][sent.size()];
            pp.flowRoot[s] = new IloNumVar[sent.size()];
            pp.flowChild[s] = new IloNumVar[sent.size()][sent.size()];
            for (int c = 0; c < sent.size(); c++) {
                pp.arcRoot[s][c] = cplex.numVar(0, 1, String.format("arcRoot_{%d,%d}", s, c));
                pp.flowRoot[s][c] = cplex.numVar(0, sent.size(), String.format("flowRoot_{%d,%d}", s, c));
            }
            for (int p = 0; p < sent.size(); p++) {
                for (int c = 0; c < sent.size(); c++) {
                    if (p == c) {
                        continue;
                    }
                    pp.arcChild[s][p][c] = cplex.numVar(0, 1, String.format("arcChild_{%d,%d,%d}", s, p, c));
                    pp.flowChild[s][p][c] = cplex
                            .numVar(0, sent.size(), String.format("flowChild_{%d,%d,%d}", s, p, c));
                }
            }
        }

        // Construct the DMV Objective support variables.
        pp.numToSide = new IloNumVar[corpus.size()][][];
        pp.genAdj = new IloNumVar[corpus.size()][][];
        pp.stopAdj = new IloNumVar[corpus.size()][][];
        pp.numNonAdj = new IloNumVar[corpus.size()][][];
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            pp.numToSide[s] = new IloNumVar[sent.size()][2];
            pp.genAdj[s] = new IloNumVar[sent.size()][2];
            pp.stopAdj[s] = new IloNumVar[sent.size()][2];
            pp.numNonAdj[s] = new IloNumVar[sent.size()][2];
            for (int i = 0; i < sent.size(); i++) {
                for (int side = 0; side < 2; side++) {
                    pp.numToSide[s][i][side] = cplex.numVar(0, sent.size(), String.format("numToSide_{%d,%d,%d}", s, i,
                            side));
                    pp.genAdj[s][i][side] = cplex.numVar(0, 1, String.format("genAdj_{%d,%d,%d}", s, i, side));
                    pp.stopAdj[s][i][side] = cplex.numVar(0, 1, String.format("stopAdj_{%d,%d,%d}", s, i, side));
                    pp.numNonAdj[s][i][side] = cplex.numVar(0, sent.size() - 1, String.format("numNonAdj_{%d,%d,%d}", s,
                            i, side));
                }
            }
        }

        // Add extra tree constraints.

        // # This constraint is optional, but we include it for all formulations
        // # The wall has one outgoing arc
        // subto one_child_for_wall:
        // forall <s> in Sents:
        // 1 == sum <j> in { 1 to Length[s] }: arc[s,0,j];
        pp.oneArcPerWall = new IloRange[corpus.size()];
        for (int s = 0; s < corpus.size(); s++) {
            double[] ones = new double[pp.arcRoot[s].length];
            Arrays.fill(ones, 1.0);
            IloLinearNumExpr expr = cplex.scalProd(ones, pp.arcRoot[s]);
            pp.oneArcPerWall[s] = cplex.eq(expr, 1.0, "oneArcPerWall");
        }

        // # Other tree constraints
        // # Each node should have a parent (except the wall)
        // subto one_incoming_arc:
        // forall <s> in Sents:
        // forall <j> in { 1 to Length[s] }:
        // sum <i> in { 0 to Length[s] } with i != j: arc[s,i,j] == 1;
        pp.oneParent = new IloRange[corpus.size()][];
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            pp.oneParent[s] = new IloRange[sent.size()];
            for (int c = 0; c < sent.size(); c++) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                for (int p = 0; p < sent.size(); p++) {
                    if (p == c) {
                        continue;
                    }
                    expr.addTerm(1.0, pp.arcChild[s][p][c]);
                }
                expr.addTerm(1.0, pp.arcRoot[s][c]);
                pp.oneParent[s][c] = cplex.eq(expr, 1.0, "oneParent");
            }
        }

        // TODO: No self-arcs.

        // Add flow constraints.

        // # ==================================================
        // # ==== Option 2: Non-projective parsing ====
        // # Flow Constraints for: B is connected
        // var flow[<s,i,j> in AllArcs] real >= 0 <= Length[s]; # No priority
        // because this is a real
        //
        // subto flow_sum:
        // forall <s> in Sents:
        // Length[s] == sum <j> in { 1 to Length[s] }: flow[s,0,j];
        pp.rootFlowIsSentLength = new IloRange[corpus.size()];
        for (int s = 0; s < corpus.size(); s++) {
            double[] ones = new double[pp.arcRoot[s].length];
            Arrays.fill(ones, 1.0);
            IloLinearNumExpr expr = cplex.scalProd(ones, pp.flowRoot[s]);
            Sentence sent = corpus.getSentence(s);
            pp.rootFlowIsSentLength[s] = cplex.eq(expr, sent.size(), "rootFlowIsSentLength");
        }

        // Out-flow equals in-flow minus one.
        // subto flow_diff:
        // forall <s> in Sents:
        // forall <i> in { 1 to Length[s] }:
        // 1 == (sum <j> in {0 to Length[s] } with i != j: flow[s,j,i])
        // - (sum <j> in { 0 to Length[s] } with i != j: flow[s,i,j]);
        pp.flowDiff = new IloRange[corpus.size()][];
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            pp.flowDiff[s] = new IloRange[sent.size()];
            for (int i = 0; i < sent.size(); i++) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                for (int p = 0; p < sent.size(); p++) {
                    if (p == i) { continue; }
                    expr.addTerm(1.0, pp.flowChild[s][p][i]);
                }
                expr.addTerm(1.0, pp.flowRoot[s][i]);
                for (int c = 0; c < sent.size(); c++) {
                    if (i == c) { continue; }
                    expr.addTerm(-1.0, pp.flowChild[s][i][c]);
                }
                pp.flowDiff[s][i] = cplex.eq(expr, 1.0, "flowDiff");
            }
        }

        // subto flow_bound:
        // forall <s,i,j> in AllArcs:
        // flow[s,i,j] <= Length[s] * arc[s,i,j];
        // EQUIVALENTLY:
        // flow[s,i,j] - Length[s] * arc[s,i,j] <= 0.0;
        pp.flowBoundRoot = new IloRange[corpus.size()][];
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            pp.flowBoundRoot[s] = new IloRange[sent.size()];
            for (int c = 0; c < sent.size(); c++) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                expr.addTerm(1.0, pp.flowRoot[s][c]);
                expr.addTerm(-sent.size(), pp.arcRoot[s][c]);
                pp.flowBoundRoot[s][c] = cplex.le(expr, 0.0, "flowBoundRoot");
            }
        }
        
        pp.flowBoundChild = new IloRange[corpus.size()][][];
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            pp.flowBoundChild[s] = new IloRange[sent.size()][sent.size()];
            for (int c = 0; c < sent.size(); c++) {
                for (int p = 0; p < sent.size(); p++) {
                    if (p == c) { continue; }
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(1.0, pp.flowChild[s][p][c]);
                    expr.addTerm(-sent.size(), pp.arcChild[s][p][c]);
                    pp.flowBoundChild[s][p][c] = cplex.le(expr, 0.0, "flowBoundChild");
                }
            }
        }

        if (formulation == IlpFormulation.FLOW_PROJ_LPRELAX) {
            // Add projectivity constraint.

            // # ==================================================
            // # ==== Option 4: Projective parsing (also requires constraints
            // from Option 2) ====
            // # This constraint ensures that descendents of Word[s,i] are not
            // parents of nodes outside the range [i,j]
            // # and that the parents of those nodes are not outside the
            // range[i,j]
            // subto proj_parse_no_illegal_parents:
            // forall <s,i,j> in AllArcs with abs(i-j) > 1:
            // (sum <k,l> in Arcs[s] with (k > min(i,j) and k < max(i,j)) and (l
            // < min(i,j) or l > max(i,j)): (arc[s,k,l] + arc[s,l,k])) <=
            // Length[s] * (1 - arc[s,i,j]);
            // # ==================================================
            pp.projectiveRoot = new IloRange[corpus.size()][];
            for (int s = 0; s < corpus.size(); s++) {
                Sentence sent = corpus.getSentence(s);
                pp.projectiveRoot[s] = new IloRange[sent.size()];
                for (int c = 0; c < sent.size(); c++) {
                    int p = -1;
                    IloLinearNumExpr expr = cplex.linearNumExpr();

                    for (int k = Math.min(p, c) + 1; k < Math.max(p, c); k++) {
                        for (int l = 0; l < sent.size(); l++) {
                            if (l >= Math.min(p, c) && l <= Math.max(p, c)) {
                                continue;
                            }
                            expr.addTerm(1.0, pp.arcChild[s][k][l]);
                            expr.addTerm(1.0, pp.arcChild[s][l][k]);
                        }
                    }
                    // TODO: a better constraint, would use the count of non-projective arcs
                    // instead of sent.size() as the multiplier.
                    expr.addTerm(sent.size(), pp.arcRoot[s][c]);
                    pp.projectiveRoot[s][c] = cplex.le(expr, sent.size(), "projectiveRoot");
                }
            }
            
            pp.projectiveChild = new IloRange[corpus.size()][][];
            for (int s = 0; s < corpus.size(); s++) {
                Sentence sent = corpus.getSentence(s);
                pp.projectiveChild[s] = new IloRange[sent.size()][sent.size()];
                for (int c = 0; c < sent.size(); c++) {
                    for (int p = 0; p < sent.size(); p++) {
                        if (c == p) {
                            // TODO: this will create null entries, is this a
                            // problem when adding to the LPMatrix?
                            continue;
                        }
                        IloLinearNumExpr expr = cplex.linearNumExpr();

                        for (int k = Math.min(p, c) + 1; k < Math.max(p, c); k++) {
                            for (int l = 0; l < sent.size(); l++) {
                                if (l >= Math.min(p, c) && l <= Math.max(p, c)) {
                                    continue;
                                }
                                expr.addTerm(1.0, pp.arcChild[s][k][l]);
                                expr.addTerm(1.0, pp.arcChild[s][l][k]);
                            }
                        }
                        // TODO: a better constraint, would use the count of non-projective arcs
                        // instead of sent.size() as the multiplier.
                        expr.addTerm(sent.size(), pp.arcChild[s][p][c]);
                        pp.projectiveChild[s][p][c] = cplex.le(expr, sent.size(), "projectiveChild");
                    }
                }
            }
        }

        // Add DMV objective support constraints.

        // subto numToSideLeft:
        // forall <s,i> in AllTokens:
        // numToSide[s,i,"l"] == sum <j> in { 0 to i-1 }: arc[s,i,j];
        // subto numToSideRight:
        // forall <s,i> in AllTokens:
        // numToSide[s,i,"r"] == sum <j> in { i+1 to Length[s] }: arc[s,i,j];
        pp.numToSideCons = new IloRange[corpus.size()][][];
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            pp.numToSideCons[s] = new IloRange[sent.size()][2];
            for (int p = 0; p < sent.size(); p++) {
                for (int side = 0; side < 2; side++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    if (side == DmvModel.Lr.LEFT.getAsInt()) {
                        // Left side.
                        for (int c = 0; c < p; c++) {
                            expr.addTerm(1.0, pp.arcChild[s][p][c]);
                        }
                    } else {
                        // Right side.
                        for (int c = p + 1; c < sent.size(); c++) {
                            expr.addTerm(1.0, pp.arcChild[s][p][c]);
                        }
                    }
                    expr.addTerm(-1.0, pp.numToSide[s][p][side]);
                    pp.numToSideCons[s][p][side] = cplex.eq(expr, 0.0, "numToSideCons");
                }
            }
        }

        // subto genAdjLeftAndRight:
        // forall <s,i,lr> in AllTokensLR:
        // genAdj[s,i,lr] >= numToSide[s,i,lr]/Length[s];
        pp.genAdjCons = new IloRange[corpus.size()][][];
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            pp.genAdjCons[s] = new IloRange[sent.size()][2];
            for (int p = 0; p < sent.size(); p++) {
                for (int side = 0; side < 2; side++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(-1.0, pp.genAdj[s][p][side]);
                    expr.addTerm(1.0 / sent.size(), pp.numToSide[s][p][side]);
                    pp.genAdjCons[s][p][side] = cplex.le(expr, 0.0, "genAdjCons");
                }
            }
        }

        // subto numNonAdjLeftAndRight:
        // forall <s,i,lr> in AllTokensLR:
        // numNonAdj[s,i,lr] == numToSide[s,i,lr] - genAdj[s,i,lr];
        pp.numNonAdjCons = new IloRange[corpus.size()][][];
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            pp.numNonAdjCons[s] = new IloRange[sent.size()][2];
            for (int p = 0; p < sent.size(); p++) {
                for (int side = 0; side < 2; side++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(1.0, pp.numToSide[s][p][side]);
                    expr.addTerm(-1.0, pp.genAdj[s][p][side]);
                    expr.addTerm(-1.0, pp.numNonAdj[s][p][side]);
                    pp.numNonAdjCons[s][p][side] = cplex.eq(expr, 0.0, "numNonAdjCons");
                }
            }
        }

        // subto noGenAdj:
        //    forall <s,i,lr> in AllTokensLR:
        //       stopAdj[s,i,lr] == 1 - genAdj[s,i,lr]
        pp.stopAdjCons = new IloRange[corpus.size()][][];
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            pp.stopAdjCons[s] = new IloRange[sent.size()][2];
            for (int p = 0; p < sent.size(); p++) {
                for (int side = 0; side < 2; side++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(1.0, pp.genAdj[s][p][side]);
                    expr.addTerm(1.0, pp.stopAdj[s][p][side]);
                    pp.stopAdjCons[s][p][side] = cplex.eq(expr, 1.0, "stopAdjCons");
                }
            }
        }

        // Add constraints to LP Matrix.
        pp.mat = cplex.LPMatrix();

        pp.mat.addRows(pp.oneArcPerWall);
        CplexUtils.addRows(pp.mat, pp.oneParent);
        pp.mat.addRows(pp.rootFlowIsSentLength);
        CplexUtils.addRows(pp.mat, pp.flowDiff);
        CplexUtils.addRows(pp.mat, pp.flowBoundRoot);
        CplexUtils.addRows(pp.mat, pp.flowBoundChild);
        if (formulation == IlpFormulation.FLOW_PROJ_LPRELAX) {
            CplexUtils.addRows(pp.mat, pp.projectiveRoot);
            CplexUtils.addRows(pp.mat, pp.projectiveChild);
        }
        CplexUtils.addRows(pp.mat, pp.numToSideCons);
        CplexUtils.addRows(pp.mat, pp.genAdjCons);
        CplexUtils.addRows(pp.mat, pp.numNonAdjCons);
        CplexUtils.addRows(pp.mat, pp.stopAdjCons);

        // Add DMV objective.

        // # ---------- DMV log-likelihood ----------
        //
        // # Prob for stop weights:
        // # 1. logprob of stopping adjacent without generating any children OR
        // # 2. logprob of not stopping adjacent
        // # + logprob of not stopping non-adjacent * number of times
        // non-adjacent children were generated
        // # + logprob of stopping non-adajacent
        // maximize goal:
        // (sum <s,i,j> in AllArcsObj:
        // arc[s,i,j] * LogChooseWeight[Word[s,i],LRForIJ[i,j],Word[s,j]])
        // + (sum <s,i,lr> in AllTokensLRObj:
        // ((1 - genAdj[s,i,lr]) * LogStopWeight[Word[s,i],lr,1]
        // + genAdj[s,i,lr] * LogNotStopWeight[Word[s,i],lr,1]
        // + numNonAdj[s,i,lr] * LogNotStopWeight[Word[s,i],lr,0]
        // + genAdj[s,i,lr] * LogStopWeight[Word[s,i],lr,0])
        // );
        pp.obj = cplex.maximize();
        IloLinearNumExpr expr = cplex.linearNumExpr();
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            DepSentenceDist sd = new DepSentenceDist(sent, model);
            for (int c = 0; c < sent.size(); c++) {
                expr.addTerm(sd.root[c], pp.arcRoot[s][c]);
                for (int p = 0; p < sent.size(); p++) {
                    if (p == c) { continue; }                    
                    expr.addTerm(sd.child[c][p][0], pp.arcChild[s][p][c]);
                }
            }
            for (int p = 0; p < sent.size(); p++) {
                for (int side = 0; side < 2; side++) {
                    expr.addTerm(sd.decision[p][side][0][0], pp.stopAdj[s][p][side]);
                    expr.addTerm(sd.decision[p][side][0][1] + sd.decision[p][side][1][0], pp.genAdj[s][p][side]);
                    expr.addTerm(sd.decision[p][side][1][1], pp.numNonAdj[s][p][side]);
                }
            }
        }
        pp.obj.setExpr(expr);

        return pp;
    }

    private RelaxedDepTreebank extractSolution(DmvTrainCorpus corpus, ParsingProblem pp) throws IloException {
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
