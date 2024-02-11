package com.codelytical.lyticalrecognition.faceantispoofing;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import com.codelytical.lyticalrecognition.utils.MyUtil;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FaceAntiSpoofing {
    private static final String MODEL_FILE = "FaceAntiSpoofing.tflite";

    public static final int INPUT_IMAGE_SIZE = 256;
    public static final float THRESHOLD = 0.2f;

    public static final int ROUTE_INDEX = 6;

    public static final int LAPLACE_THRESHOLD = 50;
    public static final int LAPLACIAN_THRESHOLD = 1000;

    private Interpreter interpreter;

    public FaceAntiSpoofing(AssetManager assetManager) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(MyUtil.loadModelFile(assetManager, MODEL_FILE), options);
    }

    /**
     * Liveness detection
     * @param bitmap
     * @return 评分
     */
    public float antiSpoofing(Bitmap bitmap) {
        //Resize the face to 256X256, because the shape of the placeholder that requires feed data below is (1, 256, 256, 3)
        Bitmap bitmapScale = Bitmap.createScaledBitmap(bitmap, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);

        float[][][] img = normalizeImage(bitmapScale);
        float[][][][] input = new float[1][][][];
        input[0] = img;
        float[][] clss_pred = new float[1][8];
        float[][] leaf_node_mask = new float[1][8];
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(interpreter.getOutputIndex("Identity"), clss_pred);
        outputs.put(interpreter.getOutputIndex("Identity_1"), leaf_node_mask);
        interpreter.runForMultipleInputsOutputs(new Object[]{input}, outputs);

        Log.i("FaceAntiSpoofing", "[" + clss_pred[0][0] + ", " + clss_pred[0][1] + ", "
                + clss_pred[0][2] + ", " + clss_pred[0][3] + ", " + clss_pred[0][4] + ", "
                + clss_pred[0][5] + ", " + clss_pred[0][6] + ", " + clss_pred[0][7] + "]");
        Log.i("FaceAntiSpoofing", "[" + leaf_node_mask[0][0] + ", " + leaf_node_mask[0][1] + ", "
                + leaf_node_mask[0][2] + ", " + leaf_node_mask[0][3] + ", " + leaf_node_mask[0][4] + ", "
                + leaf_node_mask[0][5] + ", " + leaf_node_mask[0][6] + ", " + leaf_node_mask[0][7] + "]");

        return leaf_score1(clss_pred, leaf_node_mask);
    }

    private float leaf_score1(float[][] clss_pred, float[][] leaf_node_mask) {
        float score = 0;
        for (int i = 0; i < 8; i++) {
            score += Math.abs(clss_pred[0][i]) * leaf_node_mask[0][i];
        }
        return score;
    }

    private float leaf_score2(float[][] clss_pred) {
        return clss_pred[0][ROUTE_INDEX];
    }

    /**
     * Normalize the image to [0, 1]
     * @param bitmap
     * @return
     */
    public static float[][][] normalizeImage(Bitmap bitmap) {
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();
        float[][][] floatValues = new float[h][w][3];

        float imageStd = 255;
        int[] pixels = new int[h * w];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, w, h);
        for (int i = 0; i < h; i++) { // 注意是先高后宽
            for (int j = 0; j < w; j++) {
                final int val = pixels[i * w + j];
                float r = ((val >> 16) & 0xFF) / imageStd;
                float g = ((val >> 8) & 0xFF) / imageStd;
                float b = (val & 0xFF) / imageStd;

                float[] arr = {r, g, b};
                floatValues[i][j] = arr;
            }
        }
        return floatValues;
    }

    /**
     * Laplacian algorithm calculates clarity
     * @param bitmap
     * @return 分数
     */
    public int laplacian(Bitmap bitmap) {
        //Resize the face to 256X256, because the shape of the placeholder that requires feed data below is (1, 256, 256, 3)
        Bitmap bitmapScale = Bitmap.createScaledBitmap(bitmap, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);

        int[][] laplace = {{0, 1, 0}, {1, -4, 1}, {0, 1, 0}};
        int size = laplace.length;
        int[][] img = MyUtil.convertGreyImg(bitmapScale);
        int height = img.length;
        int width = img[0].length;

        int score = 0;
        for (int x = 0; x < height - size + 1; x++){
            for (int y = 0; y < width - size + 1; y++){
                int result = 0;
                // 对size*size区域进行卷积操作
                for (int i = 0; i < size; i++){
                    for (int j = 0; j < size; j++){
                        result += (img[x + i][y + j] & 0xFF) * laplace[i][j];
                    }
                }
                if (result > LAPLACE_THRESHOLD) {
                    score++;
                }
            }
        }
        return score;
    }
}
