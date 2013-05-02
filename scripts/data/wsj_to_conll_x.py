#!/usr/bin/python
#
# Convert WSJ data to CoNLL-X format.
#

import os
import sys

java = "java -ea -Xmx512m edu.jhu.hltcoe.data.conll.Ptb2ConllX "

cmds = []
cmds += ["mkdir data/conllx"]
# Fine tags.
cmds += [java + "--ptbIn data/treebank_3/wsj/22 --conllxOut data/conllx/english_ptb-wsj-len10_dev.conll --maxSentenceLength 10"]
cmds += [java + "--ptbIn data/treebank_3/wsj/22 --conllxOut data/conllx/english_ptb-wsj_dev.conll"]
cmds += [java + "--ptbIn data/treebank_3/wsj/23 --conllxOut data/conllx/english_ptb-wsj-len10_test.conll --maxSentenceLength 10"]
cmds += [java + "--ptbIn data/treebank_3/wsj/23 --conllxOut data/conllx/english_ptb-wsj_test.conll"]
cmds += [java + "--ptbIn data/treebank_3_sym/wsj --conllxOut data/conllx/english_ptb-wsj-len10_train.conll --maxSentenceLength 10"]
cmds += [java + "--ptbIn data/treebank_3_sym/wsj --conllxOut data/conllx/english_ptb-wsj_train.conll"]
# Coarse tags.
cmds += [java + "--ptbIn data/treebank_3/wsj/22 --conllxOut data/conllx/english_ptb-wsj-coarse-len10_dev.conll --maxSentenceLength 10 --reduceTags data/universal_pos_tags.1.02/en-ptb.map"]
cmds += [java + "--ptbIn data/treebank_3/wsj/22 --conllxOut data/conllx/english_ptb-wsj-coarse_dev.conll --reduceTags data/universal_pos_tags.1.02/en-ptb.map"]
cmds += [java + "--ptbIn data/treebank_3/wsj/23 --conllxOut data/conllx/english_ptb-wsj-coarse-len10_test.conll --maxSentenceLength 10 --reduceTags data/universal_pos_tags.1.02/en-ptb.map"]
cmds += [java + "--ptbIn data/treebank_3/wsj/23 --conllxOut data/conllx/english_ptb-wsj-coarse_test.conll --reduceTags data/universal_pos_tags.1.02/en-ptb.map"]
cmds += [java + "--ptbIn data/treebank_3_sym/wsj --conllxOut data/conllx/english_ptb-wsj-coarse-len10_train.conll --maxSentenceLength 10 --reduceTags data/universal_pos_tags.1.02/en-ptb.map"]
cmds += [java + "--ptbIn data/treebank_3_sym/wsj --conllxOut data/conllx/english_ptb-wsj-coarse_train.conll --reduceTags data/universal_pos_tags.1.02/en-ptb.map"]

for cmd in cmds:
    print cmd
    os.system(cmd)
