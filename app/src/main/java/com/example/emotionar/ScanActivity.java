package com.example.emotionar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class ScanActivity extends AppCompatActivity {

    Button camara_btn;
    ProgressBar pb;
    int counter = 0;
    PreviewView previewView;
    private ImageCapture imageCapture;
    private Handler handler = new Handler();
    private Runnable progressRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        previewView = findViewById(R.id.imageView8);
        camara_btn = findViewById(R.id.camara_btn);
        pb = findViewById(R.id.pb);
        pb.setMax(100);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        camara_btn.setOnClickListener(view -> {
            startProgressBar();
            captureImage();
        });

        requestCameraPermission(); // Request camera permission and start the camera

        // Initialize and start the progress bar
        Prog();
    }

    private void Prog() {
        Timer t = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                counter++;
                pb.setProgress(counter);
                if (counter >= pb.getMax()) {
                    t.cancel(); // Stop the timer when progress reaches max
                }
            }
        };
        t.schedule(tt, 0, 100);
    }

    // Request camera permissions
    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to use this app", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // Handle any errors (including cancellation) here.
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        imageCapture = new ImageCapture.Builder().build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void captureImage() {
        if (imageCapture == null) {
            Log.e("ImageCapture", "Image capture is not initialized");
            return;
        }

        counter = 0;
        pb.setProgress(counter);

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Log.d("ImageCapture", "Image captured successfully");

                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                // Rotate the image if necessary
                bitmap = rotateImageIfRequired(bitmap);

                File imageFile = saveImageToFile(bitmap);
                if (imageFile != null) {
                    Intent intent = new Intent(ScanActivity.this, IdentifierActivity.class);
                    intent.putExtra("imageFilePath", imageFile.getAbsolutePath());

                    image.close(); // Ensure the image is closed
                    stopProgressBar();
                    startActivity(intent);
                    finish(); // Finish ScanActivity to prevent it from being accessible after starting IdentifierActivity

                    // Update progress bar to 100%
                    counter = pb.getMax();
                    pb.setProgress(counter);
                } else {
                    Log.d("ImageCapture", "Failed to save image to file");
                    stopProgressBar();
                    Toast.makeText(ScanActivity.this, "Failed to save image", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("ImageCapture", "Image capture failed", exception);
                stopProgressBar();
            }
        });
    }

    private Bitmap rotateImageIfRequired(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(270); // Rotate image by 270 degrees
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private File saveImageToFile(Bitmap bitmap) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "captured_image.jpg");
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            outputStream.write(byteArray);
            return file;
        } catch (IOException e) {
            Log.e("ImageCapture", "Failed to save image to file", e);
            return null;
        }
    }

    private void startProgressBar() {
        pb.setVisibility(View.VISIBLE);
        counter = 0;
        pb.setProgress(counter);

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                counter += 5; // Increment counter by 5 for faster progress (adjust as needed)
                pb.setProgress(counter);
                if (counter < pb.getMax()) {
                    handler.postDelayed(this, 49); // Update every 50 ms for faster progress (adjust as needed)
                }
            }
        };
        handler.post(progressRunnable);
    }


    private void stopProgressBar() {
        handler.removeCallbacks(progressRunnable);
        pb.setVisibility(View.GONE);
        counter = pb.getMax();
        pb.setProgress(counter);
    }
}
