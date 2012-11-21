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
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/bnb-depth-test/curnode-status.data"
df <- read.table(results.file, header=TRUE)
df.orig <- df

df$method = str_c(df$algorithm, df$offsetProb, df$probOfSkipCm, df$initWeights, sep=".")
incumbentScore <- -119.228527

plotLowerBoundVsDepth <- function(mydata) {
  title = "Synthetic Data from DMV with 3 POS tags"
  xlab = "Depth"
  ylab = "Bound on log-likelihood"
  p <- ggplot(mydata, aes(x=depth,
                          y=upperBound))#, color=factor(depth)))
  p <- p + geom_boxplot(aes(group=depth))
  #p <- p + geom_jitter(aes(group=depth, alpha=0.7))
  #p <- p + geom_point()
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_color_discrete(guide=FALSE)
  # Add a line showing the lower bound found.
  p <- p + geom_abline(intercept=incumbentScore, slope=0)
}

myplot(plotLowerBoundVsDepth(df),
       str_c(results.file, "lbvdepth", "pdf", sep="."))

## Plot counts of each status.
plotCountsVsDepth <- function(mydata) {
  title = "Synthetic Data from DMV with 3 POS tags"
  xlab = "Depth"
  ylab = "Count"
  p <- qplot(factor(depth), data=mydata, geom="bar", fill=factor(relaxStatus2))
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_fill_discrete(name="Relaxation Status")
}

df$relaxStatus2 <- ifelse(df$upperBound < incumbentScore, "Pruned", "Not-pruned")
myplot(plotCountsVsDepth(df),
       str_c(results.file, "countsvdepth", "pdf", sep="."))

## Plot proportion of each status.
mysummary <- function(df) {
  df$isKept <- ifelse(df$relaxStatus2 == "Pruned", 0, 1)

  mydf <- data.frame(depth = 0)  
  mydf$depth <- df$depth[1]
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
depths <- ddply(df, .(depth), mysummary)

plotProportionVsDepth <- function(mydata) {
  title = "Synthetic Data from DMV with 3 POS tags"
  xlab = "Depth"
  ylab = "Proportion of nodes pruned"
  ## p <- ggplot(mydata, aes(x=factor(depth), y=count, fill=factor(relaxStatus2))) 
  ## p <- p + geom_bar(position="fill")
  ## p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  ## p <- p + scale_fill_discrete(name="Relaxation Status")
  p <- ggplot(mydata, aes(x=depth, y=(1 - sampleMean)))
  p <- p + geom_line()
  p <- p + xlab(xlab) + ylab(ylab) + opts(title=title)
  p <- p + scale_fill_discrete(name="Relaxation Status")
}

##depths <- ddply(df, .(depth, relaxStatus2), summarise, count=length(depth))
myplot(plotProportionVsDepth(depths),
       str_c(results.file, "propvdepth", "pdf", sep="."))

## Print out the estimated number of nodes in the pruned B&B tree
##

## Estimate of the total number of nodes kept.
est.pop.tot <- sum(depths$estNumKept)

## The stratified sample variance for that estimate.
## See : http://webcast.idready.org/materials/fall07/appliedepir/2007-11-27/stratsurvey2.pdf
## Also see pg 217 of Rice book.
attach(depths)
var.tot.vec <- (1 - (numSampled/population)) * population^2 * (sampleVariance/numSampled)
detach(depths)
## Set the variance of the root node to zero.
var.tot.vec[1] <- 0.0
var.tot <- sum(var.tot.vec)
se.tot <- sqrt(var.tot)

## Confidence interval for the estimate.
ci <- c(est.pop.tot - 1.96 * se.tot,
        est.pop.tot + 1.96 * se.tot)

# Print the estimates.
sprintf("estimate=%f stddev=%f", est.pop.tot, se.tot)
sprintf("confidence interval = %f", ci)
sprintf("number of hours to complete (26ms per node) = %f", est.pop.tot * 26 / 1000 / 60 / 60)
sprintf("number of days to complete (26ms per node) = %f", est.pop.tot * 26 / 1000 / 60 / 60 / 24)

## ------ Trash -------
## depths <- ddply(df, depth ~ relaxStatus2, summarise,
##                 numSampled = length(depth),
##                 numPruned =  length(subset(relaxStatus2, relaxStatus2 == "Pruned")))
## depths <- aggregate(df, by=list(depth, relaxStatus2), FUN=length)
## attach(df)
## proportion <- function(x) { length(subset(x, x == "Pruned")) / length(x) }
## depths <- aggregate(relaxStatus2 ~ depth, data=df, FUN=proportion)
## depths$propPruned <- depths$relaxStatus2
## # Approximate the number of nodes remaining in the tree at each depth.
## depths$numNotPruned <- (1.0 - depths$propPruned) * 2^(depths$depth)
## # Print the total number of nodes.
## print(sum(depths$numNotPruned))
## detach(df)
