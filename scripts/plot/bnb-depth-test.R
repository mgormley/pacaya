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
safe.as.numeric <- function(x) {
  x <- as.character(x)
  x <- str_replace(x, "Infinity", "Inf")
  as.numeric(x)
}

## Read data
args <- commandArgs(TRUE)
if (file.exists(args[1])) {
  results.file <- args[1]
} else {
  localWorkDir <- "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/bnb-depth-test"
  setwd(localWorkDir)
  results.file <- "curnode-status.data"
}

print(sprintf("Using results file %s", results.file))
df <- read.table(results.file, header=TRUE)
df.orig <- df

## Print out incumbent scores and upper bounds.
## print("Incumbent scores:")
## print(unique(df$incumbentScore))
## print("Upper bounds:")
## print(unique(df$upperBound))
## print("Depths:")
## print(unique(df$depth))

print("Coercing infinities to numerics.")
df$incumbentScore <- safe.as.numeric(df$incumbentScore)
df$upperBound <- safe.as.numeric(df$upperBound)

print("Adding columns.")
df$method = str_c(df$relaxation, df$envelopeOnly, df$rltInitProp, df$varSelection, df$rltCutProp, sep=".")
incumbentScore <- max(df$incumbentScore)
df$relaxStatus2 <- ifelse(df$upperBound < incumbentScore, "Pruned", "Not-pruned")

plotLowerBoundVsDepth <- function(mydata) {
  title = "Synthetic Data from DMV with 3 POS tags"
  xlab = "Depth"
  ylab = "Bound on log-likelihood"
  p <- ggplot(mydata, aes(x=depth,
                          y=upperBound, color=method)) #color=factor(depth)))
  #p <- p + geom_boxplot(aes(group=depth))
  p <- p + geom_jitter(aes(group=method, alpha=0.1))
  #p <- p + geom_smooth(aes(group=method, alpha=0.7))
  #p <- p + geom_point()
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  #p <- p + scale_color_discrete(guide=FALSE)
  # Add a line showing the lower bound found.
  p <- p + geom_abline(intercept=incumbentScore, slope=0)
}

## To hide negative infinities: df <- subset(df, upperBound > -100)
myplot(plotLowerBoundVsDepth(df),
       str_c(results.file, "lbvdepth", "pdf", sep="."))

## Plot counts of each status.
plotCountsVsDepth <- function(mydata) {
  title = "Synthetic Data from DMV with 3 POS tags"
  xlab = "Depth"
  ylab = "Count"
  p <- qplot(factor(depth), data=mydata, geom="bar", fill=factor(relaxStatus2), group=method)
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_fill_discrete(name="Relaxation Status")
}

myplot(plotCountsVsDepth(df),
       str_c(results.file, "countsvdepth", "pdf", sep="."))

plotProportionVsDepth <- function(mydata) {
  title = "Synthetic Data from DMV with 3 POS tags"
  xlab = "Depth"
  ylab = "Proportion of nodes pruned"
  ## p <- ggplot(mydata, aes(x=factor(depth), y=count, fill=factor(relaxStatus2))) 
  ## p <- p + geom_bar(position="fill")
  ## p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  ## p <- p + scale_fill_discrete(name="Relaxation Status")
  p <- ggplot(mydata, aes(x=depth, y=(1 - sampleMean)))
  p <- p + geom_line(aes(color=method))
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_fill_discrete(name="Relaxation Status")
}

## Plot proportion of each status.
mysummary <- function(df) {
  df$isKept <- ifelse(df$relaxStatus2 == "Pruned", 0, 1)

  mydf <- data.frame(depth = 0)  
  mydf$depth <- df$depth[1]
  mydf$method <- df$method[1]

  ## Get the average node time from the node with the highest ID.
  mydf$avgNodeTime <- df[which.max(df$id),]$avgNodeTime

  mydf$sampleMean <- mean(df$isKept)
  mydf$sampleVariance <- var(df$isKept)
  mydf$numSampled <- length(df$depth)
  mydf$numKept <- sum(df$isKept)
  mydf$population <- 2^mydf$depth
  mydf$numPruned <- mydf$numSampled - mydf$numKept
  mydf$stratWeight <- mydf$population / mydf$numSampled
  mydf$estNumKept <- mydf$population * mydf$sampleMean
  
  return(mydf)
}

df.subset <- subset(df, dataset == "alt-three")
depths <- ddply(df.subset, .(depth, method), mysummary)
depths.subset <- subset(depths, numSampled > 50)
## For 4 sentences: depths <- ddply(subset(df, varSelection=="rand-uniform" & (rltCutProp == 0.2 | envelopeOnly == "True") & depth < 40), .(depth, method), mysummary)
## Rand-uniform: depths <- ddply(subset(df, varSelection=="rand-uniform"), .(depth, method), mysummary)
## Other summary: depths <- ddply(df, .(depth, relaxStatus2), summarise, count=length(depth))
myplot(plotProportionVsDepth(depths.subset),
       str_c(results.file, "propvdepth", "pdf", sep="."))

