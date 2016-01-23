package com.example.zzr.mediaprojection;
//This is RTSP branch.

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Random;

public class MainActivity extends Activity {
    protected static final int PERMISSION_CODE = 1;
    protected static final String START_MEDIARECORDER = "start_mediarecorder";
    private int mScreenDensity;
    protected static MediaRecorder mMediaRecorder;
    private MediaProjectionManager mProjectionManager;
    private ToggleButton mToggleButton;
    private MediaProjectionCallback mMediaProjectionCallback;
    private static MediaProjection mMediaProjection;
    private static final String TAG = "MediaProjection_main";
    private VirtualDisplay mVirtualDisplay;
    public int buffersize = 500000;
    protected LocalSocket mReceiver, mSender = null;
    private LocalServerSocket mLss = null;
    private int mSocketId = 0;
    protected InetAddress mDestination;
    protected H264Packetizer mPacketizer = null;
    private static VideoStream mVideoStream;
    public static VideoStream getInstance()
    {
        if (mVideoStream == null)
        {
            mVideoStream = new VideoStream();
        }
        return mVideoStream;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        Log.d("RtspServer","metrics.densityDpi"+metrics.densityDpi);
        mMediaRecorder = new MediaRecorder();

        initRecorder();

        mProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mToggleButton = (ToggleButton)findViewById(R.id.toggleButton);

        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    onToggleScreenShare(v);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        mMediaProjectionCallback=new MediaProjectionCallback();
        // Prevents the phone from going to sleep mode
        // PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "com.example.zzr.mediaprojection.wakelock");
        this.startService(new Intent(this,RtspServer.class));
        shareScreen();
        RtspServer.setContext(getApplicationContext());
        SessionBuilder.getInstance().build();
    }
    protected void createSockets() throws IOException {

//        if (sPipeApi == PIPE_API_LS) {
            Log.e(TAG, "in the createSockets");
            final String LOCAL_ADDR = "com.example.zzr.mediaprojection-";

            for (int i=0;i<10;i++) {
                try {
                    mSocketId = new Random().nextInt();
                    mLss = new LocalServerSocket(LOCAL_ADDR+mSocketId);
                    break;
                } catch (IOException ignored) {}
            }

            mReceiver = new LocalSocket();
            mReceiver.connect( new LocalSocketAddress(LOCAL_ADDR+mSocketId));
            mReceiver.setReceiveBufferSize(500000);
            mReceiver.setSoTimeout(3000);
            mSender = mLss.accept();
            mSender.setSendBufferSize(500000);
//
//        } else {
//            Log.e(TAG, "parcelFileDescriptors createPipe version = Lollipop");
//            mParcelFileDescriptors = ParcelFileDescriptor.createPipe();
//            mParcelRead = new ParcelFileDescriptor(mParcelFileDescriptors[0]);
//            mParcelWrite = new ParcelFileDescriptor(mParcelFileDescriptors[1]);
//        }
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(mMediaProjection != null){
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        quitMediaProjection();
    }

    private void quitMediaProjection() {
        this.stopService(new Intent(this,RtspServer.class));
    }

    public void onToggleScreenShare(View view) throws IOException {
        if(((ToggleButton) view).isChecked())
        {
            shareScreen();
        } else {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaProjection.stop();
        Log.v(TAG, "Recording Stopped");
        stopScreenSharing();
//        initRecorder();
//        prepareRecorder();
        }
    }

    public void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
    }

    public  void shareScreen()  {
        if(mMediaProjection == null)
        {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(),PERMISSION_CODE);
            return;
        }
//        mVirtualDisplay = createVirtualDisplay();
//        mMediaRecorder.start();
//        InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(mParcelRead);
//        getH264Stream();//delete the head of mp4 stream
        /* put h264 stream to rtsp server*/
    }

    public void getH264Stream() throws IOException {
        InputStream is = mReceiver.getInputStream();
//        InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(mParcelRead);
        try {
            byte buffer[] = new byte[4];
            // Skip all atoms preceding mdat atom
            while (!Thread.interrupted()) {
                while (is.read() != 'm');
                is.read(buffer,0,3);
                if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
            }
        } catch (IOException e) {
            Log.e(TAG,"Couldn't skip mp4 header :/");
            e.printStackTrace();
            throw e;
        }
        int[] a = getInstance().getDestinationPorts(); //use a to store mRtpPort and mRtcpPort.
  //      mPacketizer.setDestination(mDestination, a[0], a[1]);
 //       mPacketizer.setInputStream(mReceiver.getInputStream());
 //       mPacketizer.setInputStream(is);
//        mPacketizer.setInputStream(new ParcelFileDescriptor.AutoCloseOutputStream(mParcelRead));
//        mPacketizer.start();
    }


    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                MP4Config.DISPLAY_WIDTH, MP4Config.DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }


    public void prepareRecorder() {
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }catch(IllegalStateException e)
        {
            e.printStackTrace();
            finish();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            mToggleButton.setChecked(false);
            return;
        }
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
//        mVirtualDisplay = createVirtualDisplay(); //TODO This code should before start() after prepare.
//        mMediaRecorder.start();
    }

    public void initRecorder() {

//        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mMediaRecorder.setVideoEncodingBitRate(10000000);
//        mMediaRecorder.setVideoSize(MP4Config.DISPLAY_WIDTH, MP4Config.DISPLAY_HEIGHT);
   //     mMediaRecorder.setOutputFile("/sdcard/DCIM/Camera/xiaokaxiu/capture.mp4");
//       mMediaRecorder.setVideoFrameRate(60);
 //       mMediaRecorder.setOutputFile(mSender.getFileDescriptor());
 //       mMediaRecorder.setOutputFile(mParcelWrite.getFileDescriptor());
//        mMediaRecorder.setMaxDuration(0);//called after setOutputFile before prepare,if zero or negation,disables the limit
//        mMediaRecorder.setMaxFileSize(0);//called after setOutputFile before prepare,if zero or negation,disables the limit

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
//            if (mToggleButton.isChecked()) {
//                mToggleButton.setChecked(false);
//                mMediaRecorder.stop();
//                mMediaRecorder.reset();
//                Log.v(TAG, "Recording Stopped");
////                initRecorder();
//                prepareRecorder();
//            }
            mMediaProjection = null;
            stopScreenSharing();
            Log.i(TAG, "MediaProjection Stopped");

        }
    }
//    class Myhandler extends Handler {
//        public Myhandler(Looper looper)
//        {
//            super(looper);
//        }
//        public void handleMessage(Message msg){
//            super.handleMessage(msg);
//            if((String)msg.obj == START_MEDIARECORDER);
//            try {
//                shareScreen();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//    }

    public static MediaProjection getmMediaProjection(){
        return mMediaProjection;
    }

}
