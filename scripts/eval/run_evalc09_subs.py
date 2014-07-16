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
import platform
from glob import glob
import re
import random
from pypipeline.scrape import CsvResultsWriter
from pypipeline.scrape import get_all_following
from pypipeline.scrape import get_following
from pypipeline.scrape import get_following_literal

from pypipeline.util import get_new_file, sweep_mult, fancify_cmd, frange
from pypipeline.util import head_sentences
from pypipeline.experiment_runner import ExpParamsRunner, get_subset
from pypipeline import experiment_runner
from pypipeline import pipeline
from pypipeline.pipeline import write_script, RootStage, Stage
from experiments import run_srl

def get_root_dir():
    scripts_dir =  os.path.abspath(sys.path[0])
    root_dir =  os.path.dirname(os.path.dirname(scripts_dir))
    print "Using root_dir: " + root_dir
    return root_dir;

def get_code_name_map(ldc_dir):
    code_name_map = {}
    cn_file = os.path.join(ldc_dir, "data/eval-data/codes-names.lst")
    with open(cn_file) as f:
        lines = f.readlines()
        for l in lines:
            l = l.strip()
            code, name = l.split("\t")
            name = name.replace(" ", "_")
            code_name_map[code] = name                
    return code_name_map

class EvalC09(experiment_runner.PythonExpParams):

    def __init__(self, **keywords):
        experiment_runner.PythonExpParams.__init__(self,keywords)
    
    def get_instance(self):
        return EvalC09()
        
    def create_experiment_script(self, exp_dir):
        lang = self.get("lang")
        ldc_dir = self.get("ldc_dir")
        ood = self.get("ood")
        sub_dir = self.get("sub_dir")        
                        
        if ood:
            ood_str = "-ood"
        else:
            ood_str = "" 
            
        # Store for convenience during scraping.
        self.update(lang_ood=lang+ood_str)
        
        # Example:    
        # perl scripts/eval/eval09-no_sense.pl \ 
        #    -g data/conll2009/LDC2012T03/data/CoNLL2009-ST-Spanish/CoNLL2009-ST-evaluation-Spanish.txt \ 
        #    -s data/conll2009/LDC2012T03/data/eval-data/0c01/CoNLL2009-ST-evaluation-Spanish-Joint-closed.txt
        
        gold_file = "%s/data/CoNLL2009-ST-%s/CoNLL2009-ST-evaluation-%s%s.txt" % (ldc_dir, lang, lang, ood_str)
        if not os.path.exists(gold_file):
            #raise Exception("gold file doesn't exist %s" % (gold_file))
            print "WARN: gold file doesn't exist %s" % (gold_file)
        
        joint_file = "%s/CoNLL2009-ST-evaluation-%s-Joint-closed%s.txt" % (sub_dir, lang, ood_str)
        srl_only_file = "%s/CoNLL2009-ST-evaluation-%s-SRLonly-closed%s.txt" % (sub_dir, lang, ood_str)
        
        if os.path.exists(joint_file):
            pred_file = joint_file
        elif os.path.exists(srl_only_file):
            pred_file = srl_only_file
        else:
            pred_file = joint_file
            #raise Exception("predicted file doesn't exists joint_file=%s srl_only_file=%s" % (joint_file, srl_only_file))  
            print "WARN: predicted file doesn't exists joint_file=%s srl_only_file=%s" % (joint_file, srl_only_file)  
        
        script = "\n"
        if self.get("fast"):
            num_lines = 100
            script += "head -n %d %s > gold.txt\n" % (num_lines, gold_file)
            script += "head -n %d %s > pred.txt\n" % (num_lines, pred_file)
            gold_file = "gold.txt"
            pred_file =  "pred.txt"
        
        # Store these for use by get_eval_script().
        self.update(testGoldOut=gold_file, testPredOut=pred_file)
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
        
class CoNLL09EvalNoSense(ExpParamsRunner):
    
    def __init__(self, options):
        ExpParamsRunner.__init__(self, "evalc09", options.queue, print_to_console=True)
        self.options = options
        self.ldcT03 = os.path.abspath(options.ldcT03)
        self.ldcT04 = os.path.abspath(options.ldcT04)
        self.dry_run = options.dry_run    
        self.fast = options.fast   
    
    def get_experiments(self):          
        if not os.path.exists(self.ldcT03):
            print "Can't find T03:", self.ldcT03
        if not os.path.exists(self.ldcT04):
            print "Can't find T04:", self.ldcT04
        
        defaults = EvalC09()
        defaults.set_incl_name("ldc_dir", False)
        defaults.set_incl_name("sub_dir", False)
        defaults.set_incl_name("gold_file", False)
        defaults.set_incl_name("sys_file", False)
        
        # The code names are the same so just create one.
        self.code_name_map = get_code_name_map(self.ldcT03)
        print self.code_name_map
        
        # Out of Domain languages
        ood_langs = ["German", "English", "Czech"]
        ldcT03_langs =  ["Catalan", "Spanish", "German", "Czech"]
        ldcT04_langs = ["English", "Chinese"]
        all_langs = ldcT03_langs + ldcT04_langs
        
        exps = []
        for ood in [False, True]:
            for lang in all_langs:
                if ood and lang not in ood_langs: continue
                if lang in ldcT03_langs: ldc_dir = self.ldcT03
                if lang in ldcT04_langs: ldc_dir = self.ldcT04
                for sub_dir in glob(ldc_dir + "/data/eval-data/*"):                    
                    code = os.path.basename(sub_dir)
                    if code not in self.code_name_map:
                        continue
                    name = self.code_name_map[code]
                    
                    exp = defaults + EvalC09(lang=lang, ldc_dir=ldc_dir, ood=ood, sub_dir=sub_dir, fast=self.fast)
                    exp.update(code=code, name=name)
                    exp.update(ldc_dir_short=os.path.basename(ldc_dir))
                    exp.update(sub_dir_short=os.path.basename(sub_dir))
                    exp.code_name_map = self.code_name_map
                    exps.append(exp)
            
        if self.fast:
            random.shuffle(exps)
            exps = exps[:40]
        root = RootStage()
        root.add_dependents(exps)         
        scrape = run_srl.ScrapeSrl(csv_file="results.csv", tsv_file="results.data")
        scrape.add_prereqs(root.dependents)
        
        return root
        
if __name__ == "__main__":
    usage = "%prog "

    parser = OptionParser(usage=usage)
    parser.add_option('--ldcT03', default="./data/conll2009/LDC2012T03", help="path to LDC2012T03")
    parser.add_option('--ldcT04', default="./data/conll2009/LDC2012T04", help="path to LDC2012T04")
    #
    parser.add_option('-q', '--queue', help="Which SGE queue to use")
    parser.add_option('-f', '--fast', action="store_true", help="Run a fast version")
    parser.add_option('-n', '--dry_run',  action="store_true", help="Whether to just do a dry run.")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) != 1:
        parser.print_help()
        sys.exit(1)
    
    runner = CoNLL09EvalNoSense(options) 
    root_stage = runner.get_experiments()
    runner.run_pipeline(root_stage)




