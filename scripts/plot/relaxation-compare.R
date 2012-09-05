## Setup
library(ggplot2)
library(plyr)
library(stringr)

stdwidth=10
stdheight=8
quartz(width=stdwidth,height=stdheight)

## Read data
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_compare/bnb_007.data"

df <- read.table(results.file, header=TRUE)

## Currently not included in exp: df$skipProb <- df$probOfSkipCm / 100.0

df$relaxSetup <- do.call(paste, c(df[c("relaxation",
                                        "maxDwIterations",
                                        "maxSimplexIterations",
                                        "maxSetSizeToConstrain",
                                        "minSumForCuts")], sep = "."))

# Skip the (uninteresting) viterbi em case
df <- subset(df, initBounds == "random")
df$initBounds <- factor(df$initBounds)
#df.viterbiEm <- subset(df, initBounds == "viterbi-em")

columns <- c("relaxation", "maxDwIterations", "maxSimplexIterations", "maxSetSizeToConstrain", "minSumForCuts")
base.col <- c("dw", max(df$maxDwIterations), max(df$maxSimplexIterations), max(df$maxSetSizeToConstrain), max(df$minSumForCuts))

##  ------- Not very interesting: plot of relaxBound vs. relaxSetup ---------
for(mnsLevel in c(300)) {
  for(ibLevel in levels(df$initBounds)) {
    for(myxcolname in c("relaxation", "maxDwIterations", "maxSimplexIterations", "maxSetSizeToConstrain", "minSumForCuts")) {
    ## Plot a box plot of all the data (cutting off the top outliers)
    plotbox <- function(mydata, myxcolname) {
      myxcol <- mydata[,myxcolname]
      xlab = myxcolname
      #ylab = "relaxTime(ms)"
      ylab = "relaxBound"

      p <- qplot(myxcol,
            relaxBound, data=mydata, group=myxcol, #color=offsetProb,
            geom="boxplot", position = "identity",
                 xlab=xlab, ylab=ylab) +
              opts(axis.text.x=theme_text(angle=70, hjust=1.0))
      if (is.numeric(myxcol)) {
        p + scale_x_log10()
      } else {
        p
      }
      
      ## TODO: + geom_abline(intercept = -283.80, slope=0)
      ## TODO: shape=containsGoldSol
    }
    
    dfsubset <- subset(df, initBounds == ibLevel & maxNumSentences == mnsLevel)
    if (myxcolname == "maxSetSizeToConstrain" || myxcolname == "minSumForCuts") {
          dfsubset <- subset(df, relaxation == "dw")
    }
    
    print(plotbox(dfsubset, myxcolname))
    ggsave(paste(c(results.file, ".", mnsLevel, ".", ibLevel, ".", myxcolname, ".pdf"), collapse = ""))
  }
  }
}

##  ------- Not very interesting: plot of relaxBound vs. relaxSetup ---------
for(mnsLevel in c(300)) {
  for(ibLevel in levels(df$initBounds)) {
    for(myxcolname in c("relaxation", "maxDwIterations", "maxSimplexIterations", "maxSetSizeToConstrain", "minSumForCuts")) {
    ## Plot a box plot of all the data (cutting off the top outliers)
    plotbox <- function(mydata, myxcolname) {
      myxcol <- mydata[,myxcolname]
      xlab = "Offset from fixed parameters"
      ylab = "Bound given by relaxation"
      qplot(factor(offsetProb),
            relaxBound, data=mydata, fill=factor(myxcol),
            #group=myxcol, color=myxcol,
            geom="boxplot", 
            xlab=xlab, ylab=ylab, alpha=0.7) +
              opts(axis.text.x=theme_text(angle=70, hjust=1.0)) #+ geom_smooth(aes(color=myxcol), size=0.5, se=FALSE) 
      ## TODO: + geom_abline(intercept = -283.80, slope=0)
      ## TODO: shape=containsGoldSol
    }

    plotpoints <- function(mydata, myxcolname) {
      myxcol <- mydata[,myxcolname]
      xlab = "Offset from fixed parameters"
      ylab = "Bound given by relaxation"
      qplot(factor(offsetProb),
            relaxBound, data=mydata, #fill=factor(myxcol),
            #group=myxcol,
            color=factor(myxcol),
            geom="jitter", 
            xlab=xlab, ylab=ylab, alpha=0.7) +
              opts(axis.text.x=theme_text(angle=70, hjust=1.0)) #+ geom_smooth(aes(color=myxcol), size=0.5, se=FALSE) 
      ## TODO: + geom_abline(intercept = -283.80, slope=0)
      ## TODO: shape=containsGoldSol
    }
    
    dfsubset <- subset(df, initBounds == ibLevel & maxNumSentences == mnsLevel)
    if (myxcolname == "maxSetSizeToConstrain" || myxcolname == "minSumForCuts") {
          dfsubset <- subset(df, relaxation == "dw")
    }
    print(plotpoints(dfsubset, myxcolname))
    ggsave(paste(c(results.file, ".", mnsLevel, ".", ibLevel, ".rb.", myxcolname, ".pdf"), collapse = ""))
  }
  }
}


