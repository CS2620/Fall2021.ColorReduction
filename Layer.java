import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.awt.Color;

public class Layer {

  BufferedImage image;

  public Layer(String filename) {
    try {
      BufferedImage bufferedImage = ImageIO.read(new File(filename));
      this.image = bufferedImage;

    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public Layer(BufferedImage bi) {
    this.image = bi;
  }

  public Layer save(String filename) {
    // Get the file type
    var ending = filename.substring(filename.length() - 3);
    try {
      ImageIO.write(this.image, ending, new File(filename));
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return this;
  }

  public Layer rotatedNearestNeighbor(double angle) {
    BufferedImage toReturn = new BufferedImage(this.image.getWidth(), this.image.getHeight(),
        BufferedImage.TYPE_INT_ARGB);

    int width = this.image.getWidth();
    int height = this.image.getHeight();
    double centerX = width / 2;
    double centerY = height / 2;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {

        // Get the distance and angle from the origin
        double distanceX = x - centerX;
        double distanceY = y - centerY;
        double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);
        double postAngle = Math.atan2(distanceY, distanceX);
        double preAngle = postAngle - angle;
        int originalX = (int) ((distance * Math.cos(preAngle) + .5) + centerX);
        int originalY = (int) ((distance * Math.sin(preAngle) + .5) + centerY);
        if (originalX < 0 || originalX >= width || originalY < 0 || originalY >= height)
          continue;

        int pixel = this.image.getRGB(originalX, originalY);
        toReturn.setRGB(x, y, pixel);
      }
    }

    this.image = toReturn;
    return this;
  }

  public Layer scaleNearestNeighbor(double scaleX, double scaleY) {
    int newWidth = (int) (this.image.getWidth() * scaleX);
    int newHeight = (int) (this.image.getHeight() * scaleY);
    BufferedImage toReturn = new BufferedImage(this.image.getWidth(), this.image.getHeight(),
        BufferedImage.TYPE_INT_ARGB);

    for (int y = 0; y < newHeight; y++) {
      for (int x = 0; x < newWidth; x++) {
        if (x >= this.image.getWidth() || y >= this.image.getHeight())
          continue;
        // Find the source pixel
        int originalX = (int) (x / scaleX);
        int originalY = (int) (y / scaleY);
        int pixel = this.image.getRGB(originalX, originalY);
        toReturn.setRGB(x, y, pixel);
      }
    }

    this.image = toReturn;
    return this;
  }

  public Layer translateNearestNeighbor(double i, double j) {
    BufferedImage toReturn = new BufferedImage(this.image.getWidth(), this.image.getHeight(),
        BufferedImage.TYPE_INT_ARGB);
    for (var y = 0; y < toReturn.getHeight(); y++) {
      for (var x = 0; x < toReturn.getWidth(); x++) {
        int originalX = (int) (x - i + .5);
        int originalY = (int) (y - j + .5);

        if (originalX < 0 || originalX >= this.image.getWidth() || originalY < 0 || originalY >= this.image.getHeight())
          continue;

        var pixelInt = this.image.getRGB(originalX, originalY);
        toReturn.setRGB(x, y, pixelInt);
      }
    }

    this.image = toReturn;
    return this;
  }

  public Layer translateLinear(double i, double j, boolean b) {
    BufferedImage toReturn = new BufferedImage(this.image.getWidth(), this.image.getHeight(),
        BufferedImage.TYPE_INT_ARGB);
    for (var y = 0; y < toReturn.getHeight(); y++) {
      for (var x = 0; x < toReturn.getWidth() - 1; x++) {
        int originalX = (int) (x - i + .5);
        int leftPixel = (originalX);
        int rightPixel = (originalX + 1);
        int originalY = (int) (y - j + .5);

        if (originalX < 0 || originalX >= this.image.getWidth() - 1 || originalY < 0
            || originalY >= this.image.getHeight() - 1)
          continue;

        Color pixelLeft = new Color(this.image.getRGB(leftPixel, originalY));
        Color pixelRight = new Color(this.image.getRGB(rightPixel, originalY));

        double percent = i - (int) i;
        Color leftPixelContibution = new Color((int) (pixelLeft.getRed() * (1 - percent)),
            (int) (pixelLeft.getGreen() * (1 - percent)), (int) (pixelLeft.getBlue() * (1 - percent)));
        Color rightPixelContibution = new Color((int) (pixelRight.getRed() * (percent)),
            (int) (pixelRight.getGreen() * (percent)), (int) (pixelRight.getBlue() * (percent)));

        int finalRed = leftPixelContibution.getRed() + rightPixelContibution.getRed();
        int finalGreen = leftPixelContibution.getGreen() + rightPixelContibution.getGreen();
        int finalBlue = leftPixelContibution.getBlue() + rightPixelContibution.getBlue();
        Color finalColor = new Color(finalRed, finalGreen, finalBlue);

        toReturn.setRGB(x, y, finalColor.getRGB());
      }
    }

    this.image = toReturn;
    return this;
  }

  public Layer crop(int ulx, int uly, int lrx, int lry) {
    var width = lrx - ulx;
    var height = lry - uly;
    var toReturn = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    for (var h = 0; h < height; h++) {
      for (var w = 0; w < width; w++) {
        var pixelInt = this.image.getRGB(w + ulx, h + uly);
        var pixelColor = new Color(pixelInt);
        toReturn.setRGB(w, h, pixelColor.getRGB());
      }
    }

    this.image = toReturn;
    return this;

  }

  public Layer histogram() {
    return histogram(-1);
  }

  public Layer histogram(int cap) {

    if (cap < 1)
      cap = 256;
    int height = cap;
    var toReturn = new BufferedImage(256, height, BufferedImage.TYPE_4BYTE_ABGR);

    // Generate the histogram info
    int[] counts = new int[256];
    for (var h = 0; h < this.image.getHeight(); h++) {
      for (var w = 0; w < this.image.getWidth(); w++) {
        Color pixel = new Color(this.image.getRGB(w, h));
        int red = pixel.getRed();
        int green = pixel.getGreen();
        int blue = pixel.getBlue();
        float[] hsv = new float[3];
        myConversion(red, green, blue, hsv);
        int value = (int) (hsv[2] * 255);
        if (value == 1)
          System.out.println();
        counts[value]++;

      }
    }

    // Render the histogram
    Graphics2D g = (Graphics2D) toReturn.getGraphics();
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, counts.length, height);

    var maxValue = Arrays.stream(counts).max().orElse(0);
    // i//f (cap != -1)
    // maxValue = cap;
    for (var i = 0; i < counts.length; i++) {
      int percent = (int) (counts[i] / (double) maxValue * height);
      g.setColor(Color.WHITE);
      g.fillRect(i, height - percent, 1, percent);
    }

    this.image = toReturn;
    return this;

  }

  private static void myConversion(int r, int g, int b, float[] hsv) {
    float hue = -1;
    float saturation = -1;
    float value = -1;

    float red = (float) (r / 255.0);
    float green = (float) (g / 255.0);
    float blue = (float) (b / 255.0);

    float cMax = Math.max(Math.max(red, green), blue);
    value = cMax;
    float cMin = Math.min(Math.min(red, green), blue);
    float delta = cMax - cMin;

    if (cMax == 0) {
      hue = 0;
      value = 0;
      saturation = 0;
    } else {
      if (delta == 0) {
        hue = 0;
        saturation = (cMax - cMin) / cMax;

      } else {

        saturation = (cMax - cMin) / cMax;

        if (cMax == red)
          hue = (60 * (green - blue) / delta + 0) % 360;
        else if (cMax == green)
          hue = (60 * (blue - red) / delta + 120) % 360;
        else if (cMax == blue)
          hue = (60 * (red - green) / delta + 240) % 360;

        hue /= 360;
      }
    }

    // Stuff

    hsv[0] = hue;
    hsv[1] = saturation;
    hsv[2] = value;

  }

  public Layer brighten(int i) {

    for (var h = 0; h < this.image.getHeight(); h++) {
      for (var w = 0; w < this.image.getWidth(); w++) {

        var pixelInt = this.image.getRGB(w, h);
        var pixelColor = new Color(pixelInt);

        float[] hsv = new float[3];

        myConversion(pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue(), hsv);

        var value = hsv[2];
        value += i / 255.0f;
        value = Math.min(1.0f, Math.max(0, value));
        var newColor = Color.getHSBColor(hsv[0], hsv[1], value);

        float[] again = new float[3];
        myConversion(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), again);

        this.image.setRGB(w, h, newColor.getRGB());
      }
    }
    return this;
  }

  public Layer addContrast(float amount) {
    for (var h = 0; h < this.image.getHeight(); h++) {
      for (var w = 0; w < this.image.getWidth(); w++) {

        var pixelInt = this.image.getRGB(w, h);
        var pixelColor = new Color(pixelInt);

        float[] hsv = new float[3];

        myConversion(pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue(), hsv);

        var value = hsv[2];
        var adjustedValue = value - .5f;
        adjustedValue *= amount;
        adjustedValue += .5;
        adjustedValue = Math.min(1.0f, Math.max(0, adjustedValue));
        var newColor = Color.getHSBColor(hsv[0], hsv[1], adjustedValue);

        float[] again = new float[3];
        myConversion(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), again);

        this.image.setRGB(w, h, newColor.getRGB());
      }
    }

    return this;
  }

  public Layer grayscale() {
    for (var h = 0; h < this.image.getHeight(); h++) {
      for (var w = 0; w < this.image.getWidth(); w++) {

        var pixelInt = this.image.getRGB(w, h);
        var pixelColor = new Color(pixelInt);

        float[] hsv = new float[3];
        myConversion(pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue(), hsv);

        int value = (int) Math.floor(Math.min(255, hsv[2] * 255.0f));

        this.image.setRGB(w, h, new Color(value, value, value).getRGB());
      }
    }
    return this;

  }

  public Layer clone() {

    // See https://stackoverflow.com/a/19327237/10047920
    BufferedImage b = new BufferedImage(this.image.getWidth(), this.image.getHeight(), this.image.getType());
    Graphics2D g = (Graphics2D) b.getGraphics();
    g.drawImage(this.image, 0, 0, null);
    g.dispose();

    Layer toReturn = new Layer(b);
    return toReturn;
  }

  public Color getPixel(int i, int j) {
    return new Color(image.getRGB(i, j));
  }

  public Layer applyCurve(IPixelFunction fun) {
    for (var h = 0; h < this.image.getHeight(); h++) {
      for (var w = 0; w < this.image.getWidth(); w++) {

        var pixelInt = this.image.getRGB(w, h);
        var pixelColor = new Color(pixelInt);

        float[] hsv = new float[3];
        myConversion(pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue(), hsv);

        float value = Math.max(0, Math.min(1, fun.run(hsv[2])));

        this.image.setRGB(w, h, new Color(value, value, value).getRGB());
      }
    }
    return this;
  }

  public Layer bitSlice(int power) {

    BufferedImage b = new BufferedImage(this.image.getWidth(), this.image.getHeight(), this.image.getType());
    for (var h = 0; h < b.getHeight(); h++) {
      for (var w = 0; w < b.getWidth(); w++) {
        // Assume we are in grayscale
        var pixelInt = this.image.getRGB(w, h);
        int value = new Color(pixelInt).getRed();
        int slicer = (int) Math.pow(2, power);
        int sliced = value & slicer;
        Color finalColor = null;
        if (sliced > 0) {
          finalColor = Color.WHITE;
        } else {
          finalColor = Color.BLACK;
        }

        b.setRGB(w, h, finalColor.getRGB());

      }
    }

    Layer toReturn = new Layer(b);
    return toReturn;

  }

  public Layer bitSlice(int powerLow, int powerHigh) {
    if (powerLow == powerHigh)
      return this.bitSlice(powerLow);

    BufferedImage b = new BufferedImage(this.image.getWidth(), this.image.getHeight(), this.image.getType());
    for (var h = 0; h < b.getHeight(); h++) {
      for (var w = 0; w < b.getWidth(); w++) {
        // Assume we are in grayscale
        var pixelInt = this.image.getRGB(w, h);
        int value = new Color(pixelInt).getRed();
        int slicer = 0;
        for (int i = powerLow; i <= powerHigh; i++) {
          slicer |= (int) Math.pow(2, i);

        }
        int sliced = value & slicer;
        sliced <<= 7 - powerHigh;
        Color finalColor = new Color(sliced, sliced, sliced);

        b.setRGB(w, h, finalColor.getRGB());

      }
    }

    Layer toReturn = new Layer(b);
    return toReturn;

  }

  public boolean compareTo(Layer otherImage) {
    // return false;
    if (this.getWidth() != otherImage.getWidth() || this.getHeight() != otherImage.getHeight())
      return false;
    for (int y = 0; y < getHeight(); y++) {
      for (int x = 0; x < getWidth(); x++) {
        Color thisColor = this.getColorAt(x, y);
        Color otherColor = otherImage.getColorAt(x, y);
        if (Helper.sameColor(thisColor, otherColor))
          continue;
        return false;
      }
    }
    return true;
  }

  private Color getColorAt(int x, int y) {
    return new Color(image.getRGB(x, y));
  }

  private int getHeight() {
    return image.getHeight();
  }

  private int getWidth() {
    return image.getWidth();
  }

  public Layer ditherBW() {
    BufferedImage b = new BufferedImage(this.image.getWidth(), this.image.getHeight(), this.image.getType());

    for (var h = 0; h < b.getHeight(); h++) {
      for (var w = 0; w < b.getWidth(); w++) {
        // Assume we are in grayscale
        var pixelInt = this.image.getRGB(w, h);
        int value = new Color(pixelInt).getRed();

        Color finalColor = null;
        if (value > 128)
          finalColor = Color.WHITE;
        else
          finalColor = Color.BLACK;

        b.setRGB(w, h, finalColor.getRGB());

      }
    }
    Layer toReturn = new Layer(b);
    return toReturn;
  }

  public Layer ditherBWFloyd() {
    BufferedImage b = new BufferedImage(this.image.getWidth(), this.image.getHeight(), this.image.getType());

    for (var h = 0; h < b.getHeight(); h++) {
      for (var w = 0; w < b.getWidth(); w++) {
        // Assume we are in grayscale
        var pixelInt = this.image.getRGB(w, h);
        int value = new Color(pixelInt).getRed();

        Color finalColor = null;
        if (value > 128)
          finalColor = Color.WHITE;
        else
          finalColor = Color.BLACK;

        b.setRGB(w, h, finalColor.getRGB());

      }
    }
    Layer toReturn = new Layer(b);
    return toReturn;
  }

  /**
   * Produce an image that only contains the <i>percent</i> most common colors
   * 
   * @param percent The percent [0,1] of the most common colors to preserve
   * @return A new layer where the most common colors are preserved and the rest
   *         are magenta
   */
  public Layer reduceColorPercent(double percent) {
    BufferedImage b = new BufferedImage(this.image.getWidth(), this.image.getHeight(), this.image.getType());

    List<Entry<Integer, Integer>> sorted = getSortedColorCountMap();
    // int toKeep = 256;
    int toKeep = (int) Math.floor(sorted.size() * percent);
    List<Entry<Integer, Integer>> subList = sorted.subList(sorted.size() - toKeep, sorted.size());

    List<Integer> colors = subList.stream().map(i -> i.getKey()).collect(Collectors.toList());
    Set<Integer> colorsHash = new LinkedHashSet<Integer>(colors);

    for (var h = 0; h < b.getHeight(); h++) {
      for (var w = 0; w < b.getWidth(); w++) {

        var pixelInt = this.image.getRGB(w, h);

        Color finalColor = null;
        if (colorsHash.contains(pixelInt))
          finalColor = new Color(pixelInt);
        else
          finalColor = Color.MAGENTA;

        b.setRGB(w, h, finalColor.getRGB());

      }
    }

    // System.out.println("Looping over image");
    Layer toReturn = new Layer(b);
    return toReturn;
  }

  /**
   * Produce an image that only contains the count most common colors
   * 
   * @param count The number of the most common colors to preserve
   * @return A new layer where the most common colors are preserved and the rest
   *         are magenta
   */
  public Layer reduceColorByCount(int count) {
    BufferedImage b = new BufferedImage(this.image.getWidth(), this.image.getHeight(), this.image.getType());

    List<Entry<Integer, Integer>> sorted = getSortedColorCountMap();
    // int toKeep = 256;
    List<Entry<Integer, Integer>> subList = sorted.subList(sorted.size() - count, sorted.size());

    List<Integer> colors = subList.stream().map(i -> i.getKey()).collect(Collectors.toList());
    Set<Integer> colorsHash = new LinkedHashSet<Integer>(colors);

    for (var h = 0; h < b.getHeight(); h++) {
      for (var w = 0; w < b.getWidth(); w++) {

        var pixelInt = this.image.getRGB(w, h);

        Color finalColor = null;
        if (colorsHash.contains(pixelInt))
          finalColor = new Color(pixelInt);
        else
          finalColor = Color.MAGENTA;

        b.setRGB(w, h, finalColor.getRGB());

      }
    }

    // System.out.println("Looping over image");
    Layer toReturn = new Layer(b);
    return toReturn;
  }

  /**
   * Given a percent of the most common colors to preseve, create a new layer with
   * only those pixels preserved.
   * 
   * @param percent The percent of the most common colors to preserve
   * @return An image with the most common colors preserved and the others turned
   *         to magenta. Note that this version is backed by an arraylist, so it
   *         runs very slow.
   */
  public Layer reduceColorNoHash(double percent) {
    BufferedImage b = new BufferedImage(this.image.getWidth(), this.image.getHeight(), this.image.getType());

    HashMap<Integer, Integer> map = getColorCountMap();

    // Get a list and sort
    List<Entry<Integer, Integer>> sorted = new ArrayList<>(map.entrySet());
    sorted.sort(Entry.comparingByValue());

    System.out.println("There are " + sorted.size() + " colors in this image");
    // int toKeep = 256;
    int toKeep = (int) Math.floor(sorted.size() * percent);
    List<Entry<Integer, Integer>> subList = sorted.subList(sorted.size() - toKeep, sorted.size());
    System.out.println("There are " + subList.size() + " colors in the reduced image");
    int count = subList.stream().map(i -> i.getValue()).reduce((a, c) -> a + c).get();
    System.out.println("Which contains " + count + " pixels");
    System.out.println("Which is " + count / (double) (b.getWidth() * b.getHeight()) + " of the image.");
    List<Integer> colors = subList.stream().map(i -> i.getKey()).collect(Collectors.toList());

    System.out.println("Saving image");

    for (var h = 0; h < b.getHeight(); h++) {
      for (var w = 0; w < b.getWidth(); w++) {

        var pixelInt = this.image.getRGB(w, h);

        Color finalColor = null;
        if (colors.contains(pixelInt))
          finalColor = new Color(pixelInt);
        else
          finalColor = Color.MAGENTA;

        b.setRGB(w, h, finalColor.getRGB());

      }
    }

    System.out.println("Looping over image");
    Layer toReturn = new Layer(b);
    return toReturn;
  }

  /**
   * Given the <i>inPercent</i> most common colors in the image, return the number
   * of pixels they cover.
   * 
   * @param inPercent The percent of the most common colors to preserve
   * @return The number of pixels covered by the most common colors
   */
  public int getPixelCountFromColorPercent(double inPercent) {

    List<Entry<Integer, Integer>> sorted = getSortedColorCountMap();

    int toKeep = (int) Math.floor(sorted.size() * inPercent);
    List<Entry<Integer, Integer>> subList = sorted.subList(sorted.size() - toKeep, sorted.size());
    int count = subList.stream().map(i -> i.getValue()).reduce((a, c) -> a + c).get();

    return count;
  }

  /**
   * Given the count most common colors in the image, return the number of pixels
   * they cover.
   * 
   * @param count The number of the most common colors to preserve
   * @return The number of pixels covered by the most common colors
   */
  public int getPixelCountFromColorCount(int _count) {

    List<Entry<Integer, Integer>> sorted = getSortedColorCountMap();

    int toKeep = _count;
    if (toKeep == 65536)
      System.out.println("Bug");
    List<Entry<Integer, Integer>> subList = sorted.subList(sorted.size() - toKeep, sorted.size());
    int count = subList.stream().map(i -> i.getValue()).reduce((a, c) -> a + c).get();

    return count;
  }

  /**
   * Get the total number of colors in the layer
   */
  public int colorCount() {
    return getColorCountMap().entrySet().size();
  }

  /**
   * Get a map of each color (as an integer) with its count in the image
   * 
   * @return
   */
  public HashMap<Integer, Integer> getColorCountMap() {
    HashMap<Integer, Integer> map = new HashMap<>();

    for (var h = 0; h < image.getHeight(); h++) {
      for (var w = 0; w < image.getWidth(); w++) {
        var pixelInt = this.image.getRGB(w, h);

        if (map.get(pixelInt) == null) {
          map.put(pixelInt, 1);
        } else {
          map.put(pixelInt, map.get(pixelInt) + 1);
        }
      }
    }

    return map;
  }

  /**
   * Get a sorted map of colors from the image
   * 
   * @return A sorted list of pixel,count pairs, from fewest to greatest
   */
  public List<Entry<Integer, Integer>> getSortedColorCountMap() {
    HashMap<Integer, Integer> map = getColorCountMap();

    // Get a list and sort
    List<Entry<Integer, Integer>> sorted = new ArrayList<>(map.entrySet());
    sorted.sort(Entry.comparingByValue());
    return sorted;
  }

  /**
   * Use a random palette from the image's original colors to reduce the number of
   * colors in an image
   * 
   * @param count The number of colors to have in the palette
   * @return A new layer with colors reduced to the given amount
   */
  public Layer reduceColorRandomly(int count) {
    Random rand = new Random(0);
    BufferedImage b = new BufferedImage(this.image.getWidth(), this.image.getHeight(), this.image.getType());

    List<Entry<Integer, Integer>> sorted = getSortedColorCountMap();

    var colors = sorted.stream().map(i -> i.getKey()).collect(Collectors.toList());

    List<Integer> palette = new ArrayList<Integer>();
    while (palette.size() < count) {
      var candidateColor = colors.get((int) Math.floor(rand.nextDouble() * sorted.size()));
      if (palette.contains(candidateColor))
        continue;
      // If we get here, we have a unique color
      palette.add(candidateColor);
    }

    // Now remap every color to its closest palette color

    for (var h = 0; h < b.getHeight(); h++) {
      for (var w = 0; w < b.getWidth(); w++) {

        var pixelInt = this.image.getRGB(w, h);
        int closestColor = -1;
        int closestDistance = Integer.MAX_VALUE;

        for (var paletteColor : palette) {
          var distance = colorDistance(pixelInt, paletteColor);
          if (distance < closestDistance) {
            closestDistance = distance;
            closestColor = paletteColor;
          }
        }

        b.setRGB(w, h, closestColor);

      }
    }

    return new Layer(b);
  }

  Random rand;

  public int getRandomColorFromColors(List<Integer> colors) {
    var candidateColor = colors.get((int) Math.floor(rand.nextDouble() * colors.size()));
    return candidateColor;
  }

  public int closestPaletteIndex(int pixelInt, int[] palette) {
    int closestColorIndex = -1;
    int closestDistance = Integer.MAX_VALUE;

    for (var i = 0; i < palette.length; i++) {
      var paletteColor = palette[i];
      var distance = colorDistance(pixelInt, paletteColor);
      if (distance < closestDistance) {
        closestDistance = distance;
        closestColorIndex = i;
      }
    }
    return closestColorIndex;
  }

  /**
   * Use KNN to reduce the number of colors in an image palette
   * 
   * @param count The number of colors to have in the palette
   * @return A new layer with colors reduced to the given amount
   */
  public Layer reduceColorByKNN(int count) {
    rand = new Random(0); // Reseed the random number generator for consistent results
    BufferedImage b = new BufferedImage(this.image.getWidth(), this.image.getHeight(), this.image.getType());

    // Get a list of colors and their counts
    List<Entry<Integer, Integer>> sorted = getSortedColorCountMap();

    // Convert the list of entries to simply a list of colors.
    var colors = sorted.stream().map(i -> i.getKey()).collect(Collectors.toList());

    int[] palette = new int[count];
    int index = 0;

    // Populate the palette with unique random colors.
    while (index < count) {
      var candidateColor = getRandomColorFromColors(colors);
      if (Arrays.stream(palette).anyMatch(i -> i == candidateColor))
        continue;
      // If we get here, we have a unique color
      palette[index++] = candidateColor;
    }

    for (int k = 0; k < 1; k++) {

      // Keep track of each pixel and which palette color it is closest to
      List<Entry<Integer, Integer>>[] matches = new ArrayList[palette.length]; //

      // Initialize the list
      for (var i = 0; i < matches.length; i++) {
        matches[i] = new ArrayList<Entry<Integer, Integer>>();
      }

      // Find the best palette color for each pixel and then add it to the correct
      // list
      for (var entry : sorted) {
        var pixelInt = entry.getKey();
        var closestIndex = closestPaletteIndex(pixelInt, palette);
        matches[closestIndex].add(entry);
      }

      // Move every palette color to its mean
      for (var i = 0; i < palette.length; i++) {
        if (matches[i].size() == 0) {
          // Get a new random color if we have no matches
          palette[i] = getRandomColorFromColors(colors);
          continue;
        }
        // Find the mean of the matches
        int red = 0;
        int green = 0;
        int blue = 0;
        int counts = 0;
        for (var entry : matches[i]) {
          Color color = new Color(entry.getKey());
          red += color.getRed() * entry.getValue();
          green += color.getGreen() * entry.getValue();
          blue += color.getBlue() * entry.getValue();
          counts += entry.getValue();
        }
        red /= counts;
        green /= counts;
        blue /= counts;

        red = (int) Math.floor(red);
        green = (int) Math.floor(green);
        blue = (int) Math.floor(blue);

        // Find the closest color in the matches to this mean
        int[] colorsInMatch = matches[i].stream().map(j -> j.getKey()).mapToInt(Integer::intValue).toArray();
        Color newColor = new Color(red, green, blue);
        int newIndex = closestPaletteIndex(newColor.getRGB(), colorsInMatch);
        palette[i] = colorsInMatch[newIndex];
      }
    }

    // Now remap every color to its closest palette color

    for (var h = 0; h < b.getHeight(); h++) {
      for (var w = 0; w < b.getWidth(); w++) {

        var pixelInt = this.image.getRGB(w, h);
        int closestColor = -1;
        int closestDistance = Integer.MAX_VALUE;

        for (var paletteColor : palette) {
          var distance = colorDistance(pixelInt, paletteColor);
          if (distance < closestDistance) {
            closestDistance = distance;
            closestColor = paletteColor;
          }
        }

        b.setRGB(w, h, closestColor);

      }
    }

    return new Layer(b);
  }

  private int colorDistance(int color1, int color2) {
    var Color1 = new Color(color1);
    var Color2 = new Color(color2);

    var distance = Math.abs(Color1.getRed() - Color2.getRed()) + Math.abs(Color1.getGreen() - Color2.getGreen())
        + Math.abs(Color1.getBlue() - Color2.getBlue());

    return distance;
  }
}
