gh_count = read.table("/Users/Di/Documents/Related-Code-Recommendation/auxiliary/numOfSimilarGH.txt")
summary(gh_count$V1)
gh = gh_count[gh_count$V1 <= 50, ]
hist(gh, 
     main="", 
     xlab="Number of GitHub clones", 
     ylab = "Number of SO snippets",
     xlim=c(0, 50),
     xaxt="n",
     yaxt="n",
     cex.lab=1.5
)
axis(1,cex.axis=1.5)
axis(2,cex.axis=1.5)

related_count = read.table("/Users/Di/Documents/Related-Code-Recommendation/ASE19/auxiliary/numOfRelated.txt")
summary(related_count$V1)
related = related_count[related_count$V1 <= 200, ]
hist(related, 
     main="", 
     xlab="Number of methods", 
     ylab = "Number of SO snippets",
     xlim=c(0, 200),
     xaxt="n",
     yaxt="n",
     cex.lab=1.5
)
axis(1,cex.axis=1.5)
axis(2,cex.axis=1.5)


avg_loc_count = read.table("/Users/Di/Documents/Related-Code-Recommendation/ASE19/auxiliary/avgLocOfRelated.txt")
summary(avg_loc_count$V1)
hist(avg_loc_count$V1, 
     main="", 
     xlab="Average LOC for methods", 
     ylab = "Number of SO snippets",
)
avg_loc = avg_loc_count[avg_loc_count$V1 <= 60, ]
hist(avg_loc, 
     main="", 
     xlab="Average LOC for methods", 
     ylab = "Number of SO snippets",
     xlim=c(0, 60),
     xaxt="n",
     yaxt="n",
     cex.lab=1.5
)
axis(1,cex.axis=1.5)
axis(2,cex.axis=1.5)
