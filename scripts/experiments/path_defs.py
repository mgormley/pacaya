'''
Path definitions for experiments.
@author: mgormley
'''
import sys
import os
import getopt
import math
import tempfile
import stat
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
        p.c09_lang_short_names = ["ca", "cs", "es", "de", "en", "zh"] 
        p.cx_lang_short_names = ["ar", "bg", "cs", "da", "ja", "nl", "de", "pt", "sl", "es", "sv", "tr", "en"]

        p.langs = {}        
        for lang_short in p.c09_lang_short_names + p.cx_lang_short_names:
            p.langs[lang_short] = ParamGroups()            
        
        corpora_dir = get_first_that_exists("/home/hltcoe/mgormley/corpora",
                                            "/Users/mgormley/research/corpora",
                                            "/home/mgormley/corpora")
        p.corpora_dir = corpora_dir
        ldc_dir = get_first_that_exists("/export/common/data/corpora/LDC",
                                        corpora_dir + "/LDC",
                                        self.root_dir + "/data/LDC")
        p.ldc_dir = ldc_dir
        
        # CoNLL'09 Shared Task datasets.
        conll09_T03_dir = ldc_dir + "/LDC2012T03/data"
        conll09_T04_dir = ldc_dir + "/LDC2012T04/data"
        
        self._set_paths_for_conll09_lang(p, "Spanish", "es", conll09_T03_dir, require=False)
        self._set_paths_for_conll09_lang(p, "German",  "de", conll09_T03_dir, require=False)
        self._set_paths_for_conll09_lang(p, "Czech",   "cs", conll09_T03_dir, require=False)
        self._set_paths_for_conll09_lang(p, "Catalan", "ca", conll09_T03_dir, require=False)
        self._set_paths_for_conll09_lang(p, "English", "en", conll09_T04_dir, require=False)
        self._set_paths_for_conll09_lang(p, "Chinese", "zh", conll09_T04_dir, require=False)
        
        # CoNLL'08 Shared Task dataset
        conll08_dir = ldc_dir + "/LDC2009T12/data"
        p.c08_pos_gold_train = os.path.join(conll08_dir, "train", "train.closed")
        p.c08_pos_gold_dev = os.path.join(conll08_dir, "devel", "devel.closed")
        p.c08_pos_gold_test_wsj = os.path.join(conll08_dir, "test.wsj", "test.wsj.closed.GOLD")
        p.c08_pos_gold_test_brown = os.path.join(conll08_dir, "test.brown", "test.brown.closed.GOLD")
        # Versions without nominal predicates.
        p.c08_pos_gold_train_simplified = os.path.join(self.root_dir, "data", "conll2008", "train.GOLD.simplified.conll08")
        p.c08_pos_gold_test_wsj_simplified = os.path.join(self.root_dir, "data", "conll2008", "test.wsj.GOLD.simplified.conll08")
        # Missing sentences.
        p.c08_pos_gold_test_wsj_missing = os.path.join(self.root_dir, "data", "conll2008", "test.wsj.missing.conll")
        
        
        # CoNLL-X Shared Task datasets.
        # Languages: arabic, bulgarian, czech, danish, dutch, german, portuguese, slovene, spanish, swedish, turkish.
        # Missing: japanese, chinese.
        # TODO: Fix language codes.
        conllx_dir = get_first_that_exists(corpora_dir + "/CoNLL-X",
                                           self.root_dir + "/data/conllx/CoNLL-X")
        self._set_paths_for_conllx_lang(p, "Arabic",     "ar", "PADT", conllx_dir, require=True)
        self._set_paths_for_conllx_lang(p, "Bulgarian",  "bg", "bultreebank", conllx_dir, require=True)
        self._set_paths_for_conllx_lang(p, "Czech",      "cs", "pdt", conllx_dir, require=True)
        self._set_paths_for_conllx_lang(p, "Danish",     "da", "ddt", conllx_dir, require=True)
        self._set_paths_for_conllx_lang(p, "Dutch",      "nl", "alpino", conllx_dir, require=True)
        self._set_paths_for_conllx_lang(p, "German",     "de", "tiger", conllx_dir, require=True)
        self._set_paths_for_conllx_lang(p, "Japanese",   "ja", "verbmobil", conllx_dir, require=True)
        self._set_paths_for_conllx_lang(p, "Portuguese", "pt", "bosque", conllx_dir, require=True)
        self._set_paths_for_conllx_lang(p, "Slovene",    "sl", "sdt", conllx_dir, require=True)
        self._set_paths_for_conllx_lang(p, "Spanish",    "es", "cast3lb", conllx_dir, require=True)
        self._set_paths_for_conllx_lang(p, "Swedish",    "sv", "talbanken05", conllx_dir, require=True)
        self._set_paths_for_conllx_lang(p, "Turkish",    "tr", "metu_sabanci", conllx_dir, require=True)
        # Other data in CoNLL-X format.
        self._set_paths_for_conllx_lang(p, "English",    "en", "ptb_ym", conllx_dir, require=False, has_dev=True)
        
        # Grammar Induction Output.
        parser_prefix = self.root_dir + "/exp/vem-conll_006"
        
        self._set_paths_for_conll09_parses(p, "Spanish", "es", parser_prefix, require=False)
        self._set_paths_for_conll09_parses(p, "German",  "de", parser_prefix, require=False)
        self._set_paths_for_conll09_parses(p, "Czech",   "cs", parser_prefix, require=False)
        self._set_paths_for_conll09_parses(p, "Catalan", "ca", parser_prefix, require=False)
        self._set_paths_for_conll09_parses(p, "English", "en", parser_prefix, require=False)
        self._set_paths_for_conll09_parses(p, "Chinese", "zh", parser_prefix, require=False)
                
        # Brown Clusters.
        bc_256_dir = get_first_that_exists("/home/hltcoe/mgormley/working/word_embeddings/bc_out_256",
                                           corpora_dir + "/embeddings/bc_out_256",
                                           self.root_dir + "/data/bc_out_256")
        bc_1000_dir = get_first_that_exists("/home/hltcoe/mgormley/working/word_embeddings/bc_out_1000",
                                            corpora_dir + "/embeddings/bc_out_1000",
                                            self.root_dir + "/data/bc_out_1000")
        p.bc_tiny = os.path.join(bc_1000_dir, "paths.tiny")
        for lang_short in p.c09_lang_short_names:
            pl = p.langs[lang_short]
            pl.bc_256 = os.path.join(bc_256_dir, "full.txt_%s_256" % (lang_short), "paths.cutoff")
            pl.bc_1000 = os.path.join(bc_1000_dir, "full.txt_%s_1000" % (lang_short), "bc", "paths")
            
        return p
    
    def _set_paths_for_conll09_lang(self, p, lang_long, lang_short, data_dir, require=False):
        ''' Creates attributes on this object for the paths to the CoNLL-2009 data files. Also
        adds them to the language specific dictionary p.langs[lang_short].
         
        Parameters:
            p - The path object on which to set the attributes.
            lang_long - The long form of the language name (e.g. Spanish)
            lang_short - The language code (e.g. es)
            data_dir - The CoNLL-2009 data directory (e.g. /export/common/data/corpora/LDC/LDC2012T03/data/)
            require - Whether to require these files to exist.
            
        For Spanish, this is equivalent to the following code:
            conll09_sp_dir = os.path.join(conll09_T03_dir, "CoNLL2009-ST-Spanish")        
            p.es_pos_gold_train = conll09_sp_dir + "/CoNLL2009-ST-Spanish-train.txt"
            p.es_pos_gold_dev = conll09_sp_dir + "/CoNLL2009-ST-Spanish-development.txt"
            p.es_pos_gold_eval = conll09_sp_dir + "/CoNLL2009-ST-evaluation-Spanish.txt"
        '''
        lang_dir = os.path.join(data_dir, "CoNLL2009-ST-" + lang_long)
        pos_gold_train = os.path.join(lang_dir, "CoNLL2009-ST-" + lang_long + "-train.txt")
        pos_gold_dev   = os.path.join(lang_dir, "CoNLL2009-ST-" + lang_long + "-development.txt")
        pos_gold_eval  = os.path.join(lang_dir, "CoNLL2009-ST-evaluation-" + lang_long + ".txt")
        # Set with setattr.
        p.set(lang_short + "_pos_gold_train", pos_gold_train)
        p.set(lang_short + "_pos_gold_dev",   pos_gold_dev)
        p.set(lang_short + "_pos_gold_eval",  pos_gold_eval)
        # Set on dictionary.
        pl = p.langs[lang_short]
        pl.pos_gold_train = pos_gold_train
        pl.pos_gold_dev = pos_gold_dev
        pl.pos_gold_eval = pos_gold_eval
        # Require some paths.
        if require:
            require_path_exists(pos_gold_train, pos_gold_dev, pos_gold_eval)
        
    def _set_paths_for_conllx_lang(self, p, lang_long, lang_short, treebank, data_dir, require=False, has_dev=False):
        ''' Creates attributes on this object for the paths to the CoNLL-X data files. Also
        adds them to the language specific dictionary p.langs[lang_short].
         
        Parameters:
            p - The path object on which to set the attributes.
            lang_long - The long form of the language name (e.g. Spanish)
            lang_short - The language code (e.g. es)
            treebank - The name of the treebank (e.g. cast3lb)
            data_dir - The CoNLL-X data directory (e.g. ./data/conllx/CoNLL-X)
            require - Whether to require these files to exist.
        '''
        lang_low = lang_long.lower() 
        # Example: ./CoNLL-X/train/data/arabic/PADT/train/arabic_PADT_train.conll
        # Example: ./CoNLL-X/test/data/arabic/PADT/test/arabic_PADT_test.conll
        # Example: ./CoNLL-X/test_blind/data/czech/pdt/test/czech_pdt_test_blind.conll
        train = os.path.join(data_dir, "train", "data", lang_low, treebank, "train", lang_low + "_" + treebank + "_train.conll")
        test = os.path.join(data_dir, "test", "data", lang_low, treebank, "test", lang_low + "_" + treebank + "_test.conll")
        test_blind  = os.path.join(data_dir, "test_blind", "data", lang_low, treebank, "test", lang_low + "_" + treebank + "_test_blind.conll")
        if has_dev: dev = os.path.join(data_dir, "train", "data", lang_low, treebank, "train", lang_low + "_" + treebank + "_dev.conll")
        # Set with setattr.
        p.set(lang_short + "_cx_train", train)
        p.set(lang_short + "_cx_test",   test)
        p.set(lang_short + "_cx_test_blind",  test_blind)
        if has_dev: p.set(lang_short + "_cx_dev", dev)
        # Set on dictionary.
        pl = p.langs[lang_short]
        pl.cx_train = train
        pl.cx_test = test
        pl.cx_test_blind = test_blind
        if has_dev: pl.cx_dev = dev
        # Require some paths.
        if require:
            require_path_exists(train, test, test_blind)
            if has_dev: require_path_exists(dev)
            
    def _set_paths_for_conll09_parses(self, p, lang_long, lang_short, data_dir, require=False): 
        #if lang_short == "es": lang_short = "sp"       
        # --- POS tags ---
        # Semi-supervised parser output: PHEAD column.
        pos_semi_train = safe_join(data_dir, "dmv_conll09-%s-train_20_True/test-parses.txt" % (lang_short))
        pos_semi_dev = safe_join(data_dir, "dmv_conll09-%s-dev_20_True/test-parses.txt" % (lang_short))
        pos_semi_eval = safe_join(data_dir, "dmv_conll09-%s-eval_20_True/test-parses.txt" % (lang_short))
        # Unsupervised parser output: PHEAD column.
        pos_unsup_train = safe_join(data_dir, "dmv_conll09-%s-train_20_False/test-parses.txt" % (lang_short))
        pos_unsup_dev = safe_join(data_dir, "dmv_conll09-%s-dev_20_False/test-parses.txt" % (lang_short))
        pos_unsup_eval = safe_join(data_dir, "dmv_conll09-%s-eval_20_False/test-parses.txt" % (lang_short))
        # --- Brown cluster tags ---
        # Semi-supervised parser output: PHEAD column.
        brown_semi_train = safe_join(data_dir, "dmv_conll09-%s-train-brown_20_True/test-parses.txt" % (lang_short))
        brown_semi_dev = safe_join(data_dir, "dmv_conll09-%s-dev-brown_20_True/test-parses.txt" % (lang_short))
        brown_semi_eval = safe_join(data_dir, "dmv_conll09-%s-eval-brown_20_True/test-parses.txt" % (lang_short))
        # Unsupervised parser output: PHEAD column.
        brown_unsup_train = safe_join(data_dir, "dmv_conll09-%s-train-brown_20_False/test-parses.txt" % (lang_short))
        brown_unsup_dev = safe_join(data_dir, "dmv_conll09-%s-dev-brown_20_False/test-parses.txt" % (lang_short))
        brown_unsup_eval = safe_join(data_dir, "dmv_conll09-%s-eval-brown_20_False/test-parses.txt" % (lang_short))
        if lang_short == "sp": lang_short = "es"       
        # Set with setattr.
        p.set(lang_short + "_pos_semi_train", pos_semi_train)
        p.set(lang_short + "_pos_semi_dev",   pos_semi_dev)
        p.set(lang_short + "_pos_semi_eval",  pos_semi_eval)
        p.set(lang_short + "_pos_unsup_train", pos_unsup_train)
        p.set(lang_short + "_pos_unsup_dev",   pos_unsup_dev)
        p.set(lang_short + "_pos_unsup_eval",  pos_unsup_eval)
        p.set(lang_short + "_brown_semi_train", brown_semi_train)
        p.set(lang_short + "_brown_semi_dev",   brown_semi_dev)
        p.set(lang_short + "_brown_semi_eval",  brown_semi_eval)
        p.set(lang_short + "_brown_unsup_train", brown_unsup_train)
        p.set(lang_short + "_brown_unsup_dev",   brown_unsup_dev)
        p.set(lang_short + "_brown_unsup_eval",  brown_unsup_eval)
        # Set on dictionary.
        pl = p.langs[lang_short]
        pl.pos_semi_train = pos_semi_train
        pl.pos_semi_dev = pos_semi_dev
        pl.pos_semi_eval = pos_semi_eval
        pl.pos_unsup_train = pos_unsup_train
        pl.pos_unsup_dev = pos_unsup_dev
        pl.pos_unsup_eval = pos_unsup_eval
        pl.brown_semi_train = brown_semi_train
        pl.brown_semi_dev = brown_semi_dev
        pl.brown_semi_eval = brown_semi_eval
        pl.brown_unsup_train = brown_unsup_train
        pl.brown_unsup_dev = brown_unsup_dev
        pl.brown_unsup_eval = brown_unsup_eval
        # Require some paths.
        if require:
            require_path_exists(pos_semi_train, pos_semi_dev, pos_semi_eval,
                                pos_unsup_train, pos_unsup_dev, pos_unsup_eval,
                                brown_semi_train, brown_semi_dev, brown_semi_eval,
                                brown_unsup_train, brown_unsup_dev, brown_unsup_eval)
