package com.example.emotionar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class IdentifierActivity extends AppCompatActivity {

    private static final String TAG = "IdentifierActivity";
    private ImageView imageView;
    private Button ok_btn, reject_btn;
    private TextView textemotion;
    private Interpreter tflite;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identifier);

        imageView = findViewById(R.id.imageView10);
        ok_btn = findViewById(R.id.ok_btn);
        reject_btn = findViewById(R.id.reject_btn);
        textemotion = findViewById(R.id.textemotion);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String imageFilePath = getIntent().getStringExtra("imageFilePath");
        if (imageFilePath != null) {
            File imageFile = new File(imageFilePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath);
                imageView.setImageBitmap(bitmap);
                try {
                    tflite = new Interpreter(loadModelFile());
                    String emotion = identifyEmotion(bitmap);
                    textemotion.setText(emotion);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "Image file does not exist: " + imageFilePath);
            }
        }

        ok_btn.setOnClickListener(v -> {
            // Handle OK button click
        });

        reject_btn.setOnClickListener(v -> {
            Intent intent = new Intent(IdentifierActivity.this, ScanActivity.class);
            startActivity(intent);
            finish(); // Finish IdentifierActivity to prevent it from being accessible after starting ScanActivity
        });
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("new_emotion_detection_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private String identifyEmotion(Bitmap bitmap) {
        // Preprocess the image to match the model input
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true);
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        // Prepare input and output buffers
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 48, 48, 1}, DataType.FLOAT32);
        inputFeature0.loadBuffer(inputBuffer);

        TensorBuffer outputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 7}, DataType.FLOAT32); // Assuming 7 emotions

        // Run the model
        tflite.run(inputFeature0.getBuffer(), outputFeature0.getBuffer());

        // Get the result
        float[] emotionProbabilities = outputFeature0.getFloatArray();
        return getEmotionFromProbabilities(emotionProbabilities);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 48 * 48 * 1);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[48 * 48];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int pixelValue : intValues) {
            // Convert the pixel to grayscale and normalize
            int r = (pixelValue >> 16) & 0xFF;
            int g = (pixelValue >> 8) & 0xFF;
            int b = pixelValue & 0xFF;
            float normalizedPixelValue = ((r + g + b) / 3.0f) / 255.0f;
            byteBuffer.putFloat(normalizedPixelValue);
        }
        return byteBuffer;
    }

    private String getEmotionFromProbabilities(float[] emotionProbabilities) {
        // Assuming the model output is an array of probabilities for 7 emotions
        String[] emotions = {"Angry", "Disgust", "Fear", "Happy", "Sad", "Surprise", "Neutral"};
        int maxIndex = 0;
        float maxProbability = 0;

        for (int i = 0; i < emotionProbabilities.length; i++) {
            if (emotionProbabilities[i] > maxProbability) {
                maxProbability = emotionProbabilities[i];
                maxIndex = i;
            }
        }
        return emotions[maxIndex];
    }
}
