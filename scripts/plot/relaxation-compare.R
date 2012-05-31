## Setup
library(ggplot2)

stdwidth=10
stdheight=8
quartz(width=stdwidth,height=stdheight)

## Read data
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_compare/bnb_008.data"

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

##  ------- Not very interesting: plot of relaxBound vs. relaxSetup ---------
for(mnsLevel in c(300)) {
  for(ibLevel in levels(df$initBounds)) {
    for(myxcolname in c("relaxation", "maxDwIterations", "maxSimplexIterations", "maxSetSizeToConstrain", "minSumForCuts")) {
    ## Plot a box plot of all the data (cutting off the top outliers)
    plotbox <- function(mydata, myxcolname) {
      myxcol <- mydata[,myxcolname]
      xlab = myxcolname
      ylab = "relaxTime(ms)"

      p <- qplot(myxcol,
            relaxTime, data=mydata, group=myxcol, #color=offsetProb,
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
    ## Plot a box plot of all the data (cutting off the top outliers)
    plotbox <- function(mydata) {
      xlab = "Offset from fixed parameters"
      ylab = "Bound given by relaxation"
      qplot(offsetProb,
            relaxBound, data=mydata, group=relaxSetup,
            geom="point", color=relaxSetup, position = "identity",
            xlab=xlab, ylab=ylab, size=3) +
              opts(axis.text.x=theme_text(angle=70, hjust=1.0)) + geom_smooth(aes(color=relaxSetup), size=0.5, se=FALSE) 
      ## TODO: + geom_abline(intercept = -283.80, slope=0)
      ## TODO: shape=containsGoldSol
    }
    
    dfsubset <- subset(df, initBounds == ibLevel & maxNumSentences == mnsLevel)
    print(plotbox(dfsubset))
    ggsave(paste(c(results.file, ".", mnsLevel, ".", ibLevel, ".relaxBound", ".pdf"), collapse = ""))
  }
}


