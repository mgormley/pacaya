#!/bin/bash
#

set -e

java -ea -Xmx1000m edu.jhu.data.conll.CoNLL09BrownTagger --brownClusters ../srl/src/main/resources/brown_clusters.es --train data/conll2009/CoNLL2009-ST-Spanish/CoNLL2009-ST-Spanish-development.txt --trainOut data/conll2009/CoNLL2009-ST-Spanish-BrownClusters/CoNLL2009-ST-Spanish-development.txt --maxTagLength 8

java -ea -Xmx1000m edu.jhu.data.conll.CoNLL09BrownTagger --brownClusters ../srl/src/main/resources/brown_clusters.es --train data/conll2009/CoNLL2009-ST-Spanish/CoNLL2009-ST-Spanish-train.txt --trainOut data/conll2009/CoNLL2009-ST-Spanish-BrownClusters/CoNLL2009-ST-Spanish-train.txt --maxTagLength 8

java -ea -Xmx1000m edu.jhu.data.conll.CoNLL09BrownTagger --brownClusters ../srl/src/main/resources/brown_clusters.es --train data/conll2009/CoNLL2009-ST-Spanish/CoNLL2009-ST-Spanish-trial.txt --trainOut data/conll2009/CoNLL2009-ST-Spanish-BrownClusters/CoNLL2009-ST-Spanish-trial.txt --maxTagLength 8

java -ea -Xmx1000m edu.jhu.data.conll.CoNLL09BrownTagger --brownClusters ../srl/src/main/resources/brown_clusters.es --train data/conll2009/CoNLL2009-ST-Spanish/CoNLL2009-ST-evaluation-Spanish.txt --trainOut data/conll2009/CoNLL2009-ST-Spanish-BrownClusters/CoNLL2009-ST-evaluation-Spanish.txt --maxTagLength 8
