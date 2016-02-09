# rtree-3d
3D R-Tree in java.

Three dimensional R-Tree in java motivated by spatio-temporal queries. Nodes would be serialized to disk or network storage.  
Particularly interested in large static datasets.

Status: *pre-alpha*

Progress is being made on this project. I've copied my [rtree 2D implementation](https://github.com/davidmoten/rtree) and beefed it up for 3D. 

* expanded the R* `Selector` and `Splitter` implementations to handle 3 dimensions
* normalized coordinates so they range from 0..1
* added [R language code](src/main/r/source.r) to produce PNG visualizations of tree structure (below) 

If the coordinates are normalized to the [0..1] range then the data structure doesn't favour one dimension over another. To favour time over position for instance
 just scale the time value down by a constant (experiment with your data!).

Greek Earthquake data
-----------------------
Given the 38,377 data points of greek earthquakes (lat, long, time), the data is scanned to establish the ranges for each coordinate then 
normalized to a [0,1] range. The points are added to an R*-tree with `minChildren`=2 and `maxChildren`=4. Visualization 
of the bounding boxes at nodes by depth is below.


| Quadratic split | R*-tree split |
| :-------------: | :-----------: |
| <img src="https://raw.githubusercontent.com/davidmoten/davidmoten.github.io/master/resources/rtree-3d/plot0-q.png" /> | <img src="https://raw.githubusercontent.com/davidmoten/davidmoten.github.io/master/resources/rtree-3d/plot0-q.png" /> |

Depth 0:
<img src="https://raw.githubusercontent.com/davidmoten/davidmoten.github.io/master/resources/rtree-3d/plot0.png"/>
<br/>
Depth 1:
<img src="https://raw.githubusercontent.com/davidmoten/davidmoten.github.io/master/resources/rtree-3d/plot1.png"/>
<br/>
Depth 2:
<img src="https://raw.githubusercontent.com/davidmoten/davidmoten.github.io/master/resources/rtree-3d/plot2.png"/>
<br/>
Depth 3:
<img src="https://raw.githubusercontent.com/davidmoten/davidmoten.github.io/master/resources/rtree-3d/plot3.png"/>
<br/>
Depth 4:
<img src="https://raw.githubusercontent.com/davidmoten/davidmoten.github.io/master/resources/rtree-3d/plot4.png"/>
<br/>
Depth 5:
<img src="https://raw.githubusercontent.com/davidmoten/davidmoten.github.io/master/resources/rtree-3d/plot5.png"/>
<br/>
Depth 6:
<img src="https://raw.githubusercontent.com/davidmoten/davidmoten.github.io/master/resources/rtree-3d/plot6.png"/>
<br/>
Depth 7:
<img src="https://raw.githubusercontent.com/davidmoten/davidmoten.github.io/master/resources/rtree-3d/plot7.png"/>
<br/>
Depth 8:
<img src="https://raw.githubusercontent.com/davidmoten/davidmoten.github.io/master/resources/rtree-3d/plot8.png"/>
<br/>
Depth 9:
<img src="https://raw.githubusercontent.com/davidmoten/davidmoten.github.io/master/resources/rtree-3d/plot9.png"/>
<br/>

