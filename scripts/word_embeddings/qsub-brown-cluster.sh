# Runs brown clustering on the grid.
#
# To fix the long word in spanish which was causing a SegFault:
#
# sed -i".with_tintin" '/Methionylthreonylthreonylglutaminylarginyltyrosylglutamylserylleucylphenylalanylalanylglutaminylleuc/d' ./wikitxt/es/full.txt 
# sed -i".with_tintin" '/uaglfukudhslukfjhsdkjfhsadkcbansdjcbdjsbckjsbhlhfehlihflcihwfihdleihfhiewifhehaflihafihefihafdhsfhlafdheihaficebalihfqalihfdhdeihqafiwaehfdhsfihhlhaeflhsfduaglfukudhslukfjhsdkjfhsadkcbansdjcbdjsbckjsb/d' ./wikitxt/es/full.txt 
#
# Observed memory / time requirements:
# LANG    NUM_CLUSTERS   MEM    TIME    MIN_OCCUR
# en      256            29G    146 hr  1
# ca      256            2.1G   20 hr   1
# de      256            12G    ?       1
#
# en      256            ?      38 hr   2
# en      1000           ?      38 hr   2

#set -x
#set -e

NUM_CLUSTERS=1000
MEM=33G
let "SECONDS = 300 * 3600"

#for LANG in en
for LANG in ca cs de en es ja zh
do
    if [ $LANG == "en" ] ;
    then MEM=33G
    else MEM=16G
    fi
	
    INPUT=./wikitxt/$LANG/full.txt
    OUTPUT=./bc_out_$NUM_CLUSTERS/full.txt\_$LANG\_$NUM_CLUSTERS
    # cleaning:
    rm -rv $INPUT.int $INPUT.strdb $OUTPUT
    mkdir -p $OUTPUT

    CMD="echo 'Starting'"
    echo $CMD >> $OUTPUT/cmd.sh
    CMD="python ../parsing/scripts/data/unigrams.py --in_text $INPUT --out_counts $OUTPUT/unigram.counts --out_words $OUTPUT/unigram.words --term_limit 300000 &> python.log"
    echo $CMD >> $OUTPUT/cmd.sh
    CMD="./brown-cluster/wcluster --text $INPUT --c $NUM_CLUSTERS --max-ind-level 3 --output_dir $OUTPUT/bc --min-occur 2 --restrict $OUTPUT/unigram.words"
    echo $CMD >> $OUTPUT/cmd.sh

    cat $OUTPUT/cmd.sh

    chmod u+x $OUTPUT/cmd.sh
    QS="qsub -cwd -j y -b y -q text.q -q all.q -l mem_free=$MEM,num_proc=1,h_rt=$SECONDS -N $LANG-wcluster  -V -e stderr -o $OUTPUT/qsub.log '$OUTPUT/cmd.sh'"
    #\"bash '$OUTPUT/cmd.sh'\""
    echo $QS
    $QS
done


# usage: ./brown-cluster/wcluster
#   chk                 : Check data structures are valid (expensive). [false]
#   stats               : Just print out stats. [false]
#   paths2map           : Take the paths file and generate a map file. [false]
#   ncollocs      <int> : Collocations with most mutual information (output). [500]
#   c             <int> : Number of clusters. [1000]
#   plen          <int> : Maximum length of a phrase to consider. [1]
#   min-occur     <int> : Keep phrases that occur at least this many times. [1]
#   rand          <int> : Number to call srand with. [-780672246]
#   max-ind-level <int> : Maximum indent level for logging [3]
#   ms-per-line   <int> : Print a line out every this many milliseconds [0]
#   output_dir    <str> : Output everything to this directory. []
#   text          <str> : Text file with corpora (input). []
#   restrict      <str> : Only consider words that appear in this text (input). []
#   paths         <str> : File containing root-to-node paths in the clustering tree (input/output). []
#   map           <str> : File containing lots of good information about each phrase, more general than paths (output) []
#   collocs       <str> : Collocations with most mutual information (output). []
#   featvec       <str> : Feature vectors (output). []
#   comment       <str> : Description of this run. []
#   log           <str> : File to write log to ("" for stdout) []
