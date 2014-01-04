
TYPE=train
MNS=2000

for LANG in  English Chinese 
do
    echo $LANG
    java edu.jhu.eval.SrlEdgeEvaluator --train data/conll2009/LDC2012T04/data/CoNLL2009-ST-$LANG/CoNLL2009-ST-$LANG-$TYPE.txt --trainType CONLL_2009 --maxNumSentences $MNS
done

for LANG in Spanish Catalan German Czech
do
    echo $LANG
    java edu.jhu.eval.SrlEdgeEvaluator --train data/conll2009/LDC2012T03/data/CoNLL2009-ST-$LANG/CoNLL2009-ST-$LANG-$TYPE.txt --trainType CONLL_2009  --maxNumSentences $MNS
done

