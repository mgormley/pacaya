## Setup
library(ggplot2)
library(plyr)
library(stringr)
library(Hmisc) ## For Pearson correlation: rcorr()

stdwidth <- 4.7
stdheight <- 3.4
quartz(width=stdwidth,height=stdheight)

myplot <- function(p, filename) {
  p <- p + theme_bw(12, base_family="serif")
  print(p)
  ggsave(filename, width=stdwidth, height=stdheight)
}

## Read data
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/viterbi-bnb/incumbent-status.data"
df <- read.table(results.file, header=TRUE)
df.orig <- df

df$method = str_c(df$algorithm, df$offsetProb, df$probOfSkipCm, df$initWeights, sep=".")

df <- subset(df, algorithm == "viterbi" | algorithm == "bnb" | probOfSkipCm == 0)
df <- subset(df, maxNumSentences == 300)
df <- subset(df, time/1000/60 < 61)

plotLogLikeVsTime <- function(mydata) {
  title = "Penn Treebank, Brown"
  xlab = "Time (min)"
  ylab = "Log-likelihood (train)"
  p <- ggplot(mydata, aes(x=time / 1000 / 60,
                          y=incumbentLogLikelihood, color=method))
  #TODO p <- p + geom_point()
  p <- p + geom_line()
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_color_discrete(name="Method")
}

plotAccVsTime <- function(mydata) {
  title = "Penn Treebank, Brown"
  xlab = "Time (min)"
  ylab = "Accuracy (train)"
  p <- ggplot(mydata, aes(x=time / 1000 / 60,
                          y=incumbentAccuracy, color=method))
  #TODO p <- p + geom_point()
  p <- p + geom_line()
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_color_discrete(name="Method")
}

myplot(plotLogLikeVsTime(df),
       str_c(results.file, "llvtime", "pdf", sep="."))

myplot(plotAccVsTime(df),
       str_c(results.file, "accvtime", "pdf", sep="."))

# For debugging:
x <- df[,c("time", "incumbentLogLikelihood", "method")]
