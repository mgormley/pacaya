'''
This module contains two sections:
1. Experiment stage definitions for experiments.
2. Parameter group definitions for experiments.

@author: mgormley
'''

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
from pypipeline.util import get_new_file, sweep_mult, fancify_cmd, frange
from pypipeline.util import head_sentences
import platform
from glob import glob
from pypipeline.experiment_runner import ExpParamsRunner, get_subset
from pypipeline import experiment_runner
from pypipeline import pipeline
import re
import random
from pypipeline.pipeline import write_script, RootStage, Stage
import multiprocessing
from experiments.exp_util import *
from experiments.path_defs import *
from experiments.srl_stages import ScrapeSrl, SrlExpParams

# ---------------------------- Definitions Classes ----------------------------------

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
        
    def get_param_groups_and_lists_and_paths(self):        
        g = ParamGroups()
        l = ParamGroupLists()
        p = self.path_defs.get_paths()
        g.langs = {} # Language-specific parameter groups. (always reference as "gl")
        l.langs = {} # Language-specific parameter lists.  (always reference as "ll")
        for lang_short in p.c09_lang_short_names + p.cx_lang_short_names:            
            g.langs[lang_short] = ParamGroups()
            l.langs[lang_short] = ParamGroups()
        
        # Define all the parameter groups.
        self._define_groups_features(g, p)
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
        self._define_lists_parse_and_srl(g, l, p)
        
        return g, l, p

    def _define_groups_defaults(self, g):
        g.defaults = SrlExpParams()
        
        g.defaults.set("expname", self.expname, False, False)
        g.defaults.set("timeoutSeconds", 48*60*60, incl_arg=False, incl_name=False)
        g.defaults.set("work_mem_megs", 1.5*1024, incl_arg=False, incl_name=False)
        g.defaults.update(seed=random.getrandbits(63))
        if self.queue:
            threads = 15
        elif self.big_machine:
            threads = 2
        else:
            threads = 1
        g.defaults.set("threads", threads, incl_name=False)
        g.defaults.set("sgdBatchSize", 30)
                
        g.defaults.update(
            printModel="./model.txt.gz",                          
            trainPredOut="./train-pred.txt",
            devPredOut="./dev-pred.txt",
            testPredOut="./test-pred.txt",
            trainGoldOut="./train-gold.txt",
            devGoldOut="./dev-gold.txt",
            testGoldOut="./test-gold.txt",
            modelOut="./model.binary.gz",
            senseFeatTplsOut="./sense-feat-tpls.txt",
            argFeatTplsOut="./arg-feat-tpls.txt",
            unaryFactors=True,
            linkVarType="OBSERVED",
            featCountCutoff=-1,
            predictSense=True,
            normalizeRoleNames=False,
            l2variance="10000.0",
            sgdNumPasses=5,
            featureHashMod=1000000,
            featureSelection=True,
            numFeatsToSelect=32,
            numSentsForFeatSelect=1000,
            #stopTrainingBy="01-10-14.06:00PM", # Stop by 9 hours before the ACL 2014 deadline.
            cacheType="NONE",
            #maxEntriesInMemory=g.defaults.get("sgdBatchSize")
            )
        
        g.defaults += g.adagrad
        g.defaults += g.feat_tpl_bjork_ig
                
        # Exclude parameters from the command line arguments.
        g.defaults.set_incl_arg("tagger_parser", False)
        g.defaults.set_incl_arg("language", False)
        
        # Exclude parameters from the experiment name.
        g.defaults.set_incl_name("train", False)
        g.defaults.set_incl_name("dev", False)
        g.defaults.set_incl_name("test", False)
        g.defaults.set_incl_name("useGoldSyntax", False)
        g.defaults.set_incl_name("brownClusters", False)        
        g.defaults.set_incl_name('removeAts', False)
        g.defaults.set_incl_name('predAts', False)
        g.defaults.set_incl_name('pruneModel', False)

    def _define_groups_features(self, g, p):
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
        
        g.feat_tpl_bjork         = self._get_named_template_set("/edu/jhu/nlp/features/bjorkelund-sense-feats.txt",
                                                                "/edu/jhu/nlp/features/bjorkelund-arg-feats.txt",
                                                                False, 'tpl_bjork')
        g.feat_tpl_bjork_es      = self._get_named_template_set("/edu/jhu/nlp/features/bjorkelund-es-sense-feats.txt",
                                                                "/edu/jhu/nlp/features/bjorkelund-es-arg-feats.txt",
                                                                False, 'tpl_bjork_es')
        g.feat_tpl_zhao          = self._get_named_template_set("/edu/jhu/nlp/features/zhao-en-sense-feats.txt",
                                                                "/edu/jhu/nlp/features/zhao-ca-arg-feats.txt",
                                                                False, 'tpl_zhao')
        g.feat_tpl_narad         = self._get_named_template_set("/edu/jhu/nlp/features/naradowsky-sense-feats.txt",
                                                                "/edu/jhu/nlp/features/naradowsky-arg-feats.txt",
                                                                False, 'tpl_narad')
        g.feat_mcdonald          = self._get_named_template_set("/edu/jhu/nlp/features/bjorkelund-sense-feats.txt",
                                                                "/edu/jhu/nlp/features/mcdonald-dep-feats.txt",
                                                                False, 'tpl_mcdonald')
        g.feat_mcdonald_basic    = self._get_named_template_set("/edu/jhu/nlp/features/bjorkelund-sense-feats.txt",
                                                                "/edu/jhu/nlp/features/mcdonald-basic-dep-feats.txt",
                                                                False, 'tpl_mcdonald_basic')
        g.feat_koo_basic         = self._get_named_template_set("/edu/jhu/nlp/features/bjorkelund-sense-feats.txt",
                                                                "/edu/jhu/nlp/features/koo-basic-dep-feats.txt",
                                                                False, 'tpl_koo_basic')
        g.feat_koo_hybrid        = self._get_named_template_set("/edu/jhu/nlp/features/bjorkelund-sense-feats.txt",
                                                                "/edu/jhu/nlp/features/koo-hybrid-dep-feats.txt",
                                                                False, 'tpl_koo_hybrid')
        g.feat_lluis             = self._get_named_template_set("/edu/jhu/nlp/features/bjorkelund-sense-feats.txt",
                                                                "/edu/jhu/nlp/features/lluis-arg-feats.txt",
                                                                False, 'tpl_lluis')
        g.feat_lluis_koo         = self._get_named_template_set("/edu/jhu/nlp/features/bjorkelund-sense-feats.txt",
                                                                "/edu/jhu/nlp/features/lluis-koo-arg-feats.txt",
                                                                False, 'tpl_lluis_koo')
        g.feat_tpl_bjork_ig      = g.feat_tpl_bjork + SrlExpParams(featureSelection=True, feature_set='tpl_bjork_ig', 
                                                                   numFeatsToSelect=32, numSentsForFeatSelect=1000)

        # Language specific feature sets from Bjorkelund et al. (2009).
        for lang_short in p.c09_lang_short_names:
            gl = g.langs[lang_short]
            gl.feat_tpl_bjork_ls = self._get_named_template_set("/edu/jhu/nlp/features/bjorkelund-%s-sense-feats.txt" % (lang_short),
                                                                "/edu/jhu/nlp/features/bjorkelund-%s-arg-feats.txt" % (lang_short),
                                                                False, 'tpl_bjork_ls_%s' % (lang_short))

        # The coarse set uses the bjorkelund sense features.
        g.feat_tpl_coarse1        = self._get_named_template_set("/edu/jhu/nlp/features/bjorkelund-sense-feats.txt", 
                                                                 "/edu/jhu/nlp/features/coarse1-arg-feats.txt", 
                                                                 False, 'tpl_coarse1')
        g.feat_tpl_coarse2        = self._get_named_template_set("/edu/jhu/nlp/features/bjorkelund-sense-feats.txt", 
                                                                 "coarse2", False, 'tpl_coarse2')
        g.feat_tpl_custom1        = self._get_named_template_set("/edu/jhu/nlp/features/custom1-sense-feats.txt",
                                                                "/edu/jhu/nlp/features/custom1-arg-feats.txt",
                                                                False, 'tpl_custom1')
        g.feat_all = g.feat_tpl_bjork_ig
    
    def _define_lists_features(self, g, l):
        l.feature_sets = [ g.feat_all, g.feat_simple_narad_zhao, g.feat_simple_narad_dep, g.feat_simple_narad, 
                           g.feat_simple_zhao_dep, g.feat_simple_zhao, g.feat_simple_dep, g.feat_simple, 
                           g.feat_narad_zhao_dep, g.feat_narad_zhao, g.feat_narad_dep, g.feat_narad, g.feat_zhao_dep, 
                           g.feat_zhao, g.feat_dep, g.feat_bjork ]
        
    def combine_feat_tpls(self, tpls1, tpls2):
        senseFeatTpls = tpls1.get("senseFeatTpls") + ":" + tpls2.get("senseFeatTpls")
        argFeatTpls = tpls1.get("argFeatTpls") + ":" + tpls2.get("argFeatTpls")
        dp1FeatTpls = tpls1.get("dp1FeatTpls") + ":" + tpls2.get("dp1FeatTpls")
        feature_set = tpls1.get("feature_set") + "_" + tpls2.get("feature_set")
        return tpls1 + tpls2 + SrlExpParams(senseFeatTpls=senseFeatTpls, argFeatTpls=argFeatTpls, 
                                            dp1FeatTpls=dp1FeatTpls, feature_set=feature_set)
        
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
        feats.set('dp1FeatTpls', arg, incl_name=False, incl_arg=True)
        feats.set('featureSelection', feature_selection, incl_name=False, incl_arg=True)
        feats.set('feature_set', feature_set_name, incl_name=True, incl_arg=False)
        return feats
    
    def _define_groups_optimizer(self, g):
        g.sgd = SrlExpParams(optimizer="SGD", sgdInitialLr=0.1, sgdAutoSelectLr=True)
        g.adagrad = SrlExpParams(optimizer="ADAGRAD", adaGradEta=0.1, adaGradConstantAddend=1e-9, sgdAutoSelectLr=True)
        g.adadelta = SrlExpParams(optimizer="ADADELTA", adaDeltaDecayRate=0.95, adaDeltaConstantAddend=math.exp(-6.0), sgdAutoSelectLr=False)
        g.lbfgs = SrlExpParams(optimizer="LBFGS")
        
    def _define_lists_optimizer(self, g, l):
        l.optimizers = [g.sgd, g.adagrad, g.adadelta, g.lbfgs]    
    
    def _define_groups_model(self, g):
        g.model_pg_lat_tree = SrlExpParams(roleStructure="PREDS_GIVEN", useProjDepTreeFactor=True, linkVarType="LATENT", 
                                           predAts="SRL", latAts="DEP_TREE", removeAts="DEPREL")
        g.model_pg_prd_tree = SrlExpParams(roleStructure="PREDS_GIVEN", useProjDepTreeFactor=True, linkVarType="PREDICTED", 
                                           predAts="SRL,DEP_TREE", removeAts="DEPREL")
        g.model_pg_obs_tree = SrlExpParams(roleStructure="PREDS_GIVEN", useProjDepTreeFactor=False, linkVarType="OBSERVED",
                                           predAts="SRL")                        
        g.model_ap_lat_tree = SrlExpParams(roleStructure="ALL_PAIRS", useProjDepTreeFactor=True, linkVarType="LATENT", 
                                           predAts="SRL", latAts="DEP_TREE", removeAts="DEPREL")
        g.model_ap_prd_tree = SrlExpParams(roleStructure="ALL_PAIRS", useProjDepTreeFactor=True, linkVarType="PREDICTED", 
                                           predAts="SRL,DEP_TREE", removeAts="DEPREL")
        g.model_ap_obs_tree = SrlExpParams(roleStructure="ALL_PAIRS", useProjDepTreeFactor=False, linkVarType="OBSERVED",
                                           predAts="SRL")
        g.model_ap_lat_tree_predpos = g.model_ap_lat_tree + SrlExpParams(roleStructure="ALL_PAIRS", makeUnknownPredRolesLatent=False, predictSense=False, predictPredPos=True, 
                                                                         binarySenseRoleFactors=False, predAts="SRL,SRL_PRED_IDX,DEP_TREE", removeAts="DEPREL")

    def _define_lists_model(self, g, l):
        l.models = [g.model_pg_obs_tree, g.model_pg_prd_tree, g.model_pg_lat_tree,
                    g.model_ap_obs_tree, g.model_ap_prd_tree, g.model_ap_lat_tree]
                
    def _define_groups_parser_output(self, g, p):
        for lang_short in p.c09_lang_short_names:
            gl = g.langs[lang_short]
            self._define_groups_parser_output_for_lang(gl, p, lang_short)
            # Store for lookup in the "language-specific" dictionary.
    
    def _define_groups_parser_output_for_lang(self, gl, p, lang_short):
        gl.pos_gold = self._get_pos_gold(p, lang_short)    
        gl.pos_sup = self._get_pos_sup(p, lang_short)
        gl.pos_semi = self._get_pos_semi(p, lang_short)
        gl.pos_unsup = self._get_pos_unsup(p, lang_short)
        gl.brown_semi = self._get_brown_semi(p, lang_short)
        gl.brown_unsup = self._get_brown_unsup(p, lang_short)
        
        # TODO: This sets the Brown clusters. MOVE THIS!!
        pl = p.langs[lang_short]
        for x in [gl.pos_gold, gl.pos_sup, gl.pos_semi, gl.pos_unsup, gl.brown_semi, gl.brown_unsup]:
            if self.fast:
                x.update(brownClusters=p.bc_tiny)
            else:
                x.update(brownClusters=pl.bc_1000)
            
    # ------------------------------ START Parser Outputs ------------------------------
    def _get_pos_gold(self, p, lang_short):
        # Gold trees: HEAD column.
        return SrlExpParams(tagger_parser = 'pos-gold', 
                            train = p.get(lang_short + "_pos_gold_train"), trainType = "CONLL_2009",
                            dev = p.get(lang_short + "_pos_gold_dev"), devType = "CONLL_2009",
                            test = p.get(lang_short + "_pos_gold_eval"), testType = "CONLL_2009",
                            useGoldSyntax = True, language = lang_short)
        
    def _get_pos_sup(self, p, lang_short):
        # Supervised parser output: PHEAD column.
        return SrlExpParams(tagger_parser = 'pos-sup', 
                            train = p.get(lang_short + "_pos_gold_train"), trainType = "CONLL_2009",
                            dev = p.get(lang_short + "_pos_gold_dev"), devType = "CONLL_2009",
                            test = p.get(lang_short + "_pos_gold_eval"), testType = "CONLL_2009",
                            useGoldSyntax = False, language = lang_short)
        
    def _get_pos_semi(self, p, lang_short):  
        # Semi-supervised parser output: PHEAD column.        
        return SrlExpParams(tagger_parser = 'pos-semi', 
                            train = p.get(lang_short + "_pos_semi_train"), trainType = "CONLL_2009",
                            dev = p.get(lang_short + "_pos_semi_dev"), devType = "CONLL_2009",
                            test = p.get(lang_short + "_pos_semi_eval"), testType = "CONLL_2009",
                            removeAts = "DEPREL", useGoldSyntax = False, language = lang_short)
        
    def _get_pos_unsup(self, p, lang_short):  
        # Unsupervised parser output: PHEAD column.
        return SrlExpParams(tagger_parser = 'pos-unsup', 
                            train = p.get(lang_short + "_pos_unsup_train"), trainType = "CONLL_2009",
                            dev = p.get(lang_short + "_pos_unsup_dev"), devType = "CONLL_2009",
                            test = p.get(lang_short + "_pos_unsup_eval"), testType = "CONLL_2009",
                            removeAts = "DEPREL", useGoldSyntax = False, language = lang_short)
                
    def _get_brown_semi(self, p, lang_short):  
        # --- Brown cluster tags ---
        # Semi-supervised parser output: PHEAD column.
        return SrlExpParams(tagger_parser = 'brown-semi', 
                            train = p.get(lang_short + "_brown_semi_train"), trainType = "CONLL_2009",
                            dev = p.get(lang_short + "_brown_semi_dev"), devType = "CONLL_2009",
                            test = p.get(lang_short + "_brown_semi_eval"), testType = "CONLL_2009",
                            removeAts = "DEPREL", useGoldSyntax = False, language = lang_short)
        
    def _get_brown_unsup(self, p, lang_short):
        # Unsupervised parser output: PHEAD column.
        return SrlExpParams(tagger_parser = 'brown-unsup', 
                            train = p.get(lang_short + "_brown_unsup_train"), trainType = "CONLL_2009",
                            dev = p.get(lang_short + "_brown_unsup_dev"), devType = "CONLL_2009",
                            test = p.get(lang_short + "_brown_unsup_eval"), testType = "CONLL_2009",
                            removeAts = "DEPREL", useGoldSyntax = False, language = lang_short)
    
    # ------------------------------ END Parser Outputs ------------------------------         
         
    def _define_lists_parser_output(self, g, l, p):
        # Single language only, with various grammar induction parsers.        
        for lang_short in p.c09_lang_short_names:
            gl = g.langs[lang_short]
            ll = l.langs[lang_short]
            ll.parser_outputs = [gl.pos_gold, gl.pos_sup, gl.pos_semi, gl.pos_unsup, gl.brown_semi, gl.brown_unsup]
            
        # All languages, with CoNLL-09 MSTParser output only.
        l.all_parser_outputs_sup = [g.langs[lang_short].pos_sup for lang_short in p.c09_lang_short_names]
    
    def _define_lists_parse_and_srl(self, g, l, p):
        # Single language only
        for lang_short in p.c09_lang_short_names:
            gl = g.langs[lang_short]
            ll = l.langs[lang_short]
            # We define the non-latent-tree model for all the input parses,
            # but we only need to define the latent-tree model for one of the input 
            # datasets.
            ll.parse_and_srl = combine_pairs(ll.parser_outputs, [g.model_pg_obs_tree]) + \
                combine_pairs([gl.pos_unsup, gl.brown_unsup], [g.model_pg_lat_tree])   
                              
        # All languages
        l.all_parse_and_srl_sup_lat = combine_pairs(l.all_parser_outputs_sup, 
                                                          [g.model_pg_obs_tree, g.model_pg_lat_tree])
    
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
            elif exp.get("includeSrl") == False:
                base_work_mem_megs = 5 * 1000
                is_higher_order = exp.get("grandparentFactors") or exp.get("siblingFactors")
                if is_higher_order: 
                    base_work_mem_megs = 10*1000
            else:
                if exp.get("useProjDepTreeFactor"):
                    base_work_mem_megs = 20 * 1000
                else:
                    base_work_mem_megs = 20 * 1000
        else:
            base_work_mem_megs = 1.5 * 1024
        return base_work_mem_megs    
    
