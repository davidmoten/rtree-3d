#!/usr/bin/Rscript
#X11(type="Xlib")
library("plot3D")
mat <- read.csv(file = "/home/dave/Development/ide/eclipse/workspace-4.4/rtree-3d/target/out.txt", header = FALSE)
png("../../../target/plot.png", height = 700, width =1000 )
box3D(
#  x0 = runif(3), y0 = runif(3),print z0 = runif(3),
#  x1 = runif(3), y1 = runif(3), z1 = runif(3),
  x0 = mat[,1], y0 = mat[,2], z0=mat[,3],
  x1 = mat[,4], y1 = mat[,5], z1=mat[,6],
  col = c("red", "lightblue", "orange"), 
  alpha = 0.5,
  border = "black", 
  lwd = 2)
dev.off()
