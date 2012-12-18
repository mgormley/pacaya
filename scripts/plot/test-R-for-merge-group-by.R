library(plyr)
#library(stringr)
num.rows = 10000
group.size = 2
df1 <- data.frame(id=1:num.rows)
df2 <- data.frame(id=sample(1:num.rows))

dfs <- list(df1=df1, df2=df2)
for(i in 1:2) {
  dfs[[i]]$col1 <- rnorm(num.rows)
  dfs[[i]]$col2 <- as.character(dfs[[i]]$col1)
  dfs[[i]]$col3 <- factor(dfs[[i]]$id)
  dfs[[i]]$col4 <- 1:(num.rows/group.size)
}

df1 <- dfs[[1]]
df2 <- dfs[[2]]
#print(df1)
#print(df2)

write.csv(df1, "df1.csv")
write.csv(df2, "df2.csv")

system.time(dfj <- merge(df1, df2, by="id"))
#print(dfj)
print(nrow(dfj))

mysummary <- function(df) {
  mydf <- data.frame(depth = 0)  
  mydf$min1x <- min(df$col1.x)
  mydf$mean1x <- mean(df$col1.x)
  mydf$max1x <- max(df$col1.x)
  mydf$sum1y <- sum(df$col1.y)
  mydf$count <- nrow(df)
  mydf$argMax2x <- df[which.max(df$col1.x),]$col2.x
  return(mydf)
}
system.time(dfgb <- ddply(dfj, .(col4.y), mysummary))
#print(dfgb)
print(nrow(dfgb))
