## Setup
library(ggplot2)
library(plyr)
library(stringr)
##library(Hmisc) ## For Pearson correlation: rcorr()

##stdwidth <- 4.7 (for paper)
stdwidth <- 4.38
stdheight <- 5.55
quartz(width=stdwidth,height=stdheight)

myplot <- function(p, filename) {
  p <- p + theme_bw(14, base_family="serif")
  print(p)
  ggsave(filename)#, width=stdwidth, height=stdheight)
}

## Read data
results.file = "/Users/mgormley/research/pacaya/remote_exp/dp-aware-langs_003/scrape_srl/results.data"
df <- read.table(results.file, header=TRUE)
df.orig <- df

df$method = str_c(df$trainer, df$dpLoss, sep=".")

df <- subset(df, inference == "BP")
df <- subset(df, trainer == "ERMA" | trainer == "CLL")
df <- subset(df, tagger_parser == "2nd-gra-asib-pr")
df <- subset(df, is.na(group))
df <- subset(df, language != "en-st")

## We don't have zh for CoNLL-X so we use CoNLL-2007.
df <- subset(df, datasource == "CoNLL-X" |
             (datasource == "CoNLL-2007" &
              (language != "ar" &
               language != "cs" &
               language != "tr"
               )))

df <- df[with(df, order(trainer)), ]

doSummary = function(df) {    
    data.frame(count=length(df$test.Unlabeled.attachment.score))
}
# Should be counts of 2
ddply(df, .(bpMaxIterations, language), summarize, uasCount=length(test.Unlabeled.attachment.score))
#
df1 <- ddply(df, .(bpMaxIterations, language, datasource), summarize, uasDiff=diff(test.Unlabeled.attachment.score))

plotCurves <- function(mydata) {
  title = "Comparison of CLL and ERMA Training"
  xlab = "# of BP iterations"
  ylab = "L2 UAS - CLL UAS"
  #ylab = "UAS Improvement of L2 over CLL"
  p <- ggplot(mydata, aes(x=factor(bpMaxIterations),
                          y=uasDiff))
  p <- p + geom_boxplot(outlier.shape=4)
  p <- p + geom_jitter(position = position_jitter(width = .15),
                       size=3,
                       aes(color=language, shape=language))
  ##p <- p + geom_line()
  p <- p + xlab(xlab) + ylab(ylab)
  ##p <- p + ggtitle(title)
  p <- p + scale_color_discrete(name="Language")
  ##p <- p + scale_shape_discrete(name="Language")
  p <- p + scale_shape_manual(name="Language", values=1:nlevels(df$language))
}

myplot(plotCurves(df1),
       str_c(results.file, "dp-aware-langs-003", "pdf", sep="."))
