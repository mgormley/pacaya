## Setup
library(ggplot2)
library(plyr)
library(stringr)

stdwidth <- 4.7
stdheight <- 3.4
quartz(width=stdwidth,height=stdheight)

myplot <- function(p, filename) {
  p <- p + theme_bw(12, base_family="serif")
  print(p)
  ggsave(filename, width=stdwidth, height=stdheight)
}

getDataset <- function(mydata) {
  colsubset <- df[,c("dataset", "maxNumSentences", "maxSentenceLength")]
  unique(str_c(df$dataset, df$maxNumSentences, df$maxSentenceLength, sep="."))
}


## Read B&B status data.
results.file = "/Users/mgormley/research/dep_parse/results/viterbi-vs-bnb/bnb-status.data"
df <- read.table(results.file, header=TRUE)
df <- df[order(df$time),]

print("Adding columns.")
# If using rltFilter="prop":
#  df$method = str_c(df$relaxation, df$envelopeOnly, df$rltInitProp, df$varSelection, df$rltCutProp, sep=".")
# If using rltFilter="max"
df$method = str_c(df$relaxation, df$envelopeOnly, df$rltInitMax, df$varSelection, df$rltCutMax, sep=".")

dfLow <- df
dfLow$bound <- df$lowBound
dfLow$boundType <- "lower"

dfUp <- df
dfUp$bound <- df$upBound
dfUp$boundType <- "upper"

dfBoth <- rbind(dfUp, dfLow)

dfBoth.subset <- subset(dfBoth, bound != -Inf & bound > -2000)
##dfBoth.subset <- dfBoth

plotbnbboundsvnodes <- function(mydata) {
  title = str_c(getDataset(mydata), unique(df$offsetProb), sep=".")
  xlab = "Number of nodes processed"
  ylab = "Bounds on log-likelihood"
  p <- ggplot(mydata, aes(x=numSeen, y=bound, color=factor(method)))
  p <- p + geom_line(aes(linetype=boundType))
  #p <- p + geom_point(aes(shape=boundType))
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_linetype_discrete(name="Bound type")
  p <- p + scale_color_discrete(name="Proportion supervised")
}
myplot(plotbnbboundsvnodes(dfBoth.subset), str_c(results.file, "ul-bounds-node", "pdf", sep="."))

plotbnbboundsvtime <- function(mydata) {
  title = str_c(getDataset(mydata), unique(df$offsetProb), sep=".")
  xlab = "Time (minutes)"
  ylab = "Bounds on log-likelihood"
  p <- ggplot(mydata, aes(x=time / 1000 / 60, y=bound, color=factor(method)))
  p <- p + geom_line(aes(linetype=boundType))
  #p <- p + geom_point(aes(shape=boundType))
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_linetype_discrete(name="Bound type")
  p <- p + scale_color_discrete(name="Proportion supervised")
}
myplot(plotbnbboundsvtime(dfBoth.subset), str_c(results.file, "ul-bounds-time", "pdf", sep="."))


plotnumfathom <- function(mydata) {
  title = str_c(getDataset(mydata), unique(df$offsetProb), sep=".")
  xlab = "Time (minutes)"
  ylab = "# fathomed / # processed"
  p <- ggplot(mydata, aes(x=time / 1000 / 60, y=(numFathom / numSeen), color=factor(propSupervised)))
  p <- p + geom_line()
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_color_discrete(name="Proportion supervised")
}
myplot(plotnumfathom(dfBoth), str_c(results.file, "fathom-rate", "pdf", sep="."))


plotnumtrees <- function(mydata) {
  title = str_c(getDataset(mydata), unique(df$offsetProb), sep=".")
  xlab = "Time (minutes)"
  ylab = "# trees"
  p <- ggplot(mydata, aes(x=time / 1000 / 60, y=num, color=factor(propSupervised)))
  p <- p + geom_line()
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_color_discrete(name="Proportion supervised")
}
myplot(plotnumfathom(dfBoth), str_c(results.file, "fathom-rate", "pdf", sep="."))


## Read B&B status data. --- Below trying to make bar charts ---
results.file = "/Users/mgormley/research/parsing/results/viterbi-vs-bnb/results.data"
df <- read.table(results.file, header=TRUE)

df <- prepData(df)

plotAcc <- function(mydata) {
  title = "Penn Treebank, Brown"
  xlab = "Time (min)"
  ylab = "Accuracy (train)"
  p <- ggplot(mydata, aes(x=method,
                          y=trainAccuracy,
                          fill=universalPostCons,
                          linetype=isGlobal))
  p <- p + geom_point() ##position="dodge")
  ##p <- p + geom_line(aes(linetype=universalPostCons))
  ##p <- p + geom_smooth(aes(linetype=universalPostCons))
  p <- p + xlab(xlab) + ylab(ylab)
  p <- p + scale_y_continuous(limits=c(0.5, 0.7))
  ## For ACL: p <- p + opts(title=title)
  ##p <- p + scale_color_discrete(name=methodDescription)
  ##p <- p + scale_shape_discrete(name=methodDescription)
  p <- p + scale_fill_discrete(name="Posterior Constraints")
  p <- p + opts(axis.text.x = theme_text(angle = 90, hjust = 1))
  p <- p + geom_text(aes(label=trainAccuracy), vjust=0.0,hjust=0.5)
}

p <- plotAcc(subset(df, universalPostCons=="True"))
filename <- str_c(results.file, "acc", "pdf", sep=".")
print(p)

  p <- p + theme_bw(12, base_family="serif") + opts(axis.text.x = theme_text(angle = -90, hjust = 1))
  print(p)
  ggsave(filename, width=stdwidth, height=stdheight)

write.table(df[,c("method", "universalPostCons", "trainAccuracy")], file=str_c(results.file, "acc", "csv", sep="."), sep=",")




