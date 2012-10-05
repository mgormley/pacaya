## Setup
library(ggplot2)
library(plyr)
library(stringr)

stdwidth <- 4.7
stdheight <- 3.4
quartz(width=stdwidth,height=stdheight)

## Read data
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/bnb_semi/bnb_016.data"

df <- read.table(results.file, header=TRUE)

df$bnbStatusShort <- str_replace(df$bnbStatus, "_SOLUTION_FOUND", "")

#df <- subset(df, varSplit == "half-prob" & maxNumSentences == 300)
df$groupLevel <- factor(str_c(df$maxNumSentences, df$maxSentenceLength, df$varSplit, sep="."))
groups <- daply(df, .(groupLevel), function(dat) { return(dat) })

getDataset <- function(mydata) {
  colsubset <- df[,c("dataset", "maxNumSentences", "maxSentenceLength")]
  unique(str_c(df$dataset, df$maxNumSentences, df$maxSentenceLength, sep="."))
}

myplot <- function(p, filename) {
  p <- p + theme_bw(12, base_family="serif")
  print(p)
  ggsave(filename, width=stdwidth, height=stdheight)
}

plotreldiff <- function(mydata) {
  xlab = "Proportion of training data supervised"
  ylab = "Relative difference"
  p <- qplot(propSupervised, relativeDiff, data=mydata,
             color=factor(offsetProb), shape=factor(bnbStatusShort),
             geom="point", xlab=xlab, ylab=ylab) +
               scale_color_discrete(name="Hypercube width/2") +
                 opts(axis.text.x=theme_text(angle=70, hjust=1.0))
  p <- p + scale_shape_discrete(name="B&B Status")
  p <- p + scale_color_discrete(name="Hypercube width/2")
}

plotaccuracy <- function(mydata) {
  xlab = "Proportion of training data supervised"
  ylab = "Train Accuracy"
  p <- qplot(propSupervised, trainAccuracy, data=mydata,
             color=factor(offsetProb), shape=factor(bnbStatusShort),
             geom="point", xlab=xlab, ylab=ylab) +
               scale_color_discrete(name="Hypercube width/2") +
                 opts(axis.text.x=theme_text(angle=70, hjust=1.0))
  p <- p + scale_shape_discrete(name="B&B Status")
  p <- p + scale_color_discrete(name="Hypercube width/2")
}

plotspace <- function(mydata) {
  xlab = "Proportion of training data supervised"
  ylab = "Proportion of root space remaining"
  p <- qplot(propSupervised, propRootSpaceRemain, data=mydata,
             color=factor(offsetProb), shape=factor(groupLevel),
             geom="point", xlab=xlab, ylab=ylab) +
               opts(axis.text.x=theme_text(angle=70, hjust=1.0))
  p <- p + scale_shape_discrete(name="Dataset")
  p <- p + scale_color_discrete(name="Hypercube width/2")
}

plotlikelihood <- function(mydata) {
  xlab = "Proportion of training data supervised"
  ylab = "Log-likelihood"
  p <- qplot(propSupervised, trainLogLikelihood, data=mydata,
             color=factor(offsetProb), shape=factor(groupLevel),
             geom="point", xlab=xlab, ylab=ylab) +
               scale_color_discrete(name="Hypercube width/2") +
                 opts(axis.text.x=theme_text(angle=70, hjust=1.0)) +
                   geom_smooth(aes(color=factor(offsetProb)), size=0.5, se=FALSE)
  p <- p + scale_shape_discrete(name="Dataset")
  p <- p + scale_color_discrete(name="Hypercube width/2")
}

plotvarsplit <- function(mydata) {
  xlab = "Variable splitting strategy"
  ylab = "Proportion of root space remaining"
  qplot(factor(varSplit), propRootSpaceRemain, data=mydata, geom="boxplot",
        xlab=xlab, ylab=ylab)
}

plotnumfathom <- function(mydata) {
  xlab = "Proportion of training data supervised"
  ylab = "Number of fathomed nodes"
  p <- qplot(factor(propSupervised), numFathom, data=mydata,
             color=factor(offsetProb), shape=factor(groupLevel),
             geom="point", xlab=xlab, ylab=ylab)
  p <- p + scale_shape_discrete(name="Dataset")
  p <- p + scale_color_discrete(name="Hypercube width/2")
}

myplot(ggplot(df, aes(factor(df$bnbStatusShort))) + geom_bar(),
       str_c(results.file, "all", "bnbStatus", "pdf", sep="."))

myplot(plotvarsplit(df),
       str_c(results.file, "all", "varSplit", "pdf", sep="."))

for(dfsubset in groups) {
  print(head(dfsubset$groupLevel, n=1))
  print(nrow(dfsubset))

  groupLevel <-  as.character(dfsubset$groupLevel[1])
  
  myplot(plotnumfathom(dfsubset),
         str_c(results.file, groupLevel, "numFathom", "pdf", sep="."))
  
  myplot(plotreldiff(dfsubset),
         str_c(results.file, groupLevel, "reldiff", "pdf", sep="."))
  
  myplot(plotaccuracy(dfsubset),
         str_c(results.file, groupLevel, "accuracy", "pdf", sep="."))
  
  myplot(plotlikelihood(dfsubset),
         str_c(results.file, groupLevel, "likelihood", "pdf", sep="."))
  
  myplot(plotspace(dfsubset),
         str_c(results.file, groupLevel, "space", "pdf", sep="."))
}



## Read data
##results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/bnb_semi/bnb_016_status_0.5_0.5.data"
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/bnb_semi/bnb_016_status.data"
df <- read.table(results.file, header=TRUE)
df <- df[order(df$time),]

dfLow <- df
dfLow$bound <- df$lowBound
dfLow$boundType <- "lower"

dfUp <- df
dfUp$bound <- df$upBound
dfUp$boundType <- "upper"

dfBoth <- rbind(dfUp, dfLow)
dfBoth <- subset(dfBoth,
                 propSupervised == 0.0 |
                 propSupervised == 0.2 |
                 propSupervised == 0.4 |
                 propSupervised == 0.6 |
                 propSupervised == 0.8)
##dfBoth <- subset(dfBoth, propSupervised == 0.9 & time < 2e5)
## | propSupervised == 0.8 | propSupervised == 0.7)


plotbnbbounds <- function(mydata) {
  title = str_c(getDataset(mydata), unique(df$offsetProb), sep=".")
  xlab = "Number of nodes processed"
  ylab = "Bounds on log-likelihood"
  p <- ggplot(mydata, aes(x=numSeen, y=bound, color=factor(propSupervised)))
  p <- p + geom_line(aes(linetype=boundType))
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_linetype_discrete(name="Bound type")
  p <- p + scale_color_discrete(name="Proportion supervised")
}
myplot(plotbnbbounds(dfBoth), str_c(results.file, "ul-bounds", "pdf", sep="."))

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




## Read data
##results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/bnb_semi/bnb_016_status_0.5_0.5.data"
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/viterbi-em/results.data"
df <- read.table(results.file, header=TRUE)

plotaccuracyvslikelihood <- function(mydata) {
  title = str_c(getDataset(mydata), unique(df$offsetProb), sep=".")
  xlab = "Train Log-likelihood"
  ylab = "Train Accuracy"
  p <- ggplot(mydata, aes(x=trainLogLikelihood, y=trainAccuracy, color=initWeights))
  p <- p + geom_point()
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
}
myplot(plotaccuracyvslikelihood(df), str_c(results.file, "accvlike", "pdf", sep="."))
