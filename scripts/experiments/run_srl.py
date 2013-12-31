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
import multiprocessing

# ---------------------------- Handy Functions ----------------------------------

def get_root_dir():
    scripts_dir =  os.path.abspath(sys.path[0])
    root_dir =  os.path.dirname(os.path.dirname(scripts_dir))
    print "Using root_dir: " + root_dir
    return root_dir

def get_first_that_exists(*paths):
    for path in paths:
        if os.path.exists(path):
            return path
    raise Exception("ERROR - None of the required paths exist: " + paths)

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
    ''' This is a carrier class. Each field of this object should be a path.
        
        Below we provide set/get methods which set the attributes of this object.            
    '''
        
    def set(self, key, value):
        setattr(self, key, value)
    
    def get(self, key):
        if hasattr(self, key):
            return getattr(self, key)
        else:
            return None

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
        conll09_T03_dir = get_first_that_exists("/export/common/data/corpora/LDC/LDC2012T03/data/",
                                                self.root_dir + "/data/conll2009/LDC2012T03/data")
        conll09_T04_dir = get_first_that_exists("/export/common/data/corpora/LDC/LDC2012T04/data/",
                                                self.root_dir + "/data/conll2009/LDC2012T04/data")
        
        parser_prefix = self.root_dir + "/exp/vem-conll_005"
        require_path_exists(parser_prefix)
        
        # --- Gold/Predicted POS tags ---
        # Gold trees: HEAD column. Supervised parser output: PHEAD column.
        self._set_paths_for_conll09_lang(p, "Spanish", "sp", conll09_T03_dir, require=True)
        self._set_paths_for_conll09_lang(p, "German",  "de", conll09_T03_dir, require=False)
        self._set_paths_for_conll09_lang(p, "Czech",   "cs", conll09_T03_dir, require=False)
        self._set_paths_for_conll09_lang(p, "Catalan", "ca", conll09_T03_dir, require=False)
        self._set_paths_for_conll09_lang(p, "English", "en", conll09_T04_dir, require=False)
        self._set_paths_for_conll09_lang(p, "Chinese", "zh", conll09_T04_dir, require=False)
        
        p.lang_short_names = ["sp", "de", "cs", "ca", "en", "zh"]
        
        # Semi-supervised parser output: PHEAD column.
        p.sp_pos_semi_train = parser_prefix + "/dmv_conll09-sp-train_20_True/test-parses.txt"
        p.sp_pos_semi_dev = parser_prefix + "/dmv_conll09-sp-dev_20_True/test-parses.txt"
        # Unsupervised parser output: PHEAD column.
        p.sp_pos_unsup_train = parser_prefix + "/dmv_conll09-sp-train_20_False/test-parses.txt"
        p.sp_pos_unsup_dev = parser_prefix + "/dmv_conll09-sp-dev_20_False/test-parses.txt"
        # --- Brown cluster tags ---
        # Semi-supervised parser output: PHEAD column.
        p.sp_brown_semi_train = parser_prefix + "/dmv_conll09-sp-brown-train_20_True/test-parses.txt"
        p.sp_brown_semi_dev = parser_prefix + "/dmv_conll09-sp-brown-dev_20_True/test-parses.txt"
        # Unsupervised parser output: PHEAD column.
        p.sp_brown_unsup_train = parser_prefix + "/dmv_conll09-sp-brown-train_20_False/test-parses.txt"
        p.sp_brown_unsup_dev = parser_prefix + "/dmv_conll09-sp-brown-dev_20_False/test-parses.txt"

        return p
    
    def _set_paths_for_conll09_lang(self, p, lang_long, lang_short, data_dir, require=False):
        ''' Creates attributes on this object for the paths to the CoNLL-2009 data files.
         
        Parameters:
            p - The path object on which to set the attributes.
            lang_long - The long form of the language name (e.g. Spanish)
            lang_short - The language code (e.g. sp)
            data_dir - The CoNLL-2009 data directory (e.g. /export/common/data/corpora/LDC/LDC2012T03/data/)
            require - Whether to require these files to exist.
            
        For Spanish, this is equivalent to the following code:
            conll09_sp_dir = os.path.join(conll09_T03_dir, "CoNLL2009-ST-Spanish")        
            p.sp_pos_gold_train = conll09_sp_dir + "/CoNLL2009-ST-Spanish-train.txt"
            p.sp_pos_gold_dev = conll09_sp_dir + "/CoNLL2009-ST-Spanish-development.txt"
            p.sp_pos_gold_eval = conll09_sp_dir + "/CoNLL2009-ST-evaluation-Spanish.txt"
        '''
        lang_dir = os.path.join(data_dir, "CoNLL2009-ST-" + lang_long)
        p.set(lang_short + "_pos_gold_train", os.path.join(lang_dir, "CoNLL2009-ST-" + lang_long + "-train.txt"))
        p.set(lang_short + "_pos_gold_dev",   os.path.join(lang_dir, "CoNLL2009-ST-" + lang_long + "-development.txt"))
        p.set(lang_short + "_pos_gold_eval",  os.path.join(lang_dir, "CoNLL2009-ST-evaluation-" + lang_long + ".txt"))
        if require:
            require_path_exists(p.get(lang_short + "_pos_gold_train"))
            require_path_exists(p.get(lang_short + "_pos_gold_dev"))
            require_path_exists(p.get(lang_short + "_pos_gold_eval"))

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
        self.queue = options.queue
        self.big_machine = (multiprocessing.cpu_count() > 2)
        
    def get_param_groups_and_lists(self):        
        g = ParamGroups()
        l = ParamGroupLists()
        p = self.path_defs.get_paths()

        # Define all the parameter groups.
        self._define_groups_features(g)
        self._define_groups_optimizer(g)
        self._define_groups_model(g)
        self._define_groups_parser_output(g, p)
        
        # Define the special group (g.defaults) which may 
        # utilize the parameter groups defined above.
        self._define_groups_defaults(g)

        # Define all the parameter lists
        self._define_lists_features(g, l)
        self._define_lists_optimizer(g, l)
        self._define_lists_model(g, l)
        self._define_lists_parser_output(g, l, p)
        self._define_lists_parse_and_srl(g, l)
        
        return g, l

    def _define_groups_defaults(self, g):
        g.defaults = SrlExpParams()
        
        g.defaults.set("expname", self.expname, False, False)
        g.defaults.set("timeoutSeconds", 48*60*60, incl_arg=False, incl_name=False)
        g.defaults.set("work_mem_megs", 1.5*1024, incl_arg=False, incl_name=False)
        g.defaults.update(seed=random.getrandbits(63))
        if self.queue:
            threads = 20
        elif self.big_machine:
            threads = 2
        else:
            threads = 1
        g.defaults.set("threads", threads, incl_name=False)
        g.defaults.set("sgdBatchSize", threads)
        
        g.defaults.update(
            printModel="./model.txt.gz",                          
            trainPredOut="./train-pred.txt",
            testPredOut="./test-pred.txt",
            trainGoldOut="./train-gold.txt",
            testGoldOut="./test-gold.txt",
            modelOut="./model.binary.gz",
            alwaysIncludeLinkVars=True,
            unaryFactors=True,
            linkVarType="OBSERVED",
            featCountCutoff=-1,
            predictSense=True,
            normalizeRoleNames=False,
            l2variance="500.0",
            sgdNumPasses=10,
            featureHashMod=-1,
            featureSelection=True,
            )
        
        g.defaults += g.adagrad
        g.defaults += g.feat_tpl_bjork_ig
                
        # Exclude parameters from the command line arguments.
        g.defaults.set_incl_arg("tagger_parser", False)
        g.defaults.set_incl_arg("language", False)
        
        # Exclude parameters from the experiment name.
        g.defaults.set_incl_name("train", False)
        g.defaults.set_incl_name("test", False)
        g.defaults.set_incl_name("removeDeprel", False)
        g.defaults.set_incl_name("useGoldSyntax", False)

    def _define_groups_features(self, g):
        g.feat_bias_only         = self._get_named_feature_set(False, False, False, False, False, 'bias_only')
        g.feat_bias_only.update(biasOnly=True)
        
        # Below defines T/F values for features in 
        # this order: 
        # useSimpleFeats, useNaradFeats, useZhaoFeats, useLexicalDepPathFeats, useBjorkelundFeats       
        g.feat_simple_narad_zhao = self._get_named_feature_set(True, True, True, False, False, 'simple_narad_zhao')
        g.feat_simple_narad_dep  = self._get_named_feature_set(True, True, False, True, False, 'simple_narad_dep')
        g.feat_simple_narad      = self._get_named_feature_set(True, True, False, False, False, 'simple_narad')
        g.feat_simple_zhao_dep   = self._get_named_feature_set(True, False, True, True, False, 'simple_zhao_dep')
        g.feat_simple_zhao       = self._get_named_feature_set(True, False, True, False, False, 'simple_zhao')
        g.feat_simple_dep        = self._get_named_feature_set(True, False, False, True, False, 'simple_dep')
        g.feat_simple            = self._get_named_feature_set(True, False, False, False, False, 'simple')
        g.feat_narad_zhao_dep    = self._get_named_feature_set(False, True, True, True, False, 'narad_zhao_dep')
        g.feat_narad_zhao        = self._get_named_feature_set(False, True, True, False, False, 'narad_zhao')
        g.feat_narad_dep         = self._get_named_feature_set(False, True, False, True, False, 'narad_dep')
        g.feat_narad             = self._get_named_feature_set(False, True, False, False, False, 'narad')
        g.feat_zhao_dep          = self._get_named_feature_set(False, False, True, True, False, 'zhao_dep')
        g.feat_zhao              = self._get_named_feature_set(False, False, True, False, False, 'zhao')
        g.feat_dep               = self._get_named_feature_set(False, False, False, True, False, 'dep')
        g.feat_bjork             = self._get_named_feature_set(False, False, False, False, True, 'bjork')
        
        g.feat_tpl_bjork         = self._get_named_template_set("/edu/jhu/featurize/bjorkelund-sense-feats.txt",
                                                                "/edu/jhu/featurize/bjorkelund-arg-feats.txt",
                                                                False, 'tpl_bjork')
        g.feat_tpl_bjork_es      = self._get_named_template_set("/edu/jhu/featurize/bjorkelund-es-sense-feats.txt",
                                                                "/edu/jhu/featurize/bjorkelund-es-arg-feats.txt",
                                                                False, 'tpl_bjork_es')
        g.feat_tpl_zhao          = self._get_named_template_set("/edu/jhu/featurize/zhao-en-sense-feats.txt",
                                                                "/edu/jhu/featurize/zhao-ca-arg-feats.txt",
                                                                False, 'tpl_zhao')
        g.feat_tpl_narad         = self._get_named_template_set("/edu/jhu/featurize/naradowsky-sense-feats.txt",
                                                                "/edu/jhu/featurize/naradowsky-arg-feats.txt",
                                                                False, 'tpl_narad')        
        g.feat_tpl_bjork_ig      = g.feat_tpl_bjork + SrlExpParams(featureSelection=True, feature_set='tpl_bjork_ig')

        # The coarse set uses the bjorkelund sense features.
        g.feat_tpl_coarse        = self._get_named_template_set("/edu/jhu/featurize/bjorkelund-sense-feats.txt", "coarse", False, 'tpl_coarse')
        
        g.feat_all = g.feat_tpl_bjork_ig
    
    def _define_lists_features(self, g, l):
        l.feature_sets = [ g.feat_all, g.feat_simple_narad_zhao, g.feat_simple_narad_dep, g.feat_simple_narad, 
                           g.feat_simple_zhao_dep, g.feat_simple_zhao, g.feat_simple_dep, g.feat_simple, 
                           g.feat_narad_zhao_dep, g.feat_narad_zhao, g.feat_narad_dep, g.feat_narad, g.feat_zhao_dep, 
                           g.feat_zhao, g.feat_dep, g.feat_bjork ]
        
    def combine_feat_tpls(self, tpls1, tpls2):
        senseFeatTpls = tpls1.get("senseFeatTpls") + ":" + tpls2.get("senseFeatTpls")
        argFeatTpls = tpls1.get("argFeatTpls") + ":" + tpls2.get("argFeatTpls")
        feature_set = tpls1.get("feature_set") + "_" + tpls2.get("feature_set")
        return tpls1 + tpls2 + SrlExpParams(senseFeatTpls=senseFeatTpls, argFeatTpls=argFeatTpls, feature_set=feature_set)
        
    def _get_named_feature_set(self, simple, narad, zhao, dep, bjork, feature_set_name):
        feats = SrlExpParams()
        # Add each feature set, excluding these arguments from the experiment name.
        feats.set('useSimpleFeats', simple, incl_name=False, incl_arg=True)
        feats.set('useNaradFeats', narad, incl_name=False, incl_arg=True)
        feats.set('useZhaoFeats', zhao, incl_name=False, incl_arg=True)
        feats.set('useLexicalDepPathFeats', dep, incl_name=False, incl_arg=True)
        feats.set('useBjorkelundFeats', bjork, incl_name=False, incl_arg=True)
        feats.set('useTemplates', False, incl_name=False, incl_arg=True)
        feats.set('featureSelection', False, incl_name=False, incl_arg=True)
        # Give the feature set a name.
        feats.set('feature_set', feature_set_name, incl_name=True, incl_arg=False)
        return feats
    
    def _get_named_template_set(self, sense, arg, feature_selection, feature_set_name):
        feats = SrlExpParams()
        # Disable all the named feature sets.
        feats.set('useSimpleFeats', False, incl_name=False, incl_arg=True)
        feats.set('useNaradFeats', False, incl_name=False, incl_arg=True)
        feats.set('useZhaoFeats', False, incl_name=False, incl_arg=True)
        feats.set('useLexicalDepPathFeats', False, incl_name=False, incl_arg=True)
        feats.set('useBjorkelundFeats', False, incl_name=False, incl_arg=True)
        # Enable templates.
        feats.set('useTemplates', True, incl_name=False, incl_arg=True)
        feats.set('senseFeatTpls', sense, incl_name=False, incl_arg=True)
        feats.set('argFeatTpls', arg, incl_name=False, incl_arg=True)
        feats.set('featureSelection', feature_selection, incl_name=False, incl_arg=True)
        feats.set('feature_set', feature_set_name, incl_name=True, incl_arg=False)
        return feats
    
    def _define_groups_optimizer(self, g):
        g.sgd = SrlExpParams(optimizer="SGD", sgdInitialLr=0.5)
        g.adagrad = SrlExpParams(optimizer="ADAGRAD", adaGradEta=0.1, adaGradConstantAddend=1e-9)
        g.adadelta = SrlExpParams(optimizer="ADADELTA", adaDeltaDecayRate=0.95, adaDeltaConstantAddend=math.exp(-6.0))
        g.lbfgs = SrlExpParams(optimizer="LBFGS")
        
    def _define_lists_optimizer(self, g, l):
        l.optimizers = [g.sgd, g.adagrad, g.adadelta, g.lbfgs]    
    
    def _define_groups_model(self, g):
        g.model_pg_lat_tree = SrlExpParams(roleStructure="PREDS_GIVEN", useProjDepTreeFactor=True, linkVarType="LATENT")
        g.model_pg_prd_tree = SrlExpParams(roleStructure="PREDS_GIVEN", useProjDepTreeFactor=True, linkVarType="PREDICTED")
        g.model_pg_obs_tree = SrlExpParams(roleStructure="PREDS_GIVEN", useProjDepTreeFactor=False, linkVarType="OBSERVED")                        
        g.model_ap_lat_tree = SrlExpParams(roleStructure="ALL_PAIRS", useProjDepTreeFactor=True, linkVarType="LATENT")
        g.model_ap_prd_tree = SrlExpParams(roleStructure="ALL_PAIRS", useProjDepTreeFactor=True, linkVarType="PREDICTED")
        g.model_ap_obs_tree = SrlExpParams(roleStructure="ALL_PAIRS", useProjDepTreeFactor=False, linkVarType="OBSERVED")                        

    def _define_lists_model(self, g, l):
        l.models = [g.model_pg_obs_tree, g.model_pg_prd_tree, g.model_pg_lat_tree,
                    g.model_ap_obs_tree, g.model_ap_prd_tree, g.model_ap_lat_tree]
                
    def _get_pos_gold(self, p, lang_short):
        # Gold trees: HEAD column.
        return SrlExpParams(tagger_parser = 'pos-gold', 
                            train = p.get(lang_short + "_pos_gold_train"), trainType = "CONLL_2009",
                            test = p.get(lang_short + "_pos_gold_dev"), testType = "CONLL_2009",
                            removeDeprel = False, useGoldSyntax = True, language = lang_short)
        
    def _get_pos_sup(self, p, lang_short):
        # Supervised parser output: PHEAD column.
        return SrlExpParams(tagger_parser = 'pos-sup', 
                            train = p.get(lang_short + "_pos_gold_train"), trainType = "CONLL_2009",
                            test = p.get(lang_short + "_pos_gold_dev"), testType = "CONLL_2009",
                            removeDeprel = False, useGoldSyntax = False, language = lang_short)
        
    def _get_pos_eval(self, p, lang_short):
        # Eval trees: POS only.
        return SrlExpParams(tagger_parser = 'pos-eval', 
                            test = p.get(lang_short + "_pos_gold_eval"), testType = "CONLL_2009",
                            language = lang_short)
              
    def _define_groups_parser_output(self, g, p):
        conll_type = "CONLL_2009"

        # Eval trees: POS only, not Brown.
        # TODO: Add Brown tagged version.
        g.pos_eval = self._get_pos_eval(p, "sp")    
        # --- Gold POS tags ---
        # Gold trees: HEAD column.
        g.pos_gold = self._get_pos_gold(p, "sp")    
        # --- Predicted POS tags ---
        # Supervised parser output: PHEAD column.
        g.pos_sup = self._get_pos_sup(p, "sp")
        # Semi-supervised parser output: PHEAD column.
        g.pos_semi = SrlExpParams(tagger_parser = 'pos-semi', 
                                train = p.sp_pos_semi_train, trainType = conll_type,
                                test = p.sp_pos_semi_dev, testType = conll_type,
                                removeDeprel = True, useGoldSyntax = False, language = "sp")
        # Unsupervised parser output: PHEAD column.
        g.pos_unsup = SrlExpParams(tagger_parser = 'pos-unsup', 
                                 train = p.sp_pos_unsup_train, trainType = conll_type,
                                 test = p.sp_pos_unsup_dev, testType = conll_type,
                                removeDeprel = True, useGoldSyntax = False, language = "sp")
        # --- Brown cluster tags ---
        # Semi-supervised parser output: PHEAD column.
        g.brown_semi = SrlExpParams(tagger_parser = 'brown-semi', 
                                train = p.sp_brown_semi_train, trainType = conll_type,
                                test = p.sp_brown_semi_dev, testType = conll_type,
                                removeDeprel = True, useGoldSyntax = False, language = "sp")
        # Unsupervised parser output: PHEAD column.
        g.brown_unsup = SrlExpParams(tagger_parser = 'brown-unsup', 
                                train = p.sp_brown_unsup_train, trainType = conll_type,
                                test = p.sp_brown_unsup_dev, testType = conll_type,
                                removeDeprel = True, useGoldSyntax = False, language = "sp")

    def _define_lists_parser_output(self, g, l, p):
        # Spanish only, with various grammar induction parsers.
        l.parser_outputs = [g.pos_gold, g.pos_sup, g.pos_semi, g.pos_unsup, g.brown_semi, g.brown_unsup]
        # All languages, with CoNLL-09 MSTParser output only.
        l.all_parser_outputs_sup = [self._get_pos_sup(p, lang_short) for lang_short in p.lang_short_names]
     
    def _define_lists_parse_and_srl(self, g, l):
        # Spanish only
        l.parse_and_srl = self._get_lists_parse_and_srl(g, l.parser_outputs, "-unsup")
        # All languages
        l.all_parse_and_srl_sup_lat = self._get_lists_parse_and_srl(g, l.all_parser_outputs_sup, "-sup")
        
    def _get_lists_parse_and_srl(self, g, parser_outputs, tp_for_global_factor):
        '''Gets a list of pipelined or joint training approaches to tagging, parsing, and SRL.
        The parsing is done ahead of time.
        
        Dependencies: 
            _define_lists_parser_output() 
            _define_lists_model().
        '''
        parse_and_srl = []
        for parser_output in parser_outputs:
            # We only want to models where the predicates are given in order to match up
            # with the CoNLL-2009 shared task.
            for model in [g.model_pg_obs_tree, g.model_pg_lat_tree]:
                if model.get("useProjDepTreeFactor") \
                    and not parser_output.get("tagger_parser").find(tp_for_global_factor) != -1:                    
                    # We define the non-latent-tree model for all the input parses,
                    # but we only need to define the latent-tree model for one of the input 
                    # datasets.
                    continue
                exp = parser_output + model
                parse_and_srl.append(exp)
        return parse_and_srl
    
    def get_srl_work_mem_megs(self, exp):
        ''' Gets the (expected) memory limit for the given parameters in exp. '''
        if self.queue:
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
                    base_work_mem_megs = 50 * 1024
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
        
        if self.get("train"): 
            script += self.get_eval_script("train", True)
            script += self.get_eval_script("train", False)
        if self.get("test"):  
            script += self.get_eval_script("test", True)
            script += self.get_eval_script("test", False)
        
        return script
    
    def get_eval_script(self, data_name, predict_sense):    
        script = "\n"
        script += 'echo "Evaluating %s with predict_sense=%r"\n' % (data_name, predict_sense)
        eval_args = "" 
        eval_args += " -g " + self.get(data_name + "GoldOut") + " -s " + self.get(data_name + "PredOut")
        if predict_sense:
            eval_out = data_name + "-eval.out"
            script += "perl %s/scripts/eval/eval09.pl %s &> %s\n" % (self.root_dir, eval_args, eval_out)
        else:
            eval_out = data_name + "-no-sense" + "-eval.out"
            script += "perl %s/scripts/eval/eval09-no_sense.pl %s &> %s\n" % (self.root_dir, eval_args, eval_out)
        script += 'grep --after-context 5 "SYNTACTIC SCORES:" %s\n' % (eval_out)
        script += 'grep --after-context 11 "SEMANTIC SCORES:" %s\n' % (eval_out)
        
        return script
    
    def get_java_args(self):
        return self._get_java_args(self.work_mem_megs)


class ScrapeSrl(experiment_runner.PythonExpParams):
    
    def __init__(self, **keywords):
        experiment_runner.PythonExpParams.__init__(self,keywords)
        self.always_relaunch()

    def get_initial_keys(self):
        return "tagger_parser model k s".split()
    
    def get_instance(self):
        return ScrapeSrl()
    
    def get_name(self):
        return "scrape_srl"
    
    def create_experiment_script(self, exp_dir):
        self.add_arg(os.path.dirname(exp_dir))
        script = ""
        cmd = "python %s/scripts/experiments/scrape_srl.py %s\n" % (self.root_dir, self.get_args())
        script += fancify_cmd(cmd)
        return script

# ---------------------------- Experiments Creator Class ----------------------------------

class SrlExpParamsRunner(ExpParamsRunner):
    
    # Class variables
    known_exps = ( "srl-narad-dev20",
                    "srl-narad",
                    "srl-all",
                    "srl-all-nosup",
                    "srl-all-sup-lat",
                    "srl-lat",
                    "srl-opt",
                    "srl-benchmark",
                    "srl-feats",
                    "srl-feat-settings",
                    "srl-feat-reg",
                    "srl-feat-ig",
                    "srl-eval",
                    )
    
    def __init__(self, options):
        if options.expname not in SrlExpParamsRunner.known_exps:
            sys.stderr.write("Unknown experiment setting.\n")
            parser.print_help()
            sys.exit()
        #if options.fast: name = 
        name = options.expname if not options.fast else "fast_" + options.expname 
        ExpParamsRunner.__init__(self, name, options.queue, print_to_console=True, dry_run=options.dry_run)
        self.root_dir = os.path.abspath(get_root_dir())
        self.fast = options.fast
        self.expname = options.expname
        self.hprof = options.hprof   
        self.eval = options.eval
        
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
            g.defaults += SrlExpParams(trainMaxNumSentences=100,
                                       testMaxNumSentences=100,
                                       threads=1)
            g.defaults += g.feat_all
            return self._get_default_pipeline(g, l)

        elif self.expname == "srl-all-sup-lat":
            # Experiment on all languages with supervised and latent parses.
            g.defaults += g.feat_all
            exps = []
            for parser_srl in l.all_parse_and_srl_sup_lat:
                exp = g.defaults + parser_srl
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
            return self._get_pipeline_from_exps(exps)

        elif self.expname == "srl-all-nosup":
            g.defaults += g.feat_all
            g.defaults.set("removeLemma", True, incl_name=False)
            g.defaults.set("removeFeat", True, incl_name=False)
            return self._get_default_pipeline(g, l)
        
        elif self.expname == "srl-lat":            
            g.defaults += g.feat_all         
            g.defaults += SrlExpParams(sgdNumPasses=10)
            #g.defaults += SrlExpParams(trainMaxNumSentences=1000, trainMaxSentenceLength=20,
            #                           testMaxNumSentences=100, testMaxSentenceLength=15)
            exps = self._get_default_experiments(g, l)
            exps = [x for x in exps if x.get("linkVarType") == "LATENT"]
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-opt":
            # Experiment to do grid search over parameters for optimization.
            
            # All experiments here use the PREDS_GIVEN, observed tree model, on supervised parser output.
            exps = []
            g.defaults.set_incl_arg("group", False)
            data_settings = SrlExpParams(trainMaxNumSentences=1000,
                                         testMaxNumSentences=500)
            
            # Best so far...
            g.defaults.update(group="tuneAdaDelta")            
            for adaDeltaDecayRate in [0.90, 0.95, 0.99]:
                for adaDeltaConstantAddend in [-2., -4., -6., -8.]:
                    adaDeltaConstantAddend = math.exp(adaDeltaConstantAddend)
                    exp = g.defaults + g.model_pg_obs_tree + g.pos_sup + data_settings \
                            + g.adadelta + SrlExpParams(adaDeltaDecayRate=adaDeltaDecayRate, 
                                                        adaDeltaConstantAddend=adaDeltaConstantAddend)
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
                    
            # Best so far is adaGradEta = 0.1
            g.defaults.update(group="tuneAdaGrad")            
            for adaGradEta in [0.001, 0.01, 0.05, 0.1, 0.5, 1.0, 10.0, 100.0]:
                exp = g.defaults + g.model_pg_obs_tree + g.pos_sup + data_settings + g.adagrad + SrlExpParams(adaGradEta=adaGradEta)
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
                
            # Best so far is sgdInitialLr = 0.1     
            g.defaults.update(group="tuneSGD")
            for sgdInitialLr in [0.001, 0.01, 0.05, 0.1, 0.5, 1.0, 10.0, 100.0]:
                exp = g.defaults + g.model_pg_obs_tree + g.pos_sup + data_settings + g.sgd + SrlExpParams(sgdInitialLr=sgdInitialLr)
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp) 
                
            # Best so far is l2variance = 500 
            g.defaults.update(group="tuneL2")
            for l2variance in [0.01, 0.1, 1., 10., 100., 250., 500., 750., 1000., 10000.]:
                exp = g.defaults + g.model_pg_obs_tree + g.pos_sup + data_settings + g.lbfgs + SrlExpParams(l2variance=l2variance)
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
                
            g.defaults.update(group="compare")
            for trainMaxNumSentences in [250, 500, 1000, 2000]:
                data_settings.update(trainMaxNumSentences=trainMaxNumSentences)
                for optimizer in l.optimizers:
                    exp = g.defaults + g.model_pg_obs_tree + g.pos_sup + data_settings + optimizer
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-benchmark":
            # Experiment to do grid search over parameters for caching computation.
            
            # All experiments here use the PREDS_GIVEN, observed tree model, on supervised parser output.
            exps = []
            data_settings = SrlExpParams(trainMaxNumSentences=500,
                                         testMaxNumSentences=1)
            g.defaults.update(sgdNumPasses=1)            
            cacheSettings = [           # Number of sents: 100      500 
                             ("DISK_STORE", False, 1),    # 0.10    0.71
                             ("DISK_STORE", False, -1),   # 0.05    0.27
                             ("MEMORY_STORE", False, -1), # 0.06    0.23
                             ("NONE", False, -1),         # 0.17    0.78 
                             ("CACHE", True, 1000000),    # 0.62    2.87
                             ]
            for cacheType, gzipCache, maxEntriesInMemory in cacheSettings:
                exp = g.defaults + g.model_pg_obs_tree + g.pos_sup + data_settings \
                        + SrlExpParams(cacheType=cacheType, 
                                       gzipCache=gzipCache,
                                       maxEntriesInMemory=maxEntriesInMemory)
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exp.remove("test")
                exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-feats":
            # Experiment to compare various feature sets.
            # Test 1: for testing correctness of feature sets.
            exps = []
            g.defaults.update(trainMaxNumSentences=1000,
                              testMaxNumSentences=500,
                              threads=6,
                              work_mem_megs=5*1024)
            feature_sets = [g.feat_tpl_coarse, g.feat_tpl_narad, g.feat_tpl_zhao, g.feat_tpl_bjork, 
                            g.feat_narad, g.feat_zhao, g.feat_bjork, 
                            g.feat_tpl_bjork_es, g.feat_tpl_bjork_ig]
            g.defaults.set_incl_name('featureSelection', True)
            for feature_set in feature_sets:
                # Spanish, observed/supervised dep parse and POS tags.                    
                parser_srl = g.model_pg_obs_tree + g.pos_sup
                exp = g.defaults + parser_srl + feature_set
                #exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-feat-settings":
            # Experiment to compare various feature sets.
            # Test 2: for testing bjork_es feature set and effect of feature hashing / feature count cutoffs.
            # Doubles a test of the quality of bjorkelund spanish feats.
            exps = []
            feature_sets = [g.feat_tpl_bjork_es]
            for feature_set in feature_sets:
                for featCountCutoff in [0, 2, 4]:
                    for featureHashMod in [-1, 500000, 1000000, 10000000]:
                        # Spanish, observed/supervised dep parse and POS tags.
                        parser_srl = g.model_pg_obs_tree + g.pos_sup
                        exp = g.defaults + parser_srl + feature_set + SrlExpParams(featCountCutoff=featCountCutoff, featureHashMod=featureHashMod)
                        exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                        exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-feat-reg":
            # Test 3: for comparing regularization weights across feature sets.
            # Findings:
            # - Best l2variance for 1000 sentences is 250.
            exps = []
            g.defaults.update(testMaxNumSentences=500)
            feature_sets = [g.feat_tpl_zhao, g.feat_tpl_bjork_es, g.feat_tpl_bjork_ig]
            for trainMaxNumSentences in [500, 1000, 2000, 4000, 15000]:
                for feature_set in feature_sets:
                    for l2variance in [0.01, 0.1, 1., 10., 100., 250., 500., 750., 1000., 10000.]:
                        # Spanish, observed/supervised dep parse and POS tags.
                        parser_srl = g.model_pg_obs_tree + g.pos_sup + SrlExpParams(l2variance=l2variance, trainMaxNumSentences=trainMaxNumSentences)
                        exp = g.defaults + parser_srl + feature_set
                        if exp.get("trainMaxNumSentences") == 15000:
                            exp += SrlExpParams(threads=20, work_mem_megs=50*1024)
                            exp.remove("testMaxNumSentences")
                        else:
                            exp += SrlExpParams(threads=6, work_mem_megs=5*1024)                              
                        #exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                        exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-feat-ig":
            # TODO: Finish this exp.
            exps = []
            g.defaults.update(trainMaxNumSentences=1000,
                              testMaxNumSentences=500,
                              threads=6,
                              work_mem_megs=5*1024)
            feature_sets = [g.feat_tpl_coarse, g.feat_tpl_narad, g.feat_tpl_zhao, g.feat_tpl_bjork, 
                            g.feat_tpl_bjork_es,
                            self.prm_defs.combine_feat_tpls(g.feat_tpl_coarse, g.feat_tpl_zhao),
                            self.prm_defs.combine_feat_tpls(g.feat_tpl_coarse, g.feat_tpl_bjork_es)]
            for feature_set in feature_sets:
                for featureSelection in [False, True]:
                    # Spanish, observed/supervised dep parse and POS tags.
                    parser_srl = g.model_pg_obs_tree + g.pos_sup 
                    exp = g.defaults + parser_srl + feature_set + SrlExpParams(featureSelection=featureSelection)
                    exp.set_incl_name('featureSelection', True)
                    #exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-eval":
            # Experiment to evaluate trained models from a specified directory using test data.
            if not self.eval:
                raise Exception("--eval value required")
            exps = []
            train_exp_dir = os.path.abspath(self.eval)
            for exp_dir in glob(train_exp_dir + "/*"):
                name = os.path.basename(exp_dir)                    
                # Skip files
                if not os.path.isdir(exp_dir): continue
                # Read old parameters
                old_params = SrlExpParams()
                old_params.read(os.path.join(exp_dir, "expparams.txt"))
                # Copy over old parameters for viewing in Excel
                old_params_for_record = SrlExpParams()
                for key in old_params.keys():
                    old_params_for_record.set("old:"+key, old_params.get(key), False, False)
                old_params_for_record.set("old:tagger_parser", old_params.get("tagger_parser"), incl_name=True, incl_arg=False)
                # Create experiment
                new_params = g.defaults + old_params_for_record + old_params + SrlExpParams()
                # -- remove irrelevant params
                keys_to_remove = [ "train", "trainType", "trainPredOut",
                                   "trainGoldOut", "trainMaxSentenceLength",
                                   "trainMaxNumSentences", 
                                   "test", "testType", "testMaxSentenceLength",
                                   "testMaxNumSentences", 
                                   "modelIn", "modelOut", "printModel", "seed", 
                                   ]
                for key in keys_to_remove: 
                    new_params.remove(key)
                # -- add new params
                modelIn = old_params.get("modelOut")
                #   - skip non-experiment dirs.
                if modelIn is None: continue
                #   - prepend the experiment directory to relative paths.
                if modelIn.startswith("."):
                    modelIn = os.path.join(exp_dir, modelIn)
                #   - skip failed experiments.
                if not os.path.exists(modelIn): continue                
                # -- compose the parameters
                new_params.set("modelIn", modelIn, incl_name=False, incl_arg=True)
                new_params += g.pos_eval
                exps.append(new_params)
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
        if self.fast and len(exps) > 4: exps = exps[:4]
        root = RootStage()            
        root.add_dependents(exps)    
        scrape = ScrapeSrl(csv_file="results.csv", tsv_file="results.data")
        scrape.add_prereqs(root.dependents)
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
    parser.add_option('-e', '--expname',  help="Experiment name. [" + ", ".join(SrlExpParamsRunner.known_exps) + "]")
    parser.add_option('--hprof',  help="What type of profiling to use [cpu, heap]")
    parser.add_option('-n', '--dry_run',  action="store_true", help="Whether to just do a dry run.")
    parser.add_option('-v', '--eval', help="Experiment directory to use as input for eval")
    parser.add_option('-r', '--remote',  action="store_true", help="Whether to run remotely.")
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


