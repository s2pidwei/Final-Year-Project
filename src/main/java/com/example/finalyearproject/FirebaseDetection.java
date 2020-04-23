package com.example.finalyearproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.custom.FirebaseCustomLocalModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelInterpreterOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.gesture.GestureAction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;

public class FirebaseDetection extends AppCompatActivity {

    CameraView cameraView;
    Button btnScan;
    boolean isDetected;
    FirebaseModelInterpreter interpreter;
    FirebaseCustomLocalModel localModel;
    FirebaseModelInterpreterOptions options;
    ProgressDialog loadingDialog;
    float[][][][] INPUT_FORMAT = new float[1][300][300][3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);
        cameraView = findViewById(R.id.cameraView);
        btnScan = findViewById(R.id.btnScan);
        isDetected = false;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        localModel = new FirebaseCustomLocalModel.Builder()
                .setAssetFilePath("product_model.tflite")
                .build();

        options = new FirebaseModelInterpreterOptions.Builder(localModel).build();
        try{
            interpreter = FirebaseModelInterpreter.getInstance(options);
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),
                    "Error encountered. Please try again later", Toast.LENGTH_SHORT).show();
            Log.d("TAG_ERROR:", e.getMessage());
        }

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                super.onPictureTaken(result);
                result.toBitmap(new BitmapCallback() {
                    @Override
                    public void onBitmapReady(@Nullable Bitmap bitmap) {
                        if(bitmap.getWidth()<bitmap.getHeight()){
                            bitmap = RotateBitmap(bitmap,270);
                        }
                        
                        bitmap = cropToSquare(bitmap);
                        objectDetection(bitmap);
                    }
                });
            }
        });

        //Scan Button function
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingDialog = ProgressDialog.show(FirebaseDetection.this, "",
                        "Processing. Please wait...", true);
                cameraView.takePicture();
            }
        });

        //Check Camera Permission
        Dexter.withActivity(FirebaseDetection.this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        setupCamera();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(FirebaseDetection.this, "Camera Permission is required.", Toast.LENGTH_SHORT).show();
                        finishAffinity();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap cropToSquare(Bitmap bitmap){
        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width)? height - ( height - width) : height;
        int cropW = (width - height) / 2;
        cropW = (cropW < 0)? 0: cropW;
        int cropH = (height - width) / 2;
        cropH = (cropH < 0)? 0: cropH;
        Bitmap cropImg = Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight);

        return cropImg;
    }

    private void setupCamera(){
        //Set up Camera
        cameraView = findViewById(R.id.cameraView);
        cameraView.setAudio(Audio.OFF);
        cameraView.setPlaySounds(false);
        cameraView.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS);
        cameraView.setLifecycleOwner(this);
    }

    private void objectDetection(Bitmap bitmap){
        try {
            float [][][][] input = convertBitmapTo4dFloat(bitmap);
            // Object detection input options
            FirebaseModelInputOutputOptions inputOutputOptions =
                    new FirebaseModelInputOutputOptions.Builder()
                            .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 300, 300, 3})
                            .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 10, 4}) // bounding box output
                            .setOutputFormat(1, FirebaseModelDataType.FLOAT32, new int[]{1, 10}) // classes number output
                            .setOutputFormat(2, FirebaseModelDataType.FLOAT32, new int[]{1, 10}) // confidence level output
                            .build();
            FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                    .add(input)  // add() as many input arrays as your model requires
                    .build();
            // Run object detection model
            Objects.requireNonNull(interpreter).run(inputs, inputOutputOptions)
                    .addOnSuccessListener(
                            new OnSuccessListener<FirebaseModelOutputs>() {
                                @Override
                                public void onSuccess(FirebaseModelOutputs result) {
                                    float[][][] boxes_array = result.getOutput(0); // position of detected item
                                    float[][] classes_array = result.getOutput(1); // classes number (item in label.txt)
                                    float[][] confidence_array = result.getOutput(2); // confidence level on detected item
                                    float[] classes = classes_array[0];
                                    float[] confidence = confidence_array[0];
                                    ArrayList<String> className = new ArrayList<>();
                                    String[] item = new String[10];
                                    try {
                                        BufferedReader reader = new BufferedReader(
                                                new InputStreamReader(getAssets().open("label.txt")));
                                        String label;
                                        while( (label = reader.readLine()) != null) {
                                            className.add(label);
                                        }

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    for (int i = 0; i<item.length;i++){
                                        int classId = (int) classes[i];
                                        item[i] = className.get(classId);
                                    }

                                    Bundle detectedProduct = new Bundle();
                                    detectedProduct.putStringArray("name",item);
                                    detectedProduct.putFloatArray("confidence",confidence);
                                    Intent intent = new Intent(getBaseContext(),ScannedResultActivity.class);
                                    intent.putExtra("detectedProduct",detectedProduct);
                                    intent.putExtra("method","detect");
                                    loadingDialog.dismiss();
                                    startActivity(intent);
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getApplicationContext(),
                                            "Error encountered. Please try again later", Toast.LENGTH_SHORT).show();
                                    Log.d("TAG_ERROR2:", e.getMessage());
                                }
                            });
        } catch (FirebaseMLException e) {
            Toast.makeText(getApplicationContext(),
                    "Error encountered. Please try again later", Toast.LENGTH_SHORT).show();
            Log.d("TAG_ERROR:", e.getMessage());
        }

    }

    private float[][][][] convertBitmapTo4dFloat(Bitmap bitmap) {
        bitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
        int batchNum = 0;
        float[][][][] input = INPUT_FORMAT;
        for (int x = 0; x < 300; x++) {
            for (int y = 0; y < 300; y++) {
                int pixel = bitmap.getPixel(x, y);

                input[batchNum][x][y][0] = (Color.red(pixel) - 127.5f) / 127.5f;
                input[batchNum][x][y][1] = (Color.green(pixel) - 127.5f) / 127.5f;
                input[batchNum][x][y][2] = (Color.blue(pixel) - 127.5f) / 127.5f;
            }
        }
        return input;
    }

}
