package cornerstone;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class Camera {
    private VideoCapture capture;
    private int cameraId;

    static {
    	System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
    }

    public Camera() {
        this(0);
    }

    public Camera(int cameraId) {
        this.cameraId = cameraId;
        capture = new VideoCapture();
        capture.open(this.cameraId);
    }

    public BufferedImage getImage() {
        if (!capture.isOpened()) {
        	open();
        	
        	if(!capture.isOpened()) {
        		return null;
        	}
        } 
        
        Mat frame = grabFrame();
//        Core.flip(frame, frame, +1);
        BufferedImage image = matToBufferedImage(frame);
        frame.release();
        frame = null;
        return image;
    }

    public Boolean isOpen() {
        return capture.isOpened();
    }

    public void open() {
        capture.open(cameraId);
    }

    private Mat grabFrame() {
        Mat frame = new Mat();
        capture.read(frame);
        return frame;
    }

    private static BufferedImage matToBufferedImage(Mat original) {
        // init
        BufferedImage image = null;
        int width = original.width(), height = original.height(), channels = original.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        original.get(0, 0, sourcePixels);

        if (original.channels() > 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }
}
