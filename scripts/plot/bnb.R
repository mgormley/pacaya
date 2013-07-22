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
results.file = "/Users/mgormley/research/parsing/results/bnb/bnb-status.data"
##results.file = "/Users/mgormley/research/parsing/results/bnb/bnb-status.data.5sent"
##results.file = "/Users/mgormley/research/parsing/results/bnb/bnb-status.data.8sent"
df <- read.table(results.file, header=TRUE)
df <- df[order(df$time),]

print("Adding columns.")
# If using rltFilter="prop":
#  df$method = str_c(df$relaxation, df$envelopeOnly, df$rltInitProp, df$varSelection, df$rltCutProp, sep=".")
# If using rltFilter="max"
df$method = str_c(df$relaxation, df$envelopeOnly, df$rltInitMax, df$varSelection, df$rltCutMax, sep=".")
## For ACL:
df$method <- ""
df$method <- ifelse(df$algorithm == "bnb" , str_c(df$method, "RLT"), df$method)
df$method <- ifelse(df$envelopeOnly == "True", str_c(df$method, " Max.0k"), df$method)
df$method <- ifelse(df$rltFilter == "obj-var" & !(df$envelopeOnly == "True"), str_c(df$method, " Obj.Filter"), df$method)
##df$method <- ifelse(df$rltFilter == "max", str_c(df$method, " Max.", df$rltInitMax), df$method)
df$method <- ifelse(df$rltFilter == "max", str_c(df$method, " Max.", df$rltInitMax/1000, "k"), df$method)
df$method <- ifelse(df$algorithm == "viterbi", "Viterbi EM", df$method)

df$method <- factor(df$method, levels = c("Viterbi EM", "RLT Obj.Filter", "RLT Max.0k","RLT Max.1k", "RLT Max.10k", "RLT Max.100k"))

methodDescription <- "Relaxation"


df <- subset(df, rltCutMax == 0 | is.na(rltCutMax))

dfLow <- df
dfLow$bound <- df$lowBound
dfLow$boundType <- "lower"

dfUp <- df
dfUp$bound <- df$upBound
dfUp$boundType <- "upper"

dfBoth <- rbind(dfUp, dfLow)

dfBoth.subset <- subset(dfBoth, dataset == "alt-three"
                        & bound != -Inf)


max <- ddply(df, .(method), function(x){ x[which.max(x$numSeen),]})
print(max[,c("method", "numSeen")])
print(max[order(max$V1),])

plotbnbboundsvtime <- function(mydata) {
  ## For ACL:
  ##mydata$rltInitMax[which(is.na(mydata$rltInitMax))] <- as.numeric("+inf")
  ##mydata$method = str_c(mydata$envelopeOnly, mydata$rltInitMax, sep=" / ")
  mydata <- subset(mydata, is.na(rltInitMax) | rltInitMax == 1000 | rltInitMax == 10000 | !is.finite(rltInitMax))

  ##title = str_c(getDataset(mydata), unique(df$offsetProb), sep=".")
  xlab = "Time (sec)"
  ylab = "Bounds on log-likelihood"
  p <- ggplot(mydata, aes(x=time / 1000, y=bound, color=factor(method)))
  p <- p + geom_line(aes(linetype=boundType))
  p <- p + geom_point(data=subset(mydata, (numSeen %% 200 == 1)), aes(shape=method))
  p <- p + xlab(xlab) + ylab(ylab) ##+ opts(title=title)
  p <- p + scale_linetype_discrete(name="Bound type")
  p <- p + scale_color_discrete(name=methodDescription) 
  p <- p + scale_shape_discrete(name=methodDescription)
  p <- p + scale_x_log10()
}
myplot(plotbnbboundsvtime(dfBoth.subset), str_c(results.file, "ul-bounds-time", "pdf", sep="."))
dfBoth.subset.temp <- subset(dfBoth.subset, is.na(rltInitMax) | rltInitMax == 1000 | rltInitMax == 10000 | !is.finite(rltInitMax))
myplot(plotbnbboundsvtime(dfBoth.subset.temp), str_c(results.file, "ul-bounds-time", "pdf", sep="."))

plotbnbboundsvnodes <- function(mydata) {
  title = "" ##str_c(getDataset(mydata), unique(df$offsetProb), sep=".")
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
myplot(plotbnbboundsvnodes(dfBoth.subset.temp), str_c(results.file, "ul-bounds-node", "pdf", sep="."))

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
  title = "" ##str_c(getDataset(mydata), unique(df$offsetProb), sep=".")
  xlab = "Time (minutes)"
  ylab = "# fathomed / # processed"
  p <- ggplot(mydata, aes(x=time / 1000 / 60, y=(numFathom / numSeen), color=factor(method)))
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



