## Setup
library(ggplot2)
##library(plyr)
library(stringr)
##library(Hmisc) ## For Pearson correlation: rcorr()

##stdwidth <- 4.7 (for paper)
stdwidth <- 6.38
stdheight <- 3.4
quartz(width=stdwidth,height=stdheight)

myplot <- function(p, filename) {
  p <- p + theme_bw(12, base_family="serif")
  print(p)
  ggsave(filename)#, width=stdwidth, height=stdheight)
}

## Read data
#results.file = "/Users/mgormley/research/pacaya/remote_exp/dp-aware_006/scrape_srl/results.data"
results.file = "/Users/mgormley/research/pacaya/remote_exp/dp-aware-en_001/scrape_srl/results.data"
df <- read.table(results.file, header=TRUE)
df.orig <- df

df$method = str_c(df$trainer, df$dpLoss, df$group, sep=".")

df <- subset(df, language == "en")
df <- subset(df, inference == "BP")
df <- subset(df, trainer == "ERMA" | trainer == "CLL")
df <- subset(df, tagger_parser == "2nd-gra-asib-pr")

plotCurves <- function(mydata) {
  title = "Comparison of CLL and ERMA Training"
  xlab = "# of BP iterations"
  ylab = "Unlabeled Attachment Score (UAS)"
  p <- ggplot(mydata, aes(x=bpMaxIterations,
                          y=test.Unlabeled.attachment.score,
                          color=method))
  p <- p + geom_point()
  p <- p + geom_line()
  p <- p + xlab(xlab) + ylab(ylab) + ggtitle(title)
  p <- p + scale_color_discrete(name="Method")
}

myplot(plotCurves(df),
       str_c(results.file, "dp-aware-en-001", "pdf", sep="."))
