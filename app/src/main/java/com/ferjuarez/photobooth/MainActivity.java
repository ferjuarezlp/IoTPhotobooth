package com.ferjuarez.photobooth;

import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import java.io.IOException;
import java.nio.ByteBuffer;
import com.google.android.things.contrib.driver.button.Button;

public class MainActivity extends AppCompatActivity {

    private final String BUTTON_GPIO_PIN = "BCM21";
    private final String PIN_LED = "BCM6";
    private Gpio mLedGpio;
    private PeripheralManagerService mPeriphericalService;

    private CameraHandler mCamera;
    private Handler mCameraHandler;
    private HandlerThread mCameraThread;
    private FirebaseDatabase mDatabase;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        // We need permission to access the camera
        if (checkSelfPermission(android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mDatabase = FirebaseDatabase.getInstance();
        mPeriphericalService = new PeripheralManagerService();
        setupButton();
        setupCamera();
        setupIndicatorLed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCamera();
        closeButton();
        closeLedIndicator();
    }

    private void setupCamera(){
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
        mCamera = CameraHandler.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

        /*final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                mCamera.takePicture();
            }
        }, 5000);*/
    }

    private void setupButton(){
        try {
            mButton = new Button(BUTTON_GPIO_PIN, Button.LogicState.PRESSED_WHEN_LOW);
            mButton.setOnButtonEventListener(mButtonCallback);
        } catch (IOException e) {
        }
    }

    private void setupIndicatorLed(){
        try {
            mLedGpio = mPeriphericalService.openGpio(PIN_LED);
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e){

        }
    }

    private void setLedState(boolean isOn) {
        try {
            mLedGpio.setValue(isOn);
        } catch (IOException e) {

        }
    }

    private void closeLedIndicator(){
        if (mLedGpio != null) {
            try {
                mLedGpio.close();
            } catch (IOException e) {
            } finally{
                mLedGpio = null;
            }
        }
    }

    private void closeCamera(){
        mCamera.shutDown();
        mCameraThread.quitSafely();
    }

    private void closeButton(){
        try {
            mButton.close();
        } catch (IOException e) {
        }
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    int width = image.getWidth();
                    int height = image.getHeight();

                    // get image bytes
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    onPictureTaken(imageBytes, null);
                }
            };


    private Button.OnButtonEventListener mButtonCallback = new Button.OnButtonEventListener() {
        @Override

        public void onButtonEvent(Button button, boolean pressed) {
            if (pressed) {
                setLedState(true);
                mCamera.takePicture();
                Log.e("PhotoBooth","Button Pressed!");
            }
        }
    };

    private void onPictureTaken(final byte[] imageBytes, final byte[] thumbnail) {
        if (imageBytes != null) {

            final DatabaseReference log = mDatabase.getReference("logs").push();
            String imageStr = Base64.encodeToString(imageBytes, Base64.NO_WRAP | Base64.URL_SAFE);
            // upload image to firebase
            log.child("timestamp").setValue(ServerValue.TIMESTAMP);
            log.child("image").setValue(imageStr, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    setLedState(false);
                }
            });

            /*mDatabase.getReference("logs").removeValue(new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    final DatabaseReference log = mDatabase.getReference("logs").push();
                    String imageStr = Base64.encodeToString(imageBytes, Base64.NO_WRAP | Base64.URL_SAFE);
                    // upload image to firebase
                    log.child("timestamp").setValue(ServerValue.TIMESTAMP);
                    log.child("image").setValue(imageStr, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            setLedState(false);
                        }
                    });

                }
            });*/

        }
    }

}
