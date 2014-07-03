set -x 
set -e

ROOT=/home/hltcoe/mgormley/working/pacaya2

# cd $ROOT/exp/srl-conll09_001/brown-unsup_en
# perl /home/hltcoe/mgormley/working/parsing/scripts/eval/eval09.pl -p  -g ./test-gold.txt -s ./test-pred.txt &> test-eval-no-punct.out

# cd $ROOT/exp/srl-conll09_001/brown-semi_en
# perl /home/hltcoe/mgormley/working/parsing/scripts/eval/eval09.pl -p  -g ./test-gold.txt -s ./test-pred.txt &> test-eval-no-punct.out

# cd $ROOT/exp/srl-conll09_001/pos-unsup_en
# perl /home/hltcoe/mgormley/working/parsing/scripts/eval/eval09.pl -p  -g ./test-gold.txt -s ./test-pred.txt &> test-eval-no-punct.out

# cd $ROOT/exp/srl-conll09_001/pos-semi_en
# perl /home/hltcoe/mgormley/working/parsing/scripts/eval/eval09.pl -p  -g ./test-gold.txt -s ./test-pred.txt &> test-eval-no-punct.out

# OLD: cd $ROOT/exp/srl-all-sup-lat_004/pos-sup_en_LATENT_True
#cd $ROOT/exp/srl-conll09_003/pos-sup_tpl_coarse1_LATENT_True
#perl /home/hltcoe/mgormley/working/parsing/scripts/eval/eval09.pl -p  -g ./test-gold.txt -s ./test-pred.txt &> test-eval-no-punct.out

# OLD: cd $ROOT/exp/srl-conll09_002/pos-sup_en/
cd $ROOT/exp/srl-conll09_003/pos-sup_tpl_bjork_LATENT_True_FASTER
perl /home/hltcoe/mgormley/working/parsing/scripts/eval/eval09.pl -p  -g ./test-gold.txt -s ./test-pred.txt &> test-eval-no-punct.out

cd $ROOT
grep "Unlabeled attachment score" \
    $ROOT/exp/srl-conll09_001/*/test-eval-no-punct.out \
    $ROOT/exp/srl-conll09_003/*/test-eval-no-punct.out 

#OLD GREP:
#$ROOT/exp/srl-all-sup-lat_004/pos-sup_en_LATENT_True/test-eval-no-punct.out \
#$ROOT/exp/srl-conll09_002/pos-sup_en/test-eval-no-punct.out


