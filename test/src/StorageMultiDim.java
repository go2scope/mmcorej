import mmcorej.*;

/**
 * Demo for the sequence acquisition storage
 */
public class StorageMultiDim {
    public static void main(String[] args) {
        // instantiate MMCore
        CMMCore core = new CMMCore();

        // decide how are we going to call our devices within this script
        String store = "Store";
        String camera = "Camera";

        int numberOfTimepoints = 5;
        int numberOfChannels = 4;
        int numberOfPositions = 3;

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

            shape.add(numberOfPositions); // positions
            shape.add(numberOfTimepoints); // time points
            shape.add(numberOfChannels); // channels
            shape.add(h); // second dimension y
            shape.add(w); // first dimension x
            int handle = core.createDataset(saveLocation, "MultiDim", shape, type, "", 0);

            core.logMessage("Dataset UID: " + handle);
            int cap = core.getBufferFreeCapacity();
            System.out.println("Circular buffer free: " + cap + ", acquiring images " + numberOfTimepoints * numberOfChannels * numberOfPositions);
            core.logMessage("START OF ACQUISITION");

            for(int i = 0; i < numberOfPositions; i++) {
                for(int j = 0; j < numberOfTimepoints; j++) {
                    for(int k = 0; k < numberOfChannels; k++) {
                        // create coordinates for the image
                        LongVector coords = new LongVector();
                        coords.add(i);
                        coords.add(j);
                        coords.add(k);
                        coords.add(0);
                        coords.add(0);

                        core.snapAndAppendToDataset(handle, coords, "", 0);

                        System.out.printf("Image saved: P=%d, T=%d, C=%d\n", i, j, k);
                    }
                    if (core.isBufferOverflowed())
                        break;
                }
                if (core.isBufferOverflowed())
                    break;
            }
            // we are done so close the dataset
            core.closeDataset(handle);

            core.logMessage("END OF ACQUISITION");
            long end = System.nanoTime();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
