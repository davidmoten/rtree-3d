#!/usr/bin/Rscript
#X11(type="Xlib")
#install.packages("plot3D")
library("plot3D")
for (i in 0:9) {
    filename = paste("../../../target/out",i,".txt", sep="")
    print(paste("reading", filename))
    mat <- read.csv(file = filename, header = FALSE)
    png(paste("../../../target/plot",i,".png",sep=""), height = 700, width =1000 )
    box3D(
      x0 = mat[,1], y0 = mat[,2], z0=mat[,3],
      x1 = mat[,4], y1 = mat[,5], z1=mat[,6],
      col = c("red", "lightblue", "orange"), 
      alpha = 0.3,
      border = "black", 
      lwd = 2)
    dev.off()
}
