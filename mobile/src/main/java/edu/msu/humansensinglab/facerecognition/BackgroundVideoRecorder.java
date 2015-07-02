package edu.msu.humansensinglab.facerecognition;

/**
 * Created by rabbimd on 3/31/2015.
 */
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import java.util.List;

//import android.app.Notification;
//import android.os.Bundle;
//import android.support.v4.app.NotificationCompat;
//import android.support.v4.app.NotificationManagerCompat;



public class BackgroundVideoRecorder extends Service implements SurfaceHolder.Callback {
    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private android.hardware.Camera camera = null;
    boolean faceDetectedFlag = false;

    //BroadcastReciever to stop and start recording according to screen state
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //Debug lines
            Toast.makeText(context,"gotit",Toast.LENGTH_LONG).show();

            //Stop recording when the screen turns off
            if(action.equals(Intent.ACTION_SCREEN_OFF)){
                Log.d("receiver", "off");
                Toast.makeText(context,"offworks",Toast.LENGTH_LONG).show();
            }

            //Resume recording when the screen turns on
            else{
                Log.d("receiver", "on");
                Toast.makeText(context,"onworks",Toast.LENGTH_LONG).show();
            }
        }
    };


    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        Log.d("service","onCreate");
        //Create and Register Filters
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(receiver,filter);

        /*Notification notification = new Notification.Builder(this)
                .setContentTitle("Background Video Recorder")
                .setContentText("")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        startForeground(4999, notification);*/

        //Set up Window and Surface
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(this);

        //Use 1x1 pixel as the preview
        LayoutParams layoutParams = new WindowManager.LayoutParams(
                400, 400,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
        surfaceView.getHolder().addCallback(this);

    }

    // Method called right after Surface created (initializing and starting MediaRecorder)
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //vibration variables
        int dot = 200;      // Length of a Morse Code "dot" in milliseconds
        int dash = 300;     // Length of a Morse Code "dash" in milliseconds
        int short_gap = 100;    // Length of Gap Between dots/dashes
        int medium_gap = 300;   // Length of Gap Between Letters
        int long_gap = 500;    // Length of Gap Between Words
        final long[] pattern = {
                0,  // Start immediately
                dot, short_gap, dot, short_gap, dot,    // s
//                medium_gap,
//                dash, short_gap, dash, short_gap, dash, // o
//                medium_gap,
//                dot, short_gap, dot, short_gap, dot,    // s
//                long_gap
        };
        final Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);




        camera = android.hardware.Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        camera.setDisplayOrientation(90);

        camera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
            @Override
            public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                if(faces != null && faces.length > 0 && faceDetectedFlag == false) {
                    faceDetectedFlag = true;

                    //TODO: Draw the face
                    Log.d("Face", "Face found");
                    //v.vibrate(pattern, -1);

                    //-----------------------------------------------
                    int notificationId = 001;

                    NotificationCompat.Builder notificationBuilder =
                            new NotificationCompat.Builder(BackgroundVideoRecorder.this)
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setVibrate(new long[]{0, 300, 200, 300, 200, 300})
                                    .setContentTitle("")
                                    .setContentText("Please don't interact with the smartphone while Driving.");

                    NotificationManagerCompat notificationManager =
                            NotificationManagerCompat.from(BackgroundVideoRecorder.this);

                    notificationManager.notify(notificationId, notificationBuilder.build());
//-----------------------------------------------

                }
                if (faces == null || faces.length == 0)
                    faceDetectedFlag = false;
            }
        });

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            camera.startFaceDetection();
        } catch (Exception ignored) {}
    }

    // Stop recording and remove SurfaceView
    @Override
    public void onDestroy() {
        camera.release();
        unregisterReceiver(receiver);
        windowManager.removeView(surfaceView);
        Toast.makeText(this,"got destroyed!",Toast.LENGTH_LONG).show();

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
// We have no surface, return immediately:
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        // Try to stop the current preview:
        try {
            camera.stopPreview();
            camera.stopFaceDetection();
        } catch (Exception e) {
            // Ignore...
        }
        // Get the supported preview sizes:
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size previewSize = previewSizes.get(0);
        // And set them:
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        camera.setParameters(parameters);
        camera.setDisplayOrientation(90);

        // Finally start the camera preview again:
        camera.startPreview();
        camera.startFaceDetection();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        camera.setPreviewCallback(null);
        camera.setFaceDetectionListener(null);
        camera.setErrorCallback(null);
        camera.release();
        camera = null;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

}