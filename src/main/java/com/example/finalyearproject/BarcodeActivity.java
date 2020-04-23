package com.example.finalyearproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.gesture.GestureAction;

import java.util.List;
import java.util.Objects;

public class BarcodeActivity extends AppCompatActivity {

    CameraView cameraView;

    FirebaseVisionBarcodeDetectorOptions options;
    FirebaseVisionBarcodeDetector detector;
    boolean isDetected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        isDetected = false;
        Dexter.withActivity(BarcodeActivity.this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        setupCamera();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(BarcodeActivity.this, "Camera Permission is required.", Toast.LENGTH_SHORT).show();
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

    private void setupCamera(){
        //Select Type of barcode
        options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(
                        FirebaseVisionBarcode.FORMAT_QR_CODE,
                        FirebaseVisionBarcode.FORMAT_EAN_13)
                .build();
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);

        //Set up Camera
        cameraView = findViewById(R.id.cameraView);

        cameraView.setAudio(Audio.OFF);
        cameraView.setPlaySounds(false);
        cameraView.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS);
        cameraView.setLifecycleOwner(this);
        cameraView.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                processImage(getVisionImageFromFrame(frame));
            }
        });
    }

    private void processImage(FirebaseVisionImage image) {
        if(!isDetected){
            detector.detectInImage(image)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                            processedResult(firebaseVisionBarcodes);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(BarcodeActivity.this, "No", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void processedResult(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
        boolean redirectIntent = false;
        if(firebaseVisionBarcodes.size() > 0 && !isDetected){
            isDetected = true;
            Intent intent = new Intent();
            for (FirebaseVisionBarcode barcode: firebaseVisionBarcodes) {
                Rect bounds = barcode.getBoundingBox();
                Point[] corners = barcode.getCornerPoints();

                String rawValue = barcode.getRawValue();
                int valueType = barcode.getValueType();
                switch (valueType) {
                    case FirebaseVisionBarcode.TYPE_TEXT:
                        //showAlertMsg(rawValue);
                        intent = new Intent(this, ScannedResultActivity.class);
                        intent.putExtra("result",rawValue);
                        intent.putExtra("method","qr");
                        redirectIntent = true;
                        break;
                    case FirebaseVisionBarcode.TYPE_WIFI:
                        String ssid = Objects.requireNonNull(barcode.getWifi()).getSsid();
                        String password = barcode.getWifi().getPassword();
                        int type = barcode.getWifi().getEncryptionType();
                        intent = new Intent(this, ScannedResultActivity.class);
                        intent.putExtra("result","SSID: " + ssid +
                                "\nPassword: " + password + "\nType: " + type);
                        intent.putExtra("method","qr");
                        redirectIntent = true;
                        break;
                    case FirebaseVisionBarcode.TYPE_URL:
                        String title = Objects.requireNonNull(barcode.getUrl()).getTitle();
                        String url = barcode.getUrl().getUrl();
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(rawValue));
                        redirectIntent = true;
                        break;
                    case FirebaseVisionBarcode.TYPE_PRODUCT:
                        intent = new Intent(getApplicationContext(), ProductDetails.class);
                        intent.putExtra("productCode", rawValue);
                        redirectIntent = true;
                        break;
                    default:
                        break;
                }
            }
            if(redirectIntent){
                startActivity(intent);
            }
        }
    }

    private FirebaseVisionImage getVisionImageFromFrame(Frame frame) {
        byte[] data = frame.getData();
        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setHeight(frame.getSize().getHeight())
                .setWidth(frame.getSize().getWidth())
                .setRotation(FirebaseVisionImageMetadata.ROTATION_90)
                .build();
        return FirebaseVisionImage.fromByteArray(data,metadata);
    }
}
