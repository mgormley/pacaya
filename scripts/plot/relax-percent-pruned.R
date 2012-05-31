## Setup
library(ggplot2)

stdwidth=10
stdheight=8
quartz(width=stdwidth,height=stdheight)

## Read data
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/bnb_005.data"

types <- c("viterbiem", "uniform", "rand")

df <- read.table(results.file, header=TRUE)
df <- subset(df, !is.na(relaxBound) & !is.na(offsetProb))

##df$skipProb <- df$probOfSkipCm / 100.0
df$offsetProb <- factor(df$offsetProb)

## Plot a box plot of all the data (cutting off the top outliers)
plotbox <- function(mydata) {
  xlab = "Offset from fixed parameters"
  ylab = "Bound given by relaxation"
  qplot(offsetProb,
        relaxBound, data=mydata, geom="jitter", color=relaxBound,
        xlab=xlab, ylab=ylab, alpha=0.8, position=position_jitter(width=0.25)) + 
          opts(axis.text.x=theme_text(angle=70, hjust=1.0))  + geom_abline(intercept = -5426.26, slope=0)
  ##+ geom_smooth(aes(color=skipProb), size=0.5) 
  ## TODO: get intercept from data!
  ## TODO: shape=containsGoldSol
}
    
print(plotbox(df))

ggsave(paste(c(results.file, ".pdf"), collapse = ""))


