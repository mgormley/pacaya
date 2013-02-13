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

safe.as.numeric <- function(x) {
  x <- as.character(x)
  x <- str_replace(x, "Infinity", "Inf")
  as.numeric(x)
}

## Plotting functions.
plotrootbound <- function(mydata) {
  xlab = "Proportion of RLT rows included"
  ylab = "Upper bound on log-likelihood at root"
  p <- qplot(rltInitProp, relaxBound, data=mydata,
             geom="point", xlab=xlab, ylab=ylab) +
                 opts(axis.text.x=theme_text(angle=70, hjust=1.0))
  p <- p + geom_line()
}

plotrelaxtime <- function(mydata) {
  xlab = "Proportion of RLT rows included"
  ylab = "Time (mins)"
  p <- qplot(rltInitProp, relaxTime / 1000 / 60, data=mydata,
             geom="point", xlab=xlab, ylab=ylab) +
                 opts(axis.text.x=theme_text(angle=70, hjust=1.0))
  p <- p + geom_line()
}

## Read data.
results.file = "/Users/mgormley/research/dep_parse/results/relax-root-rlt/results.data"

df.orig <- read.table(results.file, header=TRUE, sep="\t")
df <- df.orig

df <- subset(df, simplexAlgorithm == "BARRIER")
df$relaxBound <- safe.as.numeric(df$relaxBound)

myplot(plotrootbound(df),
       str_c(results.file, groupLevel, "relaxBound", "pdf", sep="."))

myplot(plotrelaxtime(df),
       str_c(results.file, groupLevel, "relaxTime", "pdf", sep="."))
