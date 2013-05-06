#!/usr/bin/python
#

import os
from glob import glob
import re

wsj_dir = './data/treebank_3/wsj'
sym_dir = './data/treebank_3_sym/wsj'

if not os.path.exists(sym_dir):
    os.makedirs(sym_dir)
    
for subdir in glob('%s/*' % (wsj_dir)):
    if not os.path.isdir(subdir):
        continue
    subdir = os.path.basename(subdir)
    subdir_num = int(re.sub('^0', '', subdir))
    if int(subdir_num) < 2 or int(subdir_num) > 21:
        continue
    src = os.path.abspath(os.path.join(wsj_dir, subdir))
    dest = os.path.abspath(os.path.join(sym_dir, subdir))
    cmd = 'cp -r %s %s' % (src, dest) 
    print cmd
    os.system(cmd)
