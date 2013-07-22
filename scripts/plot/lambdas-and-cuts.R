# For use with output of leaves-vs-fathom.sh for lambdas and cuts
#
library("ggplot2")

stdwidth=8
stdheight=6
quartz(width=stdwidth,height=stdheight)

results.file = "/Users/mgormley/Documents/JHU4_S10/dep_parse/out"
df <- read.table(results.file, col.names=c("lambdas", "cuts"))
df$iteration <- as.numeric(row.names(df))
mydata <- df[sort(sample(nrow(df),1000,replace=FALSE)),]

xlab = "# Nodes Explored"
ylab = "# Lambdas / # Cuts"
##p <- ggplot(mydata, aes(x=iteration, y=lambdas)) + labs(x=xlab, y=ylab)
##p + geom_line() + geom_abline(intercept=0, slope=1)
##p + geom_line(aes(x=iteration, y=cuts))


p <- qplot(x=iteration, y=lambdas, data=mydata, geom="line",
           stat="identity", xlab=xlab, ylab=ylab, ylim=c(0,max(mydata$lambdas)))
p + geom_line() + geom_abline(intercept=0, slope=1)

##barplot(mydata$lambdas)
            
