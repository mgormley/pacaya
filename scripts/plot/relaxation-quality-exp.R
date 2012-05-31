## Setup
library(ggplot2)

stdwidth=10
stdheight=8
quartz(width=stdwidth,height=stdheight)

## Read data
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/bnb_004.data"

types <- c("viterbiem", "uniform", "rand")

df <- read.table(results.file, header=TRUE)

df$skipProb <- df$probOfSkipCm / 100.0

for(mnsLevel in c(100,300)) {
  for(ibLevel in levels(df$initBounds)) {
    ## Plot a box plot of all the data (cutting off the top outliers)
    plotbox <- function(mydata) {
      xlab = "Offset from fixed parameters"
      ylab = "Bound given by relaxation"
      qplot(offsetProb,
            relaxBound, data=mydata, group=skipProb,
            geom="point", color=skipProb, position = "identity",
            xlab=xlab, ylab=ylab, size=3) +
              opts(axis.text.x=theme_text(angle=70, hjust=1.0)) + geom_smooth(aes(color=skipProb), size=0.5) 
      ## TODO: + geom_abline(intercept = -283.80, slope=0)
      ## TODO: shape=containsGoldSol
    }
    
    dfsubset <- subset(df, initBounds == ibLevel & maxNumSentences == mnsLevel)
    print(plotbox(dfsubset))
    ggsave(paste(c(results.file, ".", mnsLevel, ".", ibLevel, ".pdf"), collapse = ""))
  }
}

