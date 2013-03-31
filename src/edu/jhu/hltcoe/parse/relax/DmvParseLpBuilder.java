package edu.jhu.hltcoe.parse.relax;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloMPModeler;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;

import java.util.Arrays;

import org.apache.log4j.Logger;

import depparsing.globals.Constants;
import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.gridsearch.cpt.CptBounds;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.gridsearch.dmv.IndexedDmvModel;
import edu.jhu.hltcoe.gridsearch.dmv.ShinyEdges;
import edu.jhu.hltcoe.lp.IloRangeLpRows;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.cky.DepSentenceDist;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.cplex.CplexUtils;

public class DmvParseLpBuilder {

    public static class DmvParseLpBuilderPrm {
        public IlpFormulation formulation = IlpFormulation.FLOW_PROJ_LPRELAX_FCOBJ;
        public boolean inclExtraCons = true;
        
        // Parameters for universal linguistic posterior constraint.
        public boolean universalPostCons = false;
        public double universalMinProp = 0.8;
        public ShinyEdges shinyEdges = null;
		public boolean setNames = false;
        
        // TODO: add a parameter for whether we want the LP relaxation, rather
        // than pushing it into the IlpFormulation enum.
    }

    private static Logger log = Logger.getLogger(DmvParseLpBuilder.class);

    private IloMPModeler cplex;
    private DmvParseLpBuilderPrm prm;
    
    public DmvParseLpBuilder(DmvParseLpBuilderPrm prm, IloMPModeler cplex) {
        this.cplex = cplex;
        this.prm = prm;
        if (!prm.formulation.isLpRelaxation()) {
            throw new IllegalStateException("must be LP relaxation");
        } else if (prm.formulation != IlpFormulation.FLOW_NONPROJ_LPRELAX
                && prm.formulation != IlpFormulation.FLOW_PROJ_LPRELAX
                && prm.formulation != IlpFormulation.FLOW_PROJ_LPRELAX_FCOBJ) {
            throw new IllegalStateException("Formulation not implemented: " + prm.formulation);
        }
        this.prm.formulation = prm.formulation;
    }

    public static class DmvTreeProgram {
        // --- Variables ---

        // Arc variables: root and child.
        public IloNumVar[][] arcRoot;
        public IloNumVar[][][] arcChild;
        // Single commodity flow variables.
        public IloNumVar[][] flowRoot;
        public IloNumVar[][][] flowChild;
        // The DMV Objective support variables.
        public IloNumVar[][][] numToSide;
        public IloNumVar[][][] genAdj;
        public IloNumVar[][][] stopAdj;
        public IloNumVar[][][] numNonAdj;
        // Feature count variables
        public IloNumVar[][] featCountVars;

        // --- Constraints ---

        // Extra tree constraints.
        public IloRange[] oneArcPerWall;
        public IloRange[][] oneParent;
        // Single commodity flow constraints.
        public IloRange[] rootFlowIsSentLength;
        public IloRange[][] flowDiff;
        public IloRange[][] flowBoundRoot;
        public IloRange[][][] flowBoundChild;
        // Extra flow constraints.
        public IloRange[][] arcFlowBoundRoot;
        public IloRange[][][] arcFlowBoundChild;
        // Projectivity constraints.
        public IloRange[][] projectiveRoot;
        public IloRange[][][] projectiveChild;
        // DMV objective support constraints.
        public IloRange[][][] numToSideCons;
        public IloRange[][][] genAdjCons;
        public IloRange[][][] numNonAdjCons;
        public IloRange[][][] stopAdjCons;
        // Feature count constraints.
        public IloRange[][] featCountCons;
        public IloRange[][][] oneArcPerPair;
        public IloRange universalPostCons;
    }
    
    public static class DmvParsingProgram extends DmvTreeProgram {
        // --- Other ---
        public IloLPMatrix mat;
        public IloObjective obj;
    }

    public DmvParsingProgram buildParsingProgram(DmvTrainCorpus corpus, DmvModel model) throws IloException {
        DmvParsingProgram pp = new DmvParsingProgram();

        // Add tree constraints and the DMV objective support constraints.
        buildDmvTreeProgram(corpus, pp);

        // Add constraints to LP Matrix.
        pp.mat = cplex.LPMatrix();
        addConsToNewMatrix(pp, pp.mat);
        
        // Add DMV objective.
        if (prm.formulation == IlpFormulation.FLOW_PROJ_LPRELAX_FCOBJ) {
            IndexedDmvModel idm = new IndexedDmvModel(corpus);
            addFeatCountDmvObj(corpus, idm, model, pp);
        } else {
            addDmvObj(corpus, model, pp);
        }

        return pp;
    }

    public DmvTreeProgram buildDmvTreeProgram(DmvTrainCorpus corpus) throws IloException {
        DmvTreeProgram pp = new DmvTreeProgram();
        buildDmvTreeProgram(corpus, pp);
        return pp;
    }
    
    private void buildDmvTreeProgram(DmvTrainCorpus corpus, DmvTreeProgram pp) throws IloException {
        // Construct the arc and flow variables.
        createArcAndFlowVars(corpus, pp);

        // Construct the DMV Objective support variables.
        createDmvSupportVars(corpus, pp);

        // Add extra tree constraints.
        addExtraTreeCons(corpus, pp);

        // Add flow constraints.
        addSingleCommodityFlowCons(corpus, pp);

        if (prm.formulation == IlpFormulation.FLOW_PROJ_LPRELAX || 
                prm.formulation == IlpFormulation.FLOW_PROJ_LPRELAX_FCOBJ) {
            // Add projectivity constraint.
            addProjectivityCons(corpus, pp);
        }

        // Add DMV objective support constraints.
        addDmvObjSupportCons(corpus, pp);
        
        if (prm.formulation == IlpFormulation.FLOW_PROJ_LPRELAX_FCOBJ) {
            // Add feature count variables and constraints.
            IndexedDmvModel idm = new IndexedDmvModel(corpus);
            addFeatCountVarsAndCons(corpus, idm, pp);
        }
        
        // Fix the supervised sentences to have the gold tree.
        addSupervision(corpus, pp);
        
        // Add posterior constraints.
        if (prm.universalPostCons) {
            IndexedDmvModel idm = new IndexedDmvModel(corpus);
            addUniversalPostCons(corpus, idm, pp);
        }
    }

    @Deprecated
    public void addConsToMatrix(DmvTreeProgram pp, IloLPMatrix mat) throws IloException {
    	log.debug("Adding constraints to matrix");
        CplexUtils.addRows(mat, pp.oneArcPerWall);
        CplexUtils.addRows(mat, pp.oneParent);
        CplexUtils.addRows(mat, pp.oneArcPerPair);
        CplexUtils.addRows(mat, pp.rootFlowIsSentLength);
        CplexUtils.addRows(mat, pp.flowDiff);
        CplexUtils.addRows(mat, pp.flowBoundRoot);
        CplexUtils.addRows(mat, pp.flowBoundChild);
        CplexUtils.addRows(mat, pp.arcFlowBoundRoot);
        CplexUtils.addRows(mat, pp.arcFlowBoundChild);
        if (pp.projectiveRoot != null && pp.projectiveChild != null) {
            CplexUtils.addRows(mat, pp.projectiveRoot);
            CplexUtils.addRows(mat, pp.projectiveChild);
        }
        CplexUtils.addRows(mat, pp.numToSideCons);
        CplexUtils.addRows(mat, pp.genAdjCons);
        CplexUtils.addRows(mat, pp.numNonAdjCons);
        CplexUtils.addRows(mat, pp.stopAdjCons);
        if (pp.featCountCons != null) {
            CplexUtils.addRows(mat, pp.featCountCons);
        }
        if (pp.universalPostCons != null) {
            CplexUtils.addRow(mat, pp.universalPostCons);
        }
        log.debug("Done adding constraints to matrix");
    }
    
    public void addConsToNewMatrix(DmvTreeProgram pp, IloLPMatrix mat) throws IloException {
    	assert(mat.getNcols() == 0);
    	IloRangeLpRows lpRows = getAsLpRows(pp);
    	log.debug("Adding rows to matrix");
    	lpRows.addRowsToMatrix(mat);
    	log.debug("Done adding rows to matrix");
    }
    
    public IloRangeLpRows getAsLpRows(DmvTreeProgram pp) throws IloException {
    	log.debug("Creating LpRows");
    	IloRangeLpRows rows = new IloRangeLpRows(prm.setNames);

        rows.addRows(pp.oneArcPerWall);
        rows.addRows(pp.oneParent);
        rows.addRows(pp.oneArcPerPair);
        rows.addRows(pp.rootFlowIsSentLength);
        rows.addRows(pp.flowDiff);
        rows.addRows(pp.flowBoundRoot);
        rows.addRows(pp.flowBoundChild);
        rows.addRows(pp.arcFlowBoundRoot);
        rows.addRows(pp.arcFlowBoundChild);
        if (pp.projectiveRoot != null && pp.projectiveChild != null) {
            rows.addRows(pp.projectiveRoot);
            rows.addRows(pp.projectiveChild);
        }
        rows.addRows(pp.numToSideCons);
        rows.addRows(pp.genAdjCons);
        rows.addRows(pp.numNonAdjCons);
        rows.addRows(pp.stopAdjCons);
        if (pp.featCountCons != null) {
            rows.addRows(pp.featCountCons);
        }
        if (pp.universalPostCons != null) {
        	rows.addRow(pp.universalPostCons);
        }
        log.debug("Done creating LpRows");
        return rows;
    }

    private void createArcAndFlowVars(DmvTrainCorpus corpus, DmvTreeProgram pp) throws IloException {
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
    }

    private void createDmvSupportVars(DmvTrainCorpus corpus, DmvTreeProgram pp) throws IloException {
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
    }

    private void addExtraTreeCons(DmvTrainCorpus corpus, DmvTreeProgram pp) throws IloException {
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
        
        if (prm.inclExtraCons) {
            // MY NEW CONSTRAINT: Each arc should be on in only one direction.
            // This was added after observing that some relaxed trees break this rule.
            //
            // forall <s> in Sents:
            // forall <j> in { 1 to Length[s] }:
            // sum <i> in { 1 to Length[s] } with i != j: arc[s,i,j] + arc[s,j,i] <= 1;
            pp.oneArcPerPair = new IloRange[corpus.size()][][];
            for (int s = 0; s < corpus.size(); s++) {
                Sentence sent = corpus.getSentence(s);
                pp.oneArcPerPair[s] = new IloRange[sent.size()][sent.size()];
                for (int c = 0; c < sent.size(); c++) {
                    for (int p = 0; p < sent.size(); p++) {
                        if (p == c) {
                            continue;
                        }
                        IloLinearNumExpr expr = cplex.linearNumExpr();
                        expr.addTerm(1.0, pp.arcChild[s][p][c]);
                        expr.addTerm(1.0, pp.arcChild[s][c][p]);
                        pp.oneArcPerPair[s][p][c] = cplex.le(expr, 1.0, "oneArcPerPair");
                    }
                }
            }
        }
    }

    private void addSingleCommodityFlowCons(DmvTrainCorpus corpus, DmvTreeProgram pp) throws IloException {
        // # ==================================================
        // # ==== Option 2: Non-projective parsing ====
        // # Flow Constraints for: B is connected
        // var flow[<s,i,j> in AllArcs] real >= 0 <= Length[s]; # No priority
        // because this is a real
        //
        
        // Root sends flow n:
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

        // Each node consumes one unit of flow:
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

        // Flow is zero on disabled arcs:
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
        
        if (prm.inclExtraCons) {
            // MY NEW CONSTRAINT: 
            // forall <s,i,j> in AllArcs:
            // arc[s,i,j] <= flow[s,i,j];
            pp.arcFlowBoundRoot = new IloRange[corpus.size()][];
            for (int s = 0; s < corpus.size(); s++) {
                Sentence sent = corpus.getSentence(s);
                pp.arcFlowBoundRoot[s] = new IloRange[sent.size()];
                for (int c = 0; c < sent.size(); c++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(-1.0, pp.flowRoot[s][c]);
                    expr.addTerm(1.0, pp.arcRoot[s][c]);
                    pp.arcFlowBoundRoot[s][c] = cplex.le(expr, 0.0, "flowBoundRoot");
                }
            }
            pp.arcFlowBoundChild = new IloRange[corpus.size()][][];
            for (int s = 0; s < corpus.size(); s++) {
                Sentence sent = corpus.getSentence(s);
                pp.arcFlowBoundChild[s] = new IloRange[sent.size()][sent.size()];
                for (int c = 0; c < sent.size(); c++) {
                    for (int p = 0; p < sent.size(); p++) {
                        if (p == c) { continue; }
                        IloLinearNumExpr expr = cplex.linearNumExpr();
                        expr.addTerm(-1.0, pp.flowChild[s][p][c]);
                        expr.addTerm(1.0, pp.arcChild[s][p][c]);
                        pp.arcFlowBoundChild[s][p][c] = cplex.le(expr, 0.0, "flowBoundChild");
                    }
                }
            }
        }
    }

    private void addProjectivityCons(DmvTrainCorpus corpus, DmvTreeProgram pp) throws IloException {
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

    private void addDmvObjSupportCons(DmvTrainCorpus corpus, DmvTreeProgram pp) throws IloException {
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
    }
    
    private void addFeatCountVarsAndCons(DmvTrainCorpus corpus, IndexedDmvModel idm, DmvTreeProgram pp) throws IloException {
        // Create feature count variables.
        createFeatCountVars(idm, pp);

        // Add feature count constraints.
        addFeatCountCons(corpus, idm, pp);
    }

    private void createFeatCountVars(IndexedDmvModel idm, DmvTreeProgram pp) throws IloException {
        CptBounds bounds = new CptBounds(idm);

        pp.featCountVars = new IloNumVar[idm.getNumConds()][];
        for (int c=0; c<idm.getNumConds(); c++) {
            pp.featCountVars[c] = new IloNumVar[idm.getNumParams(c)];
            for (int m=0; m<idm.getNumParams(c); m++) {
                // These bounds recognize that the supervised data will make a fixed contribution.
                pp.featCountVars[c][m] = cplex.numVar(bounds.getLb(Type.COUNT, c, m), bounds.getUb(Type.COUNT, c, m), String.format("featCount_{%d,%d}", c, m));
            }
        }
    }

    private void addFeatCountCons(DmvTrainCorpus corpus, IndexedDmvModel idm, DmvTreeProgram pp) throws IloException {
        // TODO: maybe use max freq to skip unnecessary featCounts. int[][] maxFreqCm = idm.getTotalMaxFreqCm();
        
        // Create the linear expressions.
        IloLinearNumExpr[][] featCountExprs = new IloLinearNumExpr[idm.getNumConds()][];
        for (int c=0; c<idm.getNumConds(); c++) {
            featCountExprs[c] = new IloLinearNumExpr[idm.getNumParams(c)];
            for (int m=0; m<idm.getNumParams(c); m++) {
                featCountExprs[c][m] = cplex.linearNumExpr();
                featCountExprs[c][m].addTerm(-1, pp.featCountVars[c][m]);
            }
        }

        // Add terms to the linear expressions.
        for (int s = 0; s < corpus.size(); s++) {
            int c;
            int m;
            Sentence sent = corpus.getSentence(s);
            int[] tags = sent.getLabelIds();
            for (int cIdx = 0; cIdx < sent.size(); cIdx++) {
                int cTag = tags[cIdx];
                
                c = idm.getCRoot();
                m = cTag;
                featCountExprs[c][m].addTerm(1, pp.arcRoot[s][cIdx]);
                
                for (int pIdx = 0; pIdx < sent.size(); pIdx++) {
                    if (pIdx == cIdx) { continue; }             
                    int pTag = tags[pIdx];
                    int side = cIdx < pIdx ? Constants.LEFT : Constants.RIGHT;
                
                    c = idm.getCChild(pTag, side, 0);
                    featCountExprs[c][m].addTerm(1, pp.arcChild[s][pIdx][cIdx]);
                }
            }
            for (int pIdx = 0; pIdx < sent.size(); pIdx++) {
                int pTag = tags[pIdx];
                for (int side = 0; side < 2; side++) {
                    c = idm.getCDecision(pTag, side, 0);
                    m = Constants.END;
                    featCountExprs[c][m].addTerm(1, pp.stopAdj[s][pIdx][side]);

                    c = idm.getCDecision(pTag, side, 0);
                    m = Constants.CONT;
                    featCountExprs[c][m].addTerm(1, pp.genAdj[s][pIdx][side]);

                    c = idm.getCDecision(pTag, side, 1);
                    m = Constants.END;
                    featCountExprs[c][m].addTerm(1, pp.genAdj[s][pIdx][side]);
                    
                    c = idm.getCDecision(pTag, side, 1);
                    m = Constants.CONT;
                    featCountExprs[c][m].addTerm(1, pp.numNonAdj[s][pIdx][side]);
                }
            }
        }

        // Create the constraints.
        pp.featCountCons = new IloRange[idm.getNumConds()][];
        for (int c=0; c<idm.getNumConds(); c++) {
            pp.featCountCons[c] = new IloRange[idm.getNumParams(c)];
            for (int m=0; m<idm.getNumParams(c); m++) {
                pp.featCountCons[c][m] = cplex.eq(featCountExprs[c][m], 0.0, "featCountCons");
            }
        }
    }

    private void addFeatCountDmvObj(DmvTrainCorpus corpus, IndexedDmvModel idm, DmvModel model, DmvParsingProgram pp) throws IloException {
        pp.obj = cplex.maximize();
        IloLinearNumExpr expr = cplex.linearNumExpr();
        double[][] logProbs = idm.getCmLogProbs(model);
        for (int c=0; c<idm.getNumConds(); c++) {
            for (int m=0; m<idm.getNumParams(c); m++) {
                expr.addTerm(logProbs[c][m], pp.featCountVars[c][m]);
            }
        }
        pp.obj.setExpr(expr);
    }

    private void addSupervision(DmvTrainCorpus corpus, DmvTreeProgram pp) throws IloException {
        for (int s = 0; s < corpus.size(); s++) {
            if (corpus.isLabeled(s)) {
                DepTree tree = corpus.getTree(s);
                int[] parents = tree.getParents();
                for (int c = 0; c < parents.length; c++) {
                    double val = (parents[c] == WallDepTreeNode.WALL_POSITION) ? 1 : 0;
                    pp.arcRoot[s][c].setLB(val);
                    pp.arcRoot[s][c].setUB(val);
                }
                for (int p = 0; p < parents.length; p++) {
                    for (int c = 0; c < parents.length; c++) {
                        if (p == c) {
                            continue;
                        }
                        double val = (parents[c] == p) ? 1 : 0;
                        pp.arcChild[s][p][c].setLB(val);
                        pp.arcChild[s][p][c].setUB(val);
                    }
                }
            }
        }
    }

    /**
     * Adds universal linguistic constraints such that prm.universalMinProp of
     * arcs are shiny edges in prm.shinyEdges.
     */
    private void addUniversalPostCons(DmvTrainCorpus corpus, IndexedDmvModel idm, DmvTreeProgram pp) throws IloException {
        log.debug("Adding posterior constraints");
        if (prm.shinyEdges == null) {
            // Default to the universal linguistic constraint.
            prm.shinyEdges = ShinyEdges.getUniversalSet(corpus.getLabelAlphabet());
        }
        
        IloLinearNumExpr expr = cplex.linearNumExpr();
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sent = corpus.getSentence(s);
            for (int c = 0; c < sent.size(); c++) {
                Label cTag = sent.get(c);
                if (prm.shinyEdges.isShiny(WallDepTreeNode.WALL_LABEL, cTag)) {
                    expr.addTerm(1.0, pp.arcRoot[s][c]);
                }
                for (int p = 0; p < sent.size(); p++) {
                    if (p == c) { continue; }
                    Label pTag = sent.get(p);
                    if (prm.shinyEdges.isShiny(pTag, cTag)) {
                        expr.addTerm(1.0, pp.arcChild[s][p][c]);
                    }
                }
            }
        }
        int numTokens = corpus.getSentences().getNumTokens();
        pp.universalPostCons = cplex.range(prm.universalMinProp  * numTokens, expr, CplexUtils.CPLEX_POS_INF, "universalPostCons");
    }

    private void addDmvObj(DmvTrainCorpus corpus, DmvModel model, DmvParsingProgram pp) throws IloException {
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
    }
}
