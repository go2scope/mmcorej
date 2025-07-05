import mmcorej.*;

/**
 * Demo for the sequence acquisition storage
 */
public class StorageSequence {
    public static void main(String[] args) {
        // instantiate MMCore
        CMMCore core = new CMMCore();

        // decide how are we going to call our devices within this script
        String store = "Store";
        String camera = "Camera";

        int numberOfTimepoints = 16; // number of images to acquire
        String saveLocation = ".\\AcData";
        long pid = ProcessHandle.current().pid();
        System.out.println("Current Java process ID (ProcessHandle): " + pid);

        try {
            // enable verbose logging
            core.enableStderrLog(true);
            core.enableDebugLog(true);
            System.out.println(core.getVersionInfo());
            System.out.println(core.getAPIVersionInfo());

            // set device adapter path to the location of binaries
            StrVector searchPaths = new StrVector();
            searchPaths.add("..\\..\\mmCoreAndDevices\\build\\Release\\x64");
            core.setDeviceAdapterSearchPaths(searchPaths);

            // load the demo camera device
            core.loadDevice(camera, "DemoCamera", "DCam");
            core.loadDevice(store, "go2scope", "G2SBigTiffStorage");

            // initialize the system, this will in turn initialize each device
            core.initializeAllDevices();

            // configure the camera device
            core.setProperty(camera, "PixelType", "16bit");
            core.setProperty(camera, "OnCameraCCDXSize", "4432");
            core.setProperty(camera, "OnCameraCCDYSize", "2368");
            core.setExposure(5.0);

            // configure storage device
            core.setProperty(store, "DirectIO", 0);     // do not use DirectIO
            core.setProperty(store, "FlushCycle", 10);   // refresh rate (in frames) for data files

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

            // create the new dataset
            LongVector shape = new LongVector();
            StorageDataType type = StorageDataType.StorageDataType_GRAY16;

            shape.add(numberOfTimepoints); // time points
            shape.add(h); // second dimension y
            shape.add(w); // first dimension x
            int handle = core.createDataset(saveLocation, "acq_test", shape, type, "", 0);

            core.logMessage("START OF ACQUISITION");
            core.startSequenceAcquisition(numberOfTimepoints, 0.0, true);

            for(int i = 0; i < numberOfTimepoints; i++) {

                // let the buffer fill
                boolean firstWait = true;
                while (core.getRemainingImageCount() == 0) {
                    if (firstWait)
                        System.out.println("Waiting for images...");
                    firstWait = false;
                    Thread.sleep(50);
                }

                LongVector coords = new LongVector();
                coords.add(i);
                coords.add(0);
                coords.add(0);

                // take the next image from circular buf
                core.appendNextToDataset(handle, coords, "", 0);
                System.out.println("Saved image " + (i + 1));
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
