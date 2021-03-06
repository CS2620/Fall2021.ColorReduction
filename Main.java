import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

public class Main {

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        //colorReduceReveal();
        //colorReduceRandom();
        //colorReduce();
        combineImage();
        
    }

    private void combineImage() {
        //Create image pairs that we are intersted in
        var names = new String[]{"horse", "mars-moon-phobos", "rotovirus", "square"};
        var sizes = new String[]{"1", "2", "4", "8", "16", "32", "64", "128", "256"};
        var variations = new String[]{"color-reduced-", "color-reduced-random-"};
        for(var name : names){
            for(var size : sizes){
                String[] options = new String[variations.length];
                for(var i = 0; i < options.length; i++){
                    options[i] = variations[i] + name + "-" + size + ".png";
                }
                System.out.println();
                System.out.println(name + "-" + size + "-");
                for(var filename : options){
                    System.out.println(filename);
                }
                Processor p = new Processor("./out/" + options[0]);
                p.canvasWidth *= 2;
                p.addLayer("./out/" + options[1]);
                p.currentLayer().dx = p.currentLayer().image.getWidth();
                p.saveLayers(new int[]{0,1}, "./temp/" + options[0]);
                //System.out.println(options);
            }
        }
    }

    public void colorReduce() {
        System.out.println("Color Reduction");
        colorReduce("horse", "png");
        colorReduce("mars-moon-phobos", "jpg");
        colorReduce("rotovirus", "jpg");
        colorReduce("square", "png");
    }

    public void colorReduceRandom() {
        System.out.println("Color Reduction Random");
        colorReduceRandom("horse", "png");
        colorReduceRandom("mars-moon-phobos", "jpg");
        colorReduceRandom("rotovirus", "jpg");
        colorReduceRandom("square", "png");
    }

    //Do a color reduction using KNN
    private void colorReduce(String filename, String extension) {
        System.out.println();
        System.out.println("----------------");
        System.out.println("Reducing colors in file: " + filename);

        var start = new Processor("./in/" + filename + "." + extension);
        var image = start.currentLayer().image;
        int colorCount = image.colorCount();
        System.out.println("There are " + colorCount + " colors in the image");

        int currentColors = 1;
        while (currentColors < colorCount && currentColors <= 256) {
            System.out.println();
            System.out.println("Saving " + currentColors + "/" + colorCount + " of the colors.");

            int count = start.currentLayer().image.getPixelCountFromColorCount(currentColors);
            System.out.println(
                    "Preserves " + count / (double) (image.image.getWidth() * image.image.getHeight()) + " pixels.");
            int cc = currentColors;
            start.addLayer(i -> i.reduceColorByKNN(cc));
            start.saveCurrentLayer("./out/color-reduced-" + filename + "-" + currentColors + ".png");
            start.setCurrentLayer(0);
            currentColors *= 2;
            // currentColors = Math.min(colorCount, currentColors);
        }
    }

    //Do a color reduction algorithm where we randomly pick the palette colors
    private void colorReduceRandom(String filename, String extension) {
        System.out.println();
        System.out.println("----------------");
        System.out.println("(Randomly) Reducing colors in file: " + filename);

        var start = new Processor("./in/" + filename + "." + extension);
        var image = start.currentLayer().image;
        int colorCount = image.colorCount();
        System.out.println("There are " + colorCount + " colors in the image");

        int currentColors = 1;
        while (currentColors < colorCount && currentColors <= 256) {
            System.out.println();
            System.out.println("Saving (Random Reduction) " + currentColors + "/" + colorCount + " of the colors.");

            int count = start.currentLayer().image.getPixelCountFromColorCount(currentColors);
            System.out.println(
                    "Preserves " + count / (double) (image.image.getWidth() * image.image.getHeight()) + " pixels.");
            int cc = currentColors;
            start.addLayer(i -> i.reduceColorRandomly(cc));
            start.saveCurrentLayer("./out/color-reduced-random-" + filename + "-" + currentColors + ".png");
            start.setCurrentLayer(0);
            currentColors *= 2;
            // currentColors = Math.min(colorCount, currentColors);
        }
    }

    public void colorReduceReveal() {
        System.out.println("Color Reduction/Reveal");
        colorReduceReveal("horse", "png");
        colorReduceReveal("mars-moon-phobos", "jpg");
        colorReduceReveal("rotovirus", "jpg");
        colorReduceReveal("square", "png");
    }

    private void colorReduceReveal(String filename, String extension) {
        System.out.println();
        System.out.println("----------------");
        System.out.println("Reducing/Revealing colors in file: " + filename);

        var start = new Processor("./in/" + filename + "." + extension);
        var image = start.currentLayer().image;
        int colorCount = image.colorCount();
        System.out.println("There are " + colorCount + " colors in the image");

        int currentColors = 1;
        while (currentColors < colorCount) {
            System.out.println();
            System.out.println("Saving (Reduce Reveal)" + currentColors + "/" + colorCount + " of the colors.");

            int count = start.currentLayer().image.getPixelCountFromColorCount(currentColors);
            System.out.println(
                    "Preserves " + count / (double) (image.image.getWidth() * image.image.getHeight()) + " pixels.");
            int cc = currentColors;
            start.addLayer(i -> i.reduceColorByCount(cc));
            start.saveCurrentLayer("./out/color-reduced-reveal-" + filename + "-" + currentColors + ".png");
            start.setCurrentLayer(0);
            currentColors *= 16;
            // currentColors = Math.min(colorCount, currentColors);
        }
    }

    public void dithering() {

        System.out.println("Normal Dithering");
        dither("horse", "png");
        dither("mars-moon-phobos", "jpg");
        dither("rotovirus", "jpg");
        dither("square", "png");

        System.out.println("Floyd-Steinberg Dithering");
        ditherF("horse", "png");
        ditherF("mars-moon-phobos", "jpg");
        ditherF("rotovirus", "jpg");
        ditherF("square", "png");

    }

    private void dither(String filename, String extension) {
        // Create the dithered image
        var start = new Processor("./in/" + filename + "." + extension).grayscale();
        start.addLayer(image -> image.ditherBW());
        start.saveCurrentLayer("./out/dithered-" + filename + ".png");

        // Create grayscale version
        start = new Processor("./in/" + filename + "." + extension).grayscale();
        start.saveCurrentLayer("./out/grayscale-" + filename + ".png");

        // Compare the file sizes
        var ditherSize = new File("./out/dithered-" + filename + ".png").length();
        var grayscaleSize = new File("./out/grayscale-" + filename + ".png").length();
        System.out.println(filename + ": " + (ditherSize / (double) grayscaleSize) + " compression ratio.");

    }

    private void ditherF(String filename, String extension) {
        // Create the dithered image
        var start = new Processor("./in/" + filename + "." + extension).grayscale();
        start.addLayer(image -> image.ditherBWFloyd());
        start.saveCurrentLayer("./out/ditheredF-" + filename + ".png");

        // Create grayscale version
        start = new Processor("./in/" + filename + "." + extension).grayscale();
        start.saveCurrentLayer("./out/grayscale-" + filename + ".png");

        // Compare the file sizes
        var ditherSize = new File("./out/ditheredF-" + filename + ".png").length();
        var grayscaleSize = new File("./out/grayscale-" + filename + ".png").length();
        System.out.println(filename + ": " + (ditherSize / (double) grayscaleSize) + " compression ratio.");

    }

    private static String[] fileFormats = null;

    public static String[] getFileFormats() {
        if (fileFormats != null) {
            return fileFormats;
        }

        // Get the list of supported file formats
        var names = ImageIO.getWriterFormatNames();

        // Use a set to remove endings that differ only by case
        Set<String> toKeep = new HashSet<>();

        // Remove redundant file endings or ones we don't want
        Collection<String> ignore = Arrays.asList(new String[] { "tiff", "jpeg", "wbmp" });
        for (int i = 0; i < names.length; ++i) {
            String name = names[i].toLowerCase();
            if (ignore.contains(name))
                continue;
            toKeep.add(name);
        }
        toKeep.add("ppm");

        fileFormats = toKeep.toArray(new String[0]);

        return fileFormats;
    }

    public void doCustomFormat(String filename, String ending) {
        var start = new Processor("./in/" + filename + "." + ending);
        start.saveCurrentLayer("./out/" + filename + ".custom");
        var end = new Processor("./out/" + filename + ".custom");

        System.out.println("Are the images the same? " + end.compareTo(start));

    }

    public void doListFormats() {
        System.out.println("The following are the file formats supported by your version of Java:");
        String[] endings = getFileFormats();
        for (var ending : endings) {
            System.out.println(ending);
        }
        System.out.println();
    }

    public void doCalculateCompressionRatio(String filename, String extension) {

        var standardFileFormats = getFileFormats();
        var start = new Processor("./in/" + filename + "." + extension);
        for (var ending : standardFileFormats) {
            start.saveCurrentLayer("./out/horse." + ending);
        }

        var compareSize = new File("./out/" + filename + "." + extension).length();

        // Get the sizes of the files
        for (var ending : standardFileFormats) {
            var size = new File("./out/horse." + ending).length();
            var ratio = compareSize / (double) size;
            System.out.println("Compression ratio compared to " + ending + ": " + ratio);
        }

    }

    public void doTestFileFormats() {
        var start = new Processor("./in/horse.png");
        start.saveCurrentLayer("./out/horse.ppm");
        var end = new Processor("./out/horse.ppm");
        var equal = end.compareTo(start);
        System.out.println(equal);
    }

    public void doBitSlicing() {
        var start = new Processor("./in/horse.png");

        for (int i = 0; i < 8; i++) {
            final int j = i;
            start.addLayer(image -> image.bitSlice(j));
            start.saveLayers(new int[] { 1 }, "./out/sliced-" + i + ".png");
            start.popLayer();
        }

        for (int i = 0; i < 8; i++) {
            start = new Processor("./in/horse.jpg").grayscale();
            final int j = i;
            start.addLayer(image -> image.bitSlice(0, j));

            start.saveCurrentLayer("./out/sliced-combined-" + 0 + "-" + i + ".png");

        }

        for (int i = 0; i < 8; i++) {
            start = new Processor("./in/horse.jpg").grayscale();
            final int j = i;
            start.addLayer(image -> image.bitSlice(j, 7));

            start.saveCurrentLayer("./out/sliced-combined2-" + i + "-" + 7 + ".png");

        }
    }

    public void doBrightening() {
        var start = new Processor("./in/horse.png");

        for (var i = 1; i < 10; i++) {
            start.push().brighten(i * 10).addLayer(image -> image.histogram(100))
                    .saveLayers(new int[] { 1, 2 }, "./out/brighten" + i * 10 + ".png").popLayer().popLayer();
        }
    }

    public void doApplyCurve() {
        var start = new Processor("./in/horse.png");

        IPixelFunction ipf = new IPixelFunction() {

            @Override
            public float run(float input) {
                return input;
                // return (float)Math.pow(input, .3);
                // return 1-input;
                // return input < .5 ? 0 : 1;

            }

        };

        start.applyCurve(ipf).addLayer(Processor.ImageFromFunction(ipf)).saveLayers(new int[] { 0, 1 },
                "./out/pixel-function.png");
    }
}
