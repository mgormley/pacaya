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
from experiments.core.scrape import CsvResultsWriter
from experiments.core.scrape import get_all_following
from experiments.core.scrape import get_following
from experiments.core.scrape import get_following_literal

def get_root_dir():
    scripts_dir =  os.path.abspath(sys.path[0])
    root_dir =  os.path.dirname(os.path.dirname(scripts_dir))
    print "Using root_dir: " + root_dir
    return root_dir;

class CoNLL09EvalNoSense:
    
    def __init__(self, options):
        self.options = options
        self.csv_file = options.csv_file
        self.tmp_dir = os.path.join(options.tmp, "conll09_eval_no_sense") 
        self.ldcT03 = options.ldcT03
        self.ldcT04 = options.ldcT04
        self.dry_run = options.dry_run
            
    def get_code_name_map(self, ldc_dir):
        code_name_map = {}
        cn_file = os.path.join(ldc_dir, "data/eval-data/codes-names.lst")
        with open(cn_file) as f:
            lines = f.readlines()
            for l in lines:
                l = l.strip()
                code, name = l.split("\t")
                code_name_map[code] = name                
        return code_name_map
    
    def eval_conll09_submissions(self):            
        print "Using temp directory: ", self.tmp_dir
        if not os.path.exists(self.tmp_dir):
            os.makedirs(self.tmp_dir)
        if not os.path.exists(self.ldcT03):
            print "Can't find T03:", self.ldcT03
        if not os.path.exists(self.ldcT04):
            print "Can't find T04:", self.ldcT04
        
        # The code names are the same so just create one.
        self.code_name_map = self.get_code_name_map(self.ldcT03)
        print self.code_name_map
        
        # Out of Domain languages
        ood_langs = ["German", "English", "Chinese"]
        ldcT03_langs =  ["Catalan", "Spanish", "German", "Czech"]
        ldcT04_langs = ["English", "Chinese"]
        
        for ood in [False, True]:
            for lang in ldcT03_langs + ldcT04_langs:
                if ood and lang not in ood_langs: continue
                if lang in ldcT03_langs: ldc_dir = self.ldcT03
                if lang in ldcT04_langs: ldc_dir = self.ldcT04
                for sub_dir in glob(ldc_dir + "/data/eval-data/*"):
                    self.eval_one(lang, ldc_dir, ood, sub_dir)
            
        #csv_out = open(csv_file, 'w')
        #writer = CsvResultsWriter(csv_out)
        #writer.close()
        
    def eval_one(self, lang, ldc_dir, is_ood, sub_dir):
        code = os.path.basename(sub_dir)
        if code not in self.code_name_map:
            return
        name = self.code_name_map[code]
        
        if is_ood:
            ood = "-ood"
        else:
            ood = "" 
        
        # Example:    
        # perl scripts/eval/eval09-no_sense.pl \ 
        #    -g data/conll2009/LDC2012T03/data/CoNLL2009-ST-Spanish/CoNLL2009-ST-evaluation-Spanish.txt \ 
        #    -s data/conll2009/LDC2012T03/data/eval-data/0c01/CoNLL2009-ST-evaluation-Spanish-Joint-closed.txt
        
        gold_file = "%s/data/CoNLL2009-ST-%s/CoNLL2009-ST-evaluation-%s%s.txt" % (ldc_dir, lang, lang, ood)
        sys_file = "%s/data/eval-data/0c01/CoNLL2009-ST-evaluation-%s-Joint-closed%s.txt" % (ldc_dir, lang, ood)
        cmd = "perl scripts/eval/eval09-no_sense.pl -g %s -s %s" % (gold_file, sys_file)
        
        eval_file = os.path.join(self.tmp_dir, name, "eval-%s%s.txt" % (lang, ood))
        if not os.path.exists(os.path.dirname(eval_file)):
            os.makedirs(os.path.dirname(eval_file))
        cmd += " > '%s'" % (eval_file)
        
        print cmd
        if self.dry_run:
            return
        
        os.system(cmd)
        
        #with open(eval_file, 'r') as eval_in:
        #    lines = eval_in.readlines()
        #    labeled_f1 = get_following("")
            
            
        
        

if __name__ == "__main__":
    usage = "%prog "

    parser = OptionParser(usage=usage)
    parser.add_option('--csv_file', default="out.csv", help="Out file for CSV")
    parser.add_option('--ldcT03', default="./data/conll2009/LDC2012T03", help="path to LDC2012T03")
    parser.add_option('--ldcT04', default="./data/conll2009/LDC2012T04", help="path to LDC2012T04")
    parser.add_option('--tmp', default="./tmp",  help="Temp directory")
    parser.add_option('-n', '--dry_run', action="store_true",  help="Temp directory")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) != 1:
        parser.print_help()
        sys.exit(1)
    
    tmp_dir = options.tmp
    eval = CoNLL09EvalNoSense(options) 
    eval.eval_conll09_submissions()


