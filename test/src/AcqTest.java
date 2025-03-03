import mmcorej.CMMCore;
import mmcorej.StrVector;
import mmcorej.TaggedImage;

public class AcqTest {
    public static void main(String[] args) {
        // instantiate MMCore
        CMMCore core = new CMMCore();

        // decide how are we going to call our devices within this script
        String store = "Store";
        String camera = "Camera";

        try {
            // enable verbose logging
            core.enableStderrLog(true);
            core.enableDebugLog(true);
            System.out.println(core.getVersionInfo());
            System.out.println(core.getAPIVersionInfo());

            // set device adapter path to default
            StrVector searchPaths = new StrVector();
            searchPaths.add("C:\\Program Files\\Micro-Manager-2.0");
            core.setDeviceAdapterSearchPaths(searchPaths);

            // load the demo camera device
            core.loadDevice(camera, "DemoCamera", "DCam");

            // initialize the system, this will in turn initialize each device
            core.initializeAllDevices();

            // configure the camera device
            core.setProperty(camera, "PixelType", "16bit");
            core.setProperty(camera, "OnCameraCCDXSize", "4432");
            core.setProperty(camera, "OnCameraCCDYSize", "2368");
            core.setExposure(5.0);

            core.setCircularBufferMemoryFootprint(8192);
            core.clearCircularBuffer();

            // take one image to "warm up" the camera and get actual image dimensions
            core.snapImage();
            int w = (int)core.getImageWidth();
            int h = (int)core.getImageHeight();

            // fetch the image with metadata
            TaggedImage img = core.getTaggedImage();
            System.out.printf("Image %d X %d, size %d", w, h, ((short[])(img.pix)).length);

            // print the metadata provided by MMCore
            System.out.println(img.tags.toString());

            int imgind = 0;
            int imgcount = 16;
            core.logMessage("START OF ACQUISITION");
            core.startSequenceAcquisition(imgcount, 0.0, true);

            for(int i = 0; i < imgcount; i++) {
                // let the buffer fill
                while (core.getRemainingImageCount() == 0) {
                    System.out.println("Waiting for images...");
                    Thread.sleep(20);
                }
                // fetch the image
                img = core.popNextTaggedImage();
                System.out.println("Got image " + (i + 1));

                // Add image index to the image metadata
                img.tags.put("Image-index", imgind);

                // add image to stream
                short[] bx = (short[]) img.pix;

                imgind++;
                if (core.isBufferOverflowed())
                    break;
            }
            long endAcq = System.nanoTime();
            core.stopSequenceAcquisition();

            core.logMessage("END OF ACQUISITION");
            long end = System.nanoTime();

            // unload all devices (not really necessary)
            core.unloadAllDevices();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
