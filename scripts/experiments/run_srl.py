#!/usr/bin/python

import sys
import os
import getopt
import math
import tempfile
import stat
import shlex
import subprocess
from subprocess import Popen
from optparse import OptionParser
from experiments.core.util import get_new_file, sweep_mult, fancify_cmd, frange
from experiments.core.util import head_sentences
import platform
from glob import glob
from experiments.core.experiment_runner import ExpParamsRunner, get_subset
from experiments.core import experiment_runner
from experiments.core import pipeline
import re
import random
from experiments.core.pipeline import write_script, RootStage, Stage

# ---------------------------- Handy Functions ----------------------------------

def get_root_dir():
    scripts_dir =  os.path.abspath(sys.path[0])
    root_dir =  os.path.dirname(os.path.dirname(scripts_dir))
    print "Using root_dir: " + root_dir
    return root_dir

def require_path_exists(path):
    if not os.path.exists(path):
        raise Exception("ERROR - Required path does not exist: " + path)

# ---------------------------- Carrier Classes ----------------------------------

class ParamGroups():
    ''' This is a carrier class. Each field of this object should be a group of parameters.'''
    pass

class ParamGroupLists():
    ''' This is a carrier class. Each field of this object should be a list of groups of parameters.'''    
    pass

class Paths():
    ''' This is a carrier class. Each field of this object should be a path.'''
    pass

# ---------------------------- Definitions Classes ----------------------------------

class PathDefinitions():
    ''' This class exposes a single public method (get_paths),
        which returns a single Paths object.
    '''

    def __init__(self, options):
        self.root_dir = os.path.abspath(get_root_dir())
        self.fast = options.fast
    
    def get_paths(self):
        p = Paths()
        
        # --- Define paths to data directories. --- 
        conll09_sp_dir = "/export/common/data/corpora/LDC/LDC2012T03/data/CoNLL2009-ST-Spanish"
        if not os.path.exists(conll09_sp_dir):
            conll09_sp_dir = self.root_dir + "/data/conll2009/CoNLL2009-ST-Spanish"
        require_path_exists(conll09_sp_dir)
        
        parser_prefix = self.root_dir + "/exp/vem-conll_005"
        require_path_exists(parser_prefix)
        
        # --- Gold POS tags ---
        # Gold trees: HEAD column.
        p.pos_gold_train = conll09_sp_dir + "/CoNLL2009-ST-Spanish-train.txt"
        p.pos_gold_test = conll09_sp_dir + "/CoNLL2009-ST-Spanish-development.txt"
        # --- Predicted POS tags ---
        # Supervised parser output: PHEAD column.
        p.pos_sup_train = conll09_sp_dir + "/CoNLL2009-ST-Spanish-train.txt"
        p.pos_sup_test = conll09_sp_dir + "/CoNLL2009-ST-Spanish-development.txt"
        # Semi-supervised parser output: PHEAD column.
        p.pos_semi_train = parser_prefix + "/dmv_conll09-sp-train_20_True/test-parses.txt"
        p.pos_semi_test = parser_prefix + "/dmv_conll09-sp-dev_20_True/test-parses.txt"
        # Unsupervised parser output: PHEAD column.
        p.pos_unsup_train = parser_prefix + "/dmv_conll09-sp-train_20_False/test-parses.txt"
        p.pos_unsup_test = parser_prefix + "/dmv_conll09-sp-dev_20_False/test-parses.txt"
        # --- Brown cluster tags ---
        # Semi-supervised parser output: PHEAD column.
        p.brown_semi_train = parser_prefix + "/dmv_conll09-sp-brown-train_20_True/test-parses.txt"
        p.brown_semi_test = parser_prefix + "/dmv_conll09-sp-brown-dev_20_True/test-parses.txt"
        # Unsupervised parser output: PHEAD column.
        p.brown_unsup_train = parser_prefix + "/dmv_conll09-sp-brown-train_20_False/test-parses.txt"
        p.brown_unsup_test = parser_prefix + "/dmv_conll09-sp-brown-dev_20_False/test-parses.txt"

        return p

class ParamDefinitions():
    ''' This class exposes only a single public method (get_param_groups_and_lists),
        which returns a single ParamGroups object and a single ParamGroupLists object.
        These objects are defined by calling a number of private methods. Those defining
        parameter groups are prefixed by '_define_groups'. Those defining parameter group
        lists are prefixed by '_define_lists'.
    '''
    
    def __init__(self, options):
        self.root_dir = os.path.abspath(get_root_dir())
        self.fast = options.fast
        self.expname = options.expname
        self.path_defs = PathDefinitions(options)
                
    def get_param_groups_and_lists(self):        
        g = ParamGroups()
        l = ParamGroupLists()
        p = self.path_defs.get_paths()

        # Define all the parameter groups.
        self._define_groups_features(g)
        self._define_groups_parser_output(g, p)
        self._define_groups_optimizer(g)
        self._define_groups_model(g)
        
        # Define the special group (g.defaults) which may 
        # utilize the parameter groups defined above.
        self._define_groups_defaults(g)

        # Define all the parameter lists
        self._define_lists_parser_output(g, l)
        self._define_lists_features(g, l)
        self._define_lists_optimizer(g, l)
        self._define_lists_model(g, l)
        self._define_lists_parse_and_srl(g, l)
        
        return g, l

    def _define_groups_defaults(self, g):
        g.defaults = SrlExpParams()
        
        g.defaults.set("expname", self.expname, False, False)
        g.defaults.set("timeoutSeconds", 48*60*60, incl_arg=False, incl_name=False)
        g.defaults.set("work_mem_megs", 1.5*1024, incl_arg=False, incl_name=False)
        g.defaults.update(seed=random.getrandbits(63))
        
        g.defaults.update(
            printModel="./model.txt",                          
            trainPredOut="./train-pred.txt",
            testPredOut="./test-pred.txt",
            trainGoldOut="./train-gold.txt",
            testGoldOut="./test-gold.txt",
            modelOut="./model.binary.gz",
            featureHashMod=-1,
            alwaysIncludeLinkVars=True,
            unaryFactors=True,
            linkVarType="OBSERVED",
            featCountCutoff=4,
            predictSense=True,
            normalizeRoleNames=False,
            )
        
        g.defaults += g.sgd
        g.defaults += g.feat_narad
                
        # Exclude parameters from the command line arguments.
        g.defaults.set("tagger_parser", "", incl_arg=False)
        
        # Exclude parameters from the experiment name.
        g.defaults.set("train", "", incl_name=False)
        g.defaults.set("test", "", incl_name=False)
                        
    def _define_groups_parser_output(self, g, p):
        conll_type = "CONLL_2009"

        # --- Gold POS tags ---
        # Gold trees: HEAD column.
        g.pos_gold = SrlExpParams(tagger_parser = 'pos-gold', 
                                train = p.pos_gold_train, trainType = conll_type,
                                test = p.pos_gold_test, testType = conll_type)
        g.pos_gold.set("removeDeprel", False, incl_name=False)
        g.pos_gold.set("useGoldSyntax", True, incl_name=False)
        # --- Predicted POS tags ---
        # Supervised parser output: PHEAD column.
        g.pos_sup = SrlExpParams(tagger_parser = 'pos-sup', 
                                train = p.pos_sup_train, trainType = conll_type,
                                test = p.pos_sup_test, testType = conll_type)
        g.pos_sup.set("removeDeprel", False, incl_name=False)
        g.pos_sup.set("useGoldSyntax", False, incl_name=False)
        # Semi-supervised parser output: PHEAD column.
        g.pos_semi = SrlExpParams(tagger_parser = 'pos-semi', 
                                train = p.pos_semi_train, trainType = conll_type,
                                test = p.pos_semi_test, testType = conll_type)
        g.pos_semi.set("removeDeprel", True, incl_name=False)
        g.pos_semi.set("useGoldSyntax", False, incl_name=False)
        # Unsupervised parser output: PHEAD column.
        g.pos_unsup = SrlExpParams(tagger_parser = 'pos-unsup', 
                                 train = p.pos_unsup_train, trainType = conll_type,
                                 test = p.pos_unsup_test, testType = conll_type)
        g.pos_unsup.set("removeDeprel", True, incl_name=False)
        g.pos_unsup.set("useGoldSyntax", False, incl_name=False)
        # --- Brown cluster tags ---
        # Semi-supervised parser output: PHEAD column.
        g.brown_semi = SrlExpParams(tagger_parser = 'brown-semi', 
                                  train = p.brown_semi_train, trainType = conll_type,
                                  test = p.brown_semi_test, testType = conll_type)
        g.brown_semi.set("removeDeprel", True, incl_name=False)
        g.brown_semi.set("useGoldSyntax", False, incl_name=False)
        # Unsupervised parser output: PHEAD column.
        g.brown_unsup = SrlExpParams(tagger_parser = 'brown-unsup', 
                                   train = p.brown_unsup_train, trainType = conll_type,
                                   test = p.brown_unsup_test, testType = conll_type)
        g.brown_unsup.set("removeDeprel", True, incl_name=False)
        g.brown_unsup.set("useGoldSyntax", False, incl_name=False)    
                    
    def _define_lists_parser_output(self, g, l): 
        l.parser_outputs = [g.pos_gold, g.pos_sup, g.pos_semi, g.pos_unsup, g.brown_semi, g.brown_unsup]
            
    def _define_groups_features(self, g):
        g.feat_bias_only         = self._get_named_feature_set(False, False, False, False, 'bias_only')
        g.feat_bias_only.update(biasOnly=True)
        
        # Below defines T/F values for features in 
        # this order: 
        # useSimpleFeats, useNaradFeats, useZhaoFeats, useDepPathFeats        
        g.feat_all               = self._get_named_feature_set(True, True, True, True, 'all')
        g.feat_simple_narad_zhao = self._get_named_feature_set(True, True, True, False, 'simple_narad_zhao')
        g.feat_simple_narad_dep  = self._get_named_feature_set(True, True, False, True, 'simple_narad_dep')
        g.feat_simple_narad      = self._get_named_feature_set(True, True, False, False, 'simple_narad')
        g.feat_simple_zhao_dep   = self._get_named_feature_set(True, False, True, True, 'simple_zhao_dep')
        g.feat_simple_zhao       = self._get_named_feature_set(True, False, True, False, 'simple_zhao')
        g.feat_simple_dep        = self._get_named_feature_set(True, False, False, True, 'simple_dep')
        g.feat_simple            = self._get_named_feature_set(True, False, False, False, 'simple')
        g.feat_narad_zhao_dep    = self._get_named_feature_set(False, True, True, True, 'narad_zhao_dep')
        g.feat_narad_zhao        = self._get_named_feature_set(False, True, True, False, 'narad_zhao')
        g.feat_narad_dep         = self._get_named_feature_set(False, True, False, True, 'narad_dep')
        g.feat_narad             = self._get_named_feature_set(False, True, False, False, 'narad')
        g.feat_zhao_dep          = self._get_named_feature_set(False, False, True, True, 'zhao_dep')
        g.feat_zhao              = self._get_named_feature_set(False, False, True, False, 'zhao')
        g.feat_dep               = self._get_named_feature_set(False, False, False, True, 'dep')
    
    def _define_lists_features(self, g, l): 
        l.feature_sets = [ g.feat_all, g.feat_simple_narad_zhao, g.feat_simple_narad_dep, g.feat_simple_narad, 
                           g.feat_simple_zhao_dep, g.feat_simple_zhao, g.feat_simple_dep, g.feat_simple, 
                           g.feat_narad_zhao_dep, g.feat_narad_zhao, g.feat_narad_dep, g.feat_narad, g.feat_zhao_dep, 
                           g.feat_zhao, g.feat_dep ]
        
    def _get_named_feature_set(self, simple, narad, zhao, dep, feature_set_name):
        feats = SrlExpParams()
        # Add each feature set, excluding these arguments from the experiment name.
        feats.set('useSimpleFeats', simple, incl_name=False, incl_arg=True)
        feats.set('useNaradFeats', narad, incl_name=False, incl_arg=True)
        feats.set('useZhaoFeats', zhao, incl_name=False, incl_arg=True)
        feats.set('useDepPathFeats', dep, incl_name=False, incl_arg=True)
        # Give the feature set a name.
        feats.set('feature_set', feature_set_name, incl_name=True, incl_arg=False)
        return feats
    
    def _define_groups_optimizer(self, g):
        g.sgd = SrlExpParams(optimizer="SGD", l2variance="1e100")
        g.lbfgs = SrlExpParams(optimizer="LBFGS", l2variance="1e100")
        
    def _define_lists_optimizer(self, g, l):
        l.optimizers = [g.sgd, g.lbfgs]    
    
    def _define_groups_model(self, g):
        g.model_pg_lat_tree = SrlExpParams(roleStructure="PREDS_GIVEN", useProjDepTreeFactor=True, linkVarType="LATENT")
        g.model_pg_obs_tree = SrlExpParams(roleStructure="PREDS_GIVEN", useProjDepTreeFactor=False, linkVarType="OBSERVED")                        
        g.model_ap_lat_tree = SrlExpParams(roleStructure="ALL_PAIRS", useProjDepTreeFactor=True, linkVarType="LATENT")
        g.model_ap_obs_tree = SrlExpParams(roleStructure="ALL_PAIRS", useProjDepTreeFactor=False, linkVarType="OBSERVED")                        

    def _define_lists_model(self, g, l):
        l.models = [g.model_pg_obs_tree, g.model_pg_lat_tree,
                    g.model_ap_obs_tree, g.model_ap_lat_tree,]
                
    def _define_lists_parse_and_srl(self, g, l):
        '''Gets a list of pipelined or joint training approaches to tagging, parsing, and SRL.
        The parsing is done ahead of time.
        
        Dependencies: 
            _define_lists_parser_output() 
            _define_lists_model().
        '''
        l.parse_and_srl = []
        for parser_output in l.parser_outputs:
            # We only want to models where the predicates are given in order to match up
            # with the CoNLL-2009 shared task.
            for model in [g.model_pg_obs_tree, g.model_pg_lat_tree]:
                if model.get("useProjDepTreeFactor") \
                    and not parser_output.get("tagger_parser").endswith("-unsup"):                    
                    # We define the non-latent-tree model for all the input parses,
                    # but we only need to define the latent-tree model for one of the input 
                    # datasets.
                    continue
                exp = parser_output + model
                l.parse_and_srl.append(exp)
    
    def get_srl_work_mem_megs(self, exp):
        ''' Gets the (expected) memory limit for the given parameters in exp. '''
        if exp.get("biasOnly") != True and re.search(r"test[^.]+\.local", os.uname()[1]):
            if exp.get("testMaxSentenceLength") is not None and exp.get("testMaxSentenceLength") <= 20 and \
                    exp.get("trainMaxSentenceLength") is not None and exp.get("trainMaxSentenceLength") <= 20:
                # 2500 of len <= 20 fit in 1G, with  8 roles, and global factor on.
                # 2700 of len <= 20 fit in 1G, with 37 roles, and global factor off.
                # 1500 of len <= 20 fit in 1G, with 37 roles, and global factor on.
                # So, increasing to 37 roles should require a 5x increase (though we see a 2x).
                # Adding the global factor should require a 5x increase.
                if not exp.get("normalizeRoleNames") and exp.get("useProjDepTreeFactor"):
                    base_work_mem_megs = 5*3*3*1024
                elif exp.get("useProjDepTreeFactor"):
                    base_work_mem_megs = 5*3*3*1024
                elif not exp.get("normalizeRoleNames"):
                    base_work_mem_megs = 5*3*1024
                else:
                    base_work_mem_megs = 5*1024
            else:
                if exp.get("useProjDepTreeFactor"):
                    base_work_mem_megs = 200 * 1024
                else:
                    base_work_mem_megs = 50 * 1024
        else:
            base_work_mem_megs = 1.5 * 1024
        return base_work_mem_megs    
    
# ---------------------------- Experiment/Stage Classes ----------------------------------

class SrlExpParams(experiment_runner.JavaExpParams):
    
    def __init__(self, **keywords):
        experiment_runner.JavaExpParams.__init__(self,keywords)
            
    def get_initial_keys(self):
        return "tagger_parser".split()
    
    def get_instance(self):
        return SrlExpParams()
    
    def create_experiment_script(self, exp_dir):
        script = "\n"
        #script += 'echo "CLASSPATH=$CLASSPATH"\n'
        cmd = "java " + self.get_java_args() + " edu.jhu.srl.SrlRunner  %s \n" % (self.get_args())
        script += fancify_cmd(cmd)
        
        script += self.get_eval_script("train")
        script += self.get_eval_script("test")
        
        return script
    
    def get_eval_script(self, data_name):    
        script = "\n"
        script += 'echo "Evaluating %s"\n' % (data_name)
        eval_args = "" 
        if self.get("normalizeRoles") is not None:
            pass
        eval_args += " -g " + self.get(data_name + "GoldOut") + " -s " + self.get(data_name + "PredOut")
        eval_out = data_name + "-eval.out"
        if self.get("predictSense") == True:
            script += "perl %s/scripts/eval/eval09.pl %s &> %s\n" % (self.root_dir, eval_args, eval_out)
        else:
            script += "perl %s/scripts/eval/eval09-no_sense.pl %s &> %s\n" % (self.root_dir, eval_args, eval_out)
        script += 'grep --after-context 11 "SEMANTIC SCORES:" %s' % (eval_out)
        return script
    
    def get_java_args(self):
        return self._get_java_args(self.work_mem_megs)

# ---------------------------- Experiments Creator Class ----------------------------------

class SrlExpParamsRunner(ExpParamsRunner):
    
    def __init__(self, options):
        self.known_exps = ( "srl-narad-dev20",
                            "srl-narad",
                            "srl-all",
                            "srl-opt",
                            "srl-feats",
                            )
        if options.expname not in self.known_exps:
            sys.stderr.write("Unknown experiment setting.\n")
            parser.print_help()
            sys.exit()
        ExpParamsRunner.__init__(self, options.expname, options.queue, print_to_console=True, dry_run=options.dry_run)
        self.root_dir = os.path.abspath(get_root_dir())
        self.fast = options.fast
        self.expname = options.expname
        self.hprof = options.hprof   
        
        self.prm_defs = ParamDefinitions(options) 

    def get_experiments(self):
        g, l = self.prm_defs.get_param_groups_and_lists()
                
        if self.expname == "srl-narad-dev20":
            g.defaults += g.feat_narad            
            g.defaults.update(trainMaxSentenceLength=20)
            return self._get_default_pipeline(g, l)
        elif self.expname == "srl-narad":
            g.defaults += g.feat_narad
            return self._get_default_pipeline(g, l)
        elif self.expname == "srl-all":
            g.defaults += g.feat_all
            return self._get_default_pipeline(g, l)
        elif self.expname == "srl-opt":
            exps = []
            data_settings = SrlExpParams(trainMaxNumSentences=1001,
                                         testMaxNumSentences=500)    
            for l2variance in [0.01, 0.1, 1.0, 10.0]:
                # Use the PREDS_GIVEN, observed tree model, on supervised parser output.
                exp = g.defaults + g.model_pg_obs_tree + g.pos_sup + data_settings + g.lbfgs + SrlExpParams(l2variance=l2variance)
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
            for trainMaxNumSentences in [250, 500, 1000, 2000]:
                data_settings.update(trainMaxNumSentences=trainMaxNumSentences)
                for optimizer in l.optimizers:
                    # Use the PREDS_GIVEN, observed tree model, on supervised parser output.
                    exp = g.defaults + g.model_pg_obs_tree + g.pos_sup + data_settings + optimizer
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        elif self.expname == "srl-feats":
            exps = []
            g.defaults.update(trainMaxSentenceLength=10)
            for feature_set in l.feature_sets:
                g.defaults += feature_set
                for parser_srl in l.parse_and_srl:
                    exp = g.defaults + parser_srl
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        else:
            raise Exception("Unknown expname: " + str(self.expname))
    
    def _get_default_experiments(self, g, l):
        exps = []
        data_settings = SrlExpParams()                    
        for normalizeRoleNames in [True, False]:
            data_settings.update(normalizeRoleNames=normalizeRoleNames)
            for parser_srl in l.parse_and_srl:
                exp = g.defaults + data_settings + parser_srl
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
        return exps
    
    def _get_default_pipeline(self, g, l):
        exps = self._get_default_experiments(g, l)
        return self._get_pipeline_from_exps(exps)
    
    def _get_pipeline_from_exps(self, exps):
        root = RootStage()            
        root.add_dependents(exps)
        return root
    
    def update_stages_for_qsub(self, root_stage):
        ''' Makes sure that the stage object specifies reasonable values for the 
            qsub parameters given its experimental parameters.
        '''
        for stage in self.get_stages_as_list(root_stage):
            # First make sure that the "fast" setting is actually fast.
            if isinstance(stage, SrlExpParams) and self.fast:
                self.make_stage_fast(stage)
            if isinstance(stage, experiment_runner.ExpParams):
                # Update the thread count
                threads = stage.get("threads")
                if threads != None: 
                    # Add an extra thread just as a precaution.
                    stage.threads = threads + 1
                work_mem_megs = stage.get("work_mem_megs")
                if work_mem_megs != None:
                    stage.work_mem_megs = work_mem_megs
                # Update the runtime
                timeoutSeconds = stage.get("timeoutSeconds")
                if timeoutSeconds != None:
                    stage.minutes = (timeoutSeconds / 60.0)
                    # Add some extra time in case some other part of the experiment
                    # (e.g. evaluation) takes excessively long.
                    stage.minutes = (stage.minutes * 2.0) + 10
            if self.hprof:
                if isinstance(stage, experiment_runner.JavaExpParams):
                    stage.hprof = self.hprof
            # Put the output of a fast run in a directory with "fast_"
            # prepended.
            # TODO: This doesn't work quite right...find a better solution.
            #if self.fast:
            #    self.expname = "fast_" + self.expname
        return root_stage
    
    def make_stage_fast(self, stage):       
        ''' Makes the stage run in a very short period of time (under 5 seconds).
        ''' 
        stage.update(maxLbfgsIterations=3,
                     trainMaxSentenceLength=7,
                     trainMaxNumSentences=3,
                     testMaxSentenceLength=7,
                     testMaxNumSentences=3,
                     work_mem_megs=2000,
                     timeoutSeconds=20)

if __name__ == "__main__":
    usage = "%prog "

    parser = OptionParser(usage=usage)
    parser.add_option('-q', '--queue', help="Which SGE queue to use")
    parser.add_option('-f', '--fast', action="store_true", help="Run a fast version")
    parser.add_option('-e', '--expname',  help="Experiment name.")
    parser.add_option('--hprof',  help="What type of profiling to use [cpu, heap]")
    parser.add_option('-n', '--dry_run',  action="store_true", help="Whether to just do a dry run.")
    (options, args) = parser.parse_args(sys.argv)
    # TODO: Above, we still want to list the experiment names in the usage printout, but we should
    # somehow pull them from SrlExpParamsRunner so that they are less likely to get stale.

    if len(args) != 1:
        parser.print_help()
        sys.exit(1)
    
    runner = SrlExpParamsRunner(options)
    root_stage = runner.get_experiments()
    root_stage = runner.update_stages_for_qsub(root_stage)
    runner.run_pipeline(root_stage)


