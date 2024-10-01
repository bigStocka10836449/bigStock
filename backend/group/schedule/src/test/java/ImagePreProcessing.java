import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class ImagePreProcessing {

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // 讀取原始圖片
        Mat image = Imgcodecs.imread("C:\\Users\\a2640\\OneDrive\\圖片\\fortest\\screenshot11725691425013108626.png");

        // 將圖片轉換為灰度
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);

        // 調整對比度
        Core.multiply(image, new Scalar(1.5), image);

        // 使用高斯模糊來減少噪音
        Imgproc.GaussianBlur(image, image, new Size(3, 3), 0);

        // 儲存處理後的圖片
        Imgcodecs.imwrite("D:\\bigStock\\backend\\group\\schedule\\src\\test\\processed_image.png", image);

        System.out.println("D:\\bigStock\\backend\\group\\schedule\\src\\test\\pprocessed_image.png");
    }
}
