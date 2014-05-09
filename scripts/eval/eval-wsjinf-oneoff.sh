ROOT=/home/hltcoe/mgormley/working/parsing

# cd $ROOT/exp/srl-conll09_001/brown-unsup_en
# perl /home/hltcoe/mgormley/working/parsing/scripts/eval/eval09.pl -p  -g ./test-gold.txt -s ./test-pred.txt &> test-eval-no-punct.out

# cd $ROOT/exp/srl-conll09_001/brown-semi_en
# perl /home/hltcoe/mgormley/working/parsing/scripts/eval/eval09.pl -p  -g ./test-gold.txt -s ./test-pred.txt &> test-eval-no-punct.out

# cd $ROOT/exp/srl-conll09_001/pos-unsup_en
# perl /home/hltcoe/mgormley/working/parsing/scripts/eval/eval09.pl -p  -g ./test-gold.txt -s ./test-pred.txt &> test-eval-no-punct.out

# cd $ROOT/exp/srl-conll09_001/pos-semi_en
# perl /home/hltcoe/mgormley/working/parsing/scripts/eval/eval09.pl -p  -g ./test-gold.txt -s ./test-pred.txt &> test-eval-no-punct.out

# cd $ROOT/exp/srl-all-sup-lat_004/pos-sup_en_LATENT_True
# perl /home/hltcoe/mgormley/working/parsing/scripts/eval/eval09.pl -p  -g ./test-gold.txt -s ./test-pred.txt &> test-eval-no-punct.out

# STILL TODO: Marginalized (Coarse+IG)
#cd $ROOT/exp/srl-conll09_002/pos-sup_en/
#perl /home/hltcoe/mgormley/working/parsing/scripts/eval/eval09.pl -p  -g ./test-gold.txt -s ./test-pred.txt &> test-eval-no-punct.out


cd $ROOT
grep "Unlabeled attachment score" \
    $ROOT/exp/srl-conll09_001/*/test-eval-no-punct.out \
    $ROOT/exp/srl-all-sup-lat_004/pos-sup_en_LATENT_True/test-eval-no-punct.out \
    $ROOT/exp/srl-conll09_002/pos-sup_en/test-eval-no-punct.out


