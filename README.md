### joons-renderer brings Sunflow's ray-tracing to Processing.
_Joon Hyub Lee, joonhyub.lee@kaist.ac.kr_

### License
Use & modify as you want.  
It's open-sourced, and your contribution will be much appreciated.

### Tutorial & Source
* [Tutorial](https://github.com/joonhyublee/joons-renderer/wiki)
* [Source](https://github.com/joonhyublee/joons-renderer)

### Reference
* [Processing](http://processing.org)
* [Processing Reference](http://processing.org/reference/)
* [Processing Source](https://github.com/processing/processing)
* [Sunflow](http://sunflow.sourceforge.net/index.php?pg=gall)
* [Sunflow Wiki](http://sfwiki.geneome.net/index.php5?title=Main_Page)
* [Latest Build of Sunflow 0.07.3 by Monkstone](https://github.com/monkstone/sunflow)


###joons-renderer brings Sunflow's ray-tracing to Processing.
* [Download, installation & first render](Getting Started).
* [Detailed tutorial with examples](Tutorial).
* [Example sketches](Example Sketches).
* [List of supported and unsupported functions](Supported-&-Unsupported-Functions).
* Works of others using **joons-renderer** on [Tumblr](http://www.tumblr.com/tagged/joonsrenderer), [Flickr](http://www.flickr.com/search/?q=joons%20renderer), [YouTube](http://www.youtube.com/results?search_query=joonsrenderer), and [Vimeo](http://vimeo.com/search?q=joonsrenderer).
* Animated examples, [depth-of-field enabled](http://www.youtube.com/watch?v=g9GZM1pmrl4) and [disabled](http://www.youtube.com/watch?v=06qPq-v1zZI).

### Features
![Example](https://lh5.googleusercontent.com/-q5MHc8nmGZ4/UcG1pU1fuzI/AAAAAAAACmg/AgsVujT-zcU/w1000-h375-no/Sample.png)
(1) Original Processing sketch, (2) Rendered using **joons-renderer**.

Code as usual in [Processing](http://processing.org), and make small additions to your code to make a  realistic render of it.  
The code can be as simple as:

    fill("mirror");
    sphere(15);

**joons-renderer** reads geometry from Processing, applies [Sunflow](http://sunflow.sourceforge.net/index.php?pg=gall) ray-tracer engine to it,  
and returns the rendered image from the exact same viewpoint. It supports:
* _Texture_
* _Light_
* _Depth-of-field_

.. and more. Check out the [full list of supported and unsupported functions](Supported-&-Unsupported-Functions).

