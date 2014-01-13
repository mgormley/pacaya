# Reduces the vocabulary size of the Brown 256 run to a manageable size.
#

#set -x
#set -e

NUM_CLUSTERS=256

#for LANG in en
for LANG in ca cs de en es ja zh
do
    OUTPUT=./bc_out_$NUM_CLUSTERS/full.txt\_$LANG\_$NUM_CLUSTERS
    echo $OUTPUT
    date
    sort -n -k 3 $OUTPUT/paths | tail -n 300000 | sort > $OUTPUT/paths.cutoff
done
