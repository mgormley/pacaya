library(scatterplot3d)

library(MCMCpack)

numpoints <- 4000
x <- runif(numpoints, min=-14, max=0)
y <- runif(numpoints, min=-14, max=0)
z <- log(1 - exp(x) - exp(y))
scatterplot3d(x,y,z, highlight.3d=TRUE, col.axis="blue",
              col.grid="lightblue", main="scatterplot3d - 1", pch=20,
              angle=-30)


x <- rdirichlet(4000, c(1,1,1))
x <- log(x)
scatterplot3d(x[,1],x[,2],x[,3], highlight.3d=TRUE, col.axis="blue",
              col.grid="lightblue", main="scatterplot3d - 1", pch=20,
              angle=-30)

