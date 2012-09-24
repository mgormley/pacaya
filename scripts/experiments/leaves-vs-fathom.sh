cat exp/bnb_003/dmv_regret_100_3/stdout  | grep -P "Lazy.*Summary" | perl -pe "s/.*#leaves=(.*) #fathom=(.*)/\1 \2/" > out
cat exp/bnb_003/dmv_regret_100_5/stdout  | grep -P "DantzigWolfe.*Summary" | perl -pe "s/.*#lambdas=(.*) #cuts=(.*)/\1 \2/" > out
#cat exp/bnb_003/dmv_regret_100_3/stdout  | grep -P "DantzigWolfe.*Summary" | perl -pe "s/.*#lambdas=(.*) #cuts=(.*)/\1 \2/" > out

# echo '
# lvf <- read.table("out", col.names=c("leaves","fathom"))
# pdf("myplot.pdf")
# plot(lvf$fathom)
# dev.off()
# ' | R --no-save
