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
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/viterbi-em/results.data"
df <- read.table(results.file, header=TRUE)

plotaccuracyvslikelihood <- function(mydata) {
  title = "Penn Treebank, Brown"
  ##title = str_c(getDataset(mydata), unique(df$offsetProb), sep=".")
  xlab = "Per token log-likelihood (train)"
  ylab = "Accuracy (train)"
  p <- ggplot(mydata, aes(x=trainLogLikelihood/numWords,
                          y=trainAccuracy, color=initWeights))
  p <- p + geom_point()
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_color_discrete(name="Initialization")
  p <- p + geom_smooth()
}
myplot(plotaccuracyvslikelihood(df),
       str_c(results.file, "accvlike", "pdf", sep="."))

dfCorr <- subset(df, initWeights == "random")
dfCorr <- dfCorr[,c("trainAccuracy", "trainLogLikelihood")]
rcorr(as.matrix(dfCorr), type="pearson")
