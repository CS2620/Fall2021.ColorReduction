# Fall2021.Canvas

A fluent approach to image process with the addition of histogarm generation.

Images now have layers. 

The test driver is found in Main.java.

Processor.java is a container of image layers, which are contained in ImageLayer. 

ImageLayer contains meta data about the layer and an image, which is contained in IPImage (Image Processing Image).

IImage is an interface for a method that takes in an image and returns at image. This allows for lambda calls, such as i->grayscale(i).
