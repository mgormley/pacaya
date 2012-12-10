## Setup
library(ggplot2)
library(plyr)
library(stringr)
library(Hmisc) ## For Pearson correlation: rcorr()

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

## Plot proportion of each status.
mysummary <- function(df) {
  df$isKept <- ifelse(df$relaxStatus2 == "Pruned", 0, 1)

  mydf <- data.frame(depth = 0)  
  mydf$depth <- df$depth[1]
  mydf$method <- df$method[1]
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

depths <- ddply(df, .(depth, method), mysummary)
print(depths)
## Print out the estimated number of nodes in the pruned B&B tree
##
## As input this requires:
## - Time per node
## - Method name
## - Relax status: pruned or not-pruned.
##   + Incumbent score
##   + Lower bound
## - Depth

depths.orig <- depths
for (m in unique(depths.orig$method)) {
  depths = subset(depths.orig, method == m)
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

  ## Time(ms) per node.
  df.subset <- subset(df, method == m)
  time.per.node <- df.subset[which.max(df$id),]$avgNodeTime

  ## Estimated time(ms) to complete B&B.
  est.time.tot <- est.pop.tot * time.per.node / 1000 / 60 / 60
  
  ## Print the estimates.
  print.noquote(sprintf("Method: %s", m))
  print.noquote(sprintf("Estimated number of nodes: %f", est.pop.tot))
  print.noquote(sprintf("Standard deviation: %f", se.tot))
  print.noquote(sprintf("Confidence interval min: %f", ci[1]))
  print.noquote(sprintf("Confidence interval max: %f", ci[2]))
  print.noquote(sprintf("Number of samples: %d", nrow(df.subset)))
  print.noquote(sprintf("Average time(ms) per node: %f", time.per.node))
  print.noquote(sprintf("Estimated number of B&B hours: %f", est.time.tot))
  print.noquote(sprintf("Estimated number of B&B days: %f", est.time.tot / 24))
}
