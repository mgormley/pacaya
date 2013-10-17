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


## Read data
results.file = "/Users/mgormley/research/parsing/results/viterbi-vs-bnb/incumbent-status.data"
df <- read.table(results.file, header=TRUE)
df.orig <- df
df <- df[order(df$time),]

prepData <- function(df) {
df$rltInitMax[which(is.na(df$rltInitMax))] <- as.numeric("+inf")
df$rltCutMax[which(is.na(df$rltCutMax))] <- as.numeric("+inf")
df$method <- str_c(df$relaxation, df$envelopeOnly, df$rltInitMax, df$varSelection, df$rltCutMax, sep=".")
## For ACL:
df$method <- ""
df$method <- ifelse(df$algorithm == "bnb" , str_c(df$method, "RLT"), df$method)
df$method <- ifelse(df$envelopeOnly == "True", str_c(df$method, " Max.0k"), df$method)
df$method <- ifelse(df$rltFilter == "obj-var" & !(df$envelopeOnly == "True"), str_c(df$method, " Obj.Filter"), df$method)
## ON FOR ACL SUBMISSION:
df$method <- ifelse(df$rltFilter == "max", str_c(df$method, " Max.", df$rltInitMax/1000, "k"), df$method)
##df$method <- ifelse(df$rltFilter == "max", str_c(df$method, " Max.", df$rltInitMax/1000, "k.", df$rltCutMax/1000, "k"), df$method)
df$method <- ifelse(df$algorithm == "viterbi", "Viterbi EM", df$method)

#df$method <- str_c(df$envelopeOnly, df$rltInitMax, sep=" / ")
methodDescription <- "Algorithm"

## OLD ACL method description:
##df$method <- ifelse(df$algorithm == "viterbi", "Viterbi EM", str_c("B&B", df$envelopeOnly, df$rltInitMax, sep=" / "))
##methodDescription <- "Algorithm /\nEnvelope Only / \nMax RLT cuts"

##df$method = str_c(df$algorithm, sep=".")
##df <- subset(df, algorithm == "viterbi" | algorithm == "bnb" )
##df <- subset(df, time/1000/60 < 61)
df <- subset(df, dataset == "wsj200")
df <- subset(df, is.na(rltInitMax) | rltInitMax == 1000 | rltInitMax == 10000 | rltInitMax == 100000 | !is.finite(rltInitMax))
df <- subset(df, maxNumSentences == 200)
## ON FOR ACL SUBMISSION:
df <- subset(df,is.na(rltCutMax) | !is.finite(rltCutMax) | rltCutMax == 0 )

df$perTokenCrossEntropy <- ifelse(df$dataset == "wsj200", - df$incumbentLogLikelihood / log(2) / 1365, NULL)
df$isGlobal <- ifelse(df$method == "Viterbi", FALSE, TRUE)


df$method <- factor(df$method, levels = c("Viterbi EM", "RLT Obj.Filter", "RLT Max.0k","RLT Max.1k", "RLT Max.10k", "RLT Max.100k"))

return(df)
}
df <- prepData(df)



# Remove any solution found after 8 hours.
df <- subset(df, time / 1000 / 60 / 60 < 8)
# Set the 8 hour incumbent as the best one seen in under 8 hours.
##df$time <- max(df$time, 8*60*60*1000)
myfun <- function(x) {
  x[which.max(x$incumbentLogLikelihood),]
}
ddf <- ddply(df, .(method, universalPostCons), myfun)
ddf$time <- 8*60*60*1000
df <- rbind(ddf, df)


myfun <- function(x) {
  x[which.max(x$time),]
}
ddf <- ddply(df, .(method, universalPostCons), myfun)
print(ddf[,c("method", "universalPostCons", "incumbentAccuracy")])
write.table(ddf[,c("method", "universalPostCons", "incumbentAccuracy")], file=str_c(results.file, "inc-acc", "csv", sep="."), sep=",")

plotLogLikeVsTime <- function(mydata) {
  title = "Penn Treebank, Brown"
  xlab = "Time (min)"
  ylab = "Log-likelihood (train)"
  #mydata$time <- max(mydata$time, 1.0)
  mydata$minutes <- mydata$time / 1000 / 60
  #mydata$minutes <- mydata$time / 1000 
  mydata$minutes[which(mydata$minutes < 1.0)] <- 1.0

  p <- ggplot(mydata, aes(x=minutes,
                          y=incumbentLogLikelihood, color=method))
  p <- p + geom_point(aes(shape=method))
  p <- p + geom_line(aes(linetype=universalPostCons))
  p <- p + xlab(xlab) + ylab(ylab) 
  ## For ACL: p <- p + opts(title=title)
  p <- p + scale_color_discrete(name=methodDescription) 
  p <- p + scale_shape_discrete(name=methodDescription)
  p <- p + scale_linetype_discrete(name="Posterior Constraints")
  #p <- p + scale_x_log10()
  #p <- p + scale_y_log10()
}

plotAccVsTime <- function(mydata) {
  title = "Penn Treebank, Brown"
  xlab = "Time (min)"
  ylab = "Accuracy (train)"
  p <- ggplot(mydata, aes(x=time / 1000 / 60,
                          y=incumbentAccuracy, color=method))
  p <- p + geom_point(aes(shape=method))
  p <- p + geom_line(aes(linetype=universalPostCons))
  ##p <- p + geom_smooth(aes(linetype=universalPostCons))
  p <- p + xlab(xlab) + ylab(ylab)
  ## For ACL: p <- p + opts(title=title)
  p <- p + scale_color_discrete(name=methodDescription)
  p <- p + scale_shape_discrete(name=methodDescription)
  p <- p + scale_linetype_discrete(name="Posterior Constraints")
  #p <- p + scale_x_log10()
}


myplot(plotLogLikeVsTime(subset(df, universalPostCons == "False")),
       str_c(results.file, "llvtime", "pdf", sep="."))

myplot(plotAccVsTime(subset(df, universalPostCons == "True")),
       str_c(results.file, "accvtime", "pdf", sep="."))

myplot(plotLogLikeVsTime(df),
       str_c(results.file, "llvtime", "pdf", sep="."))

myplot(plotAccVsTime(df),
       str_c(results.file, "accvtime", "pdf", sep="."))

max <- ddply(df, .(method, universalPostCons), function(x) max(x$incumbentLogLikelihood))
print(max[order(max$V1),])
max <- ddply(df, .(method, universalPostCons), function(x){ x[which.max(x$incumbentAccuracy),] })

myplot(plotAccVsTime(subset(df, method == "Viterbi EM" | method == "RLT Max 1000")),
       str_c(results.file, "accvtime", "pdf", sep="."))

