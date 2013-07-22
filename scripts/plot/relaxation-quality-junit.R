## Setup
library(ggplot2)

stdwidth=10
stdheight=8
quartz(width=stdwidth,height=stdheight)

## Read data
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/relax-quality-synth3pos-viterbiem.data"
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/relax-quality-synth3pos-uniform.data"
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/relax-quality-synth3pos-rand1.data"
results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/relax-quality-synth3pos-rand2.data"

prefix = "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/relax-quality-synth3pos-"
filenames <- c("viterbiem", "uniform", "rand1", "rand2")
files <- filenames
for (i in 1:length(filenames)) {
  files[i] <- paste(c(prefix,filenames[i]), sep="")
}

files <- c("/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/relax-quality-synth3pos-gold.data", "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/relax-quality-synth3pos-viterbiem1.data", "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/relax-quality-synth3pos-uniform.data", "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/relax-quality-synth3pos-rand1.data", "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/relax-quality-synth3pos-rand2.data",  "/Users/mgormley/Documents/JHU4_S10/dep_parse/results/relax_quality/relax-quality-synth3pos-rand3.data")

results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/relax-quality.data"
files = c(results.file)

for (results.file in files) {
  df <- read.table(results.file, header=TRUE)

  df$skipProb <- df$skip / 100.0
  
  ## Plot a box plot of all the data (cutting off the top outliers)
  plotbox <- function(mydata) {
    xlab = "Offset from fixed parameters"
    ylab = "Bound given by relaxation"
    qplot(offset,
          relaxBound, data=mydata, group=skip,
          geom="point", color=skipProb, position = "identity",
          xlab=xlab, ylab=ylab, shape=containsGoldSol, size=3) +
            opts(axis.text.x=theme_text(angle=70, hjust=1.0)) + geom_smooth(aes(color=skipProb), size=0.5) +
              geom_abline(intercept = -283.80, slope=0) 
  }
  
  print(plotbox(df))
  ggsave(paste(c(results.file, ".pdf"), collapse = ""))
}

