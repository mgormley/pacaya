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

#df <- subset(df, varSplit == "half-prob" & maxNumSentences == 300)
df$groupLevel <- factor(do.call(paste, c(df[c("maxNumSentences",
                                                "maxSentenceLength",
                                                "varSplit")], sep = ".")))
##df$groupLevel <- factor(str_c(df$maxNumSentences, df$maxSentenceLength, df$varSplit, sep="."))
df$bnbStatusShort <- str_replace(df$bnbStatus, "_SOLUTION_FOUND", "")

groups <- daply(df, .(groupLevel), function(dat) { return(dat) })

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
       paste(c(results.file, "all", "bnbStatus", "pdf"), collapse = "."))

myplot(plotvarsplit(df),
       paste(c(results.file, "all", "varSplit", "pdf"), collapse = "."))

for(dfsubset in groups) {
  print(head(dfsubset$groupLevel, n=1))
  print(nrow(dfsubset))

  groupLevel <-  as.character(dfsubset$groupLevel[1])
  
  myplot(plotnumfathom(dfsubset),
         paste(c(results.file, groupLevel, "numFathom", "pdf"), collapse = "."))
  
  myplot(plotreldiff(dfsubset),
         paste(c(results.file, groupLevel, "reldiff", "pdf"), collapse = "."))
  
  myplot(plotaccuracy(dfsubset),
         paste(c(results.file, groupLevel, "accuracy", "pdf"), collapse = "."))
  
  myplot(plotlikelihood(dfsubset),
         paste(c(results.file, groupLevel, "likelihood", "pdf"), collapse = "."))
  
  myplot(plotspace(dfsubset),
         paste(c(results.file, groupLevel, "space", "pdf"), collapse = "."))
}



## Read data
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/bnb_semi/bnb_016_status_0.5_0.5.data"
##results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/bnb_semi/bnb_016_status.data"
df <- read.table(results.file, header=TRUE)
df <- df[order(df$time),]

dfLow <- df
dfLow$bound <- df$lowBound
dfLow$boundType <- "lower"

dfUp <- df
dfUp$bound <- df$upBound
dfUp$boundType <- "upper"

dfBoth <- rbind(dfUp, dfLow)
##dfBoth <- subset(dfBoth, propSupervised == 0.9 & time < 2e5)# | propSupervised == 0.8 | propSupervised == 0.7)

p <- ggplot(dfBoth, aes(x=numSeen, y=(numFathom / numSeen), color=exp_dir)) + geom_line(aes(linetype=boundType))
print(p)
