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

## Read data
results.file = "/Users/mgormley/research/dep_parse/results/viterbi-vs-bnb/incumbent-status.data"
df <- read.table(results.file, header=TRUE)
df.orig <- df
df <- df[order(df$time),]

df$rltInitMax[which(is.na(df$rltInitMax))] <- as.numeric("+inf")
df$rltCutMax[which(is.na(df$rltCutMax))] <- as.numeric("+inf")
df$method <- str_c(df$relaxation, df$envelopeOnly, df$rltInitMax, df$varSelection, df$rltCutMax, sep=".")
## For ACL:
df$method <- ""
df$method <- ifelse(df$algorithm == "bnb" , str_c(df$method, "RLT"), df$method)
df$method <- ifelse(df$envelopeOnly == "True", str_c(df$method, " Conv.Env."), df$method)
df$method <- ifelse(df$rltFilter == "obj-var" & !(df$envelopeOnly == "True"), str_c(df$method, " Obj.Filter"), df$method)
##df$method <- ifelse(df$rltFilter == "max", str_c(df$method, " Max ", df$rltInitMax), df$method)
df$method <- ifelse(df$rltFilter == "max", str_c(df$method, " Max.", df$rltInitMax/1000, "k"), df$method)
df$method <- ifelse(df$algorithm == "viterbi", "Viterbi EM", df$method)

#df$method <- str_c(df$envelopeOnly, df$rltInitMax, sep=" / ")
methodDescription <- "Algorithm"

## OLD ACL method description:
##df$method <- ifelse(df$algorithm == "viterbi", "Viterbi EM", str_c("B&B", df$envelopeOnly, df$rltInitMax, sep=" / "))
##methodDescription <- "Algorithm /\nEnvelope Only / \nMax RLT cuts"

##df$method = str_c(df$algorithm, sep=".")
##df <- subset(df, algorithm == "viterbi" | algorithm == "bnb" )
##df <- subset(df, time/1000/60 < 61)
df <- subset(df, is.na(rltInitMax) | rltInitMax == 1000 | rltInitMax == 10000 | rltInitMax == 100001 | !is.finite(rltInitMax))
df <- subset(df, maxNumSentences == 200)
df <- subset(df,is.na(rltCutMax) | !is.finite(rltCutMax) | rltCutMax == 0 )

plotLogLikeVsTime <- function(mydata) {
  title = "Penn Treebank, Brown"
  xlab = "Time (min)"
  ylab = "Log-likelihood (train)"
  p <- ggplot(mydata, aes(x=time / 1000 / 60,
                          y=incumbentLogLikelihood, color=method))
  p <- p + geom_point(aes(shape=method))
  p <- p + geom_line(aes(linetype=universalPostCons))
  p <- p + xlab(xlab) + ylab(ylab) 
  ## For ACL: p <- p + opts(title=title)
  p <- p + scale_color_discrete(name=methodDescription) 
  p <- p + scale_shape_discrete(name=methodDescription)
  p <- p + scale_linetype_discrete(name="Posterior Constraints")
}

plotAccVsTime <- function(mydata) {
  title = "Penn Treebank, Brown"
  xlab = "Time (min)"
  ylab = "Accuracy (train)"
  p <- ggplot(mydata, aes(x=time / 1000 / 60,
                          y=incumbentAccuracy, color=method))
  p <- p + geom_point(aes(shape=method))
  p <- p + geom_line(aes(linetype=universalPostCons))
  ##p <- p + geom_smooth(aes(linetype=universalPostCons))
  p <- p + xlab(xlab) + ylab(ylab)
  ## For ACL: p <- p + opts(title=title)
  p <- p + scale_color_discrete(name=methodDescription)
  p <- p + scale_shape_discrete(name=methodDescription)
  p <- p + scale_linetype_discrete(name="Posterior Constraints")
}


myplot(plotLogLikeVsTime(subset(df, universalPostCons == "False")),
       str_c(results.file, "llvtime", "pdf", sep="."))

myplot(plotLogLikeVsTime(df),
       str_c(results.file, "llvtime", "pdf", sep="."))

myplot(plotAccVsTime(df),
       str_c(results.file, "accvtime", "pdf", sep="."))

max <- ddply(df, .(method, universalPostCons), function(x) max(x$incumbentLogLikelihood))
print(max[order(max$V1),])

myplot(plotAccVsTime(subset(df, method == "Viterbi EM" | method == "RLT Max 1000")),
       str_c(results.file, "accvtime", "pdf", sep="."))



## Read B&B status data.
results.file = "/Users/mgormley/research/dep_parse/results/viterbi-vs-bnb/results.data"
df <- read.table(results.file, header=TRUE)
df <- df[order(df$time),]


df$rltInitMax[which(is.na(df$rltInitMax))] <- as.numeric("+inf")
df$rltCutMax[which(is.na(df$rltCutMax))] <- as.numeric("+inf")
df$method <- str_c(df$relaxation, df$envelopeOnly, df$rltInitMax, df$varSelection, df$rltCutMax, sep=".")
## For ACL:
df$method <- ""
df$method <- ifelse(df$algorithm == "bnb" , str_c(df$method, "RLT"), df$method)
df$method <- ifelse(df$envelopeOnly == "True", str_c(df$method, " Conv.Env."), df$method)
df$method <- ifelse(df$rltFilter == "obj-var" & !(df$envelopeOnly == "True"), str_c(df$method, " Obj.Filter"), df$method)
##df$method <- ifelse(df$rltFilter == "max", str_c(df$method, " Max ", df$rltInitMax), df$method)
df$method <- ifelse(df$rltFilter == "max", str_c(df$method, " Max.", df$rltInitMax/1000, "k"), df$method)
df$method <- ifelse(df$algorithm == "viterbi", "Viterbi EM", df$method)

#df$method <- str_c(df$envelopeOnly, df$rltInitMax, sep=" / ")
methodDescription <- "Algorithm"

## OLD ACL method description:
##df$method <- ifelse(df$algorithm == "viterbi", "Viterbi EM", str_c("B&B", df$envelopeOnly, df$rltInitMax, sep=" / "))
##methodDescription <- "Algorithm /\nEnvelope Only / \nMax RLT cuts"

##df$method = str_c(df$algorithm, sep=".")
##df <- subset(df, algorithm == "viterbi" | algorithm == "bnb" )
##df <- subset(df, time/1000/60 < 61)
df <- subset(df, is.na(rltInitMax) | rltInitMax == 1000 | rltInitMax == 10000 | rltInitMax == 100001 | !is.finite(rltInitMax))
df <- subset(df, maxNumSentences == 200)
df <- subset(df,is.na(rltCutMax) | !is.finite(rltCutMax) | rltCutMax == 0 )


plotAcc <- function(mydata) {
  title = "Penn Treebank, Brown"
  xlab = "Time (min)"
  ylab = "Accuracy (train)"
  p <- ggplot(mydata, aes(x=method,
                          y=trainAccuracy, fill=universalPostCons))
  p <- p + geom_bar(position="dodge")
  ##p <- p + geom_line(aes(linetype=universalPostCons))
  ##p <- p + geom_smooth(aes(linetype=universalPostCons))
  p <- p + xlab(xlab) + ylab(ylab)
  ## For ACL: p <- p + opts(title=title)
  ##p <- p + scale_color_discrete(name=methodDescription)
  ##p <- p + scale_shape_discrete(name=methodDescription)
  p <- p + scale_fill_discrete(name="Posterior Constraints")
  p <- p + opts(axis.text.x = theme_text(angle = 90, hjust = 1))
}

myplot(plotAcc(df),
       str_c(results.file, "acc", "pdf", sep="."))



