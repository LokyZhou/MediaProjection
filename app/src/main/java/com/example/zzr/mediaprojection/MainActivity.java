package com.example.zzr.mediaprojection;
//This is RTSP branch.

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.zzr.mediaprojection.ftp.FsService;
import com.example.zzr.mediaprojection.ftp.Setting;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends Activity {
    protected static final int PERMISSION_CODE = 1;
    protected static final String START_MEDIARECORDER = "start_mediarecorder";
    protected static int TEST = 0X000000;
    public static final int socket_msg = 0x123;
    protected static MediaRecorder mMediaRecorder;
    private MediaProjectionManager mProjectionManager;
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
    protected static ArrayList<Socket> socketList = new ArrayList<Socket>();
    public static Context mcontext = null;
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
        this.mcontext = this.getApplication().getApplicationContext();
        mMediaRecorder = new MediaRecorder();
        mProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        new Thread(){
            public void run(){
                try {
                    StartListenerSocket();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        mMediaProjectionCallback=new MediaProjectionCallback();
        startFtpServer();
        this.startService(new Intent(this, RtspServer.class));
        shareScreen();
        RtspServer.setContext(getApplicationContext());
        SessionBuilder.getInstance().build();

    }

    private void startFtpServer() {
        sendBroadcast(new Intent(FsService.ACTION_START_FTPSERVER));
    }

    /*
     * set the control port is 8088
     * start the TCP Server
     */
    public void StartListenerSocket() throws IOException{
        ServerSocket ss = new ServerSocket(8088);
//        Toast.makeText(this, "TCP Server is ready", Toast.LENGTH_SHORT).show();
        while(true){
            Socket socket = ss.accept();
            Toast.makeText(getApplicationContext(),"client is connected",Toast.LENGTH_SHORT).show();
            socketList.add(socket);
            new Thread(new ServerThread(socket)).start();
        }
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
            return;
        }
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
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
            Intent intent = new Intent(this,Setting.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mMediaProjection = null;
            stopScreenSharing();
            Log.i(TAG, "MediaProjection Stopped");

        }
    }
    /**
     * To process message
     */
    public Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {

            if (msg.what == socket_msg) {
                if (msg.obj.equals(TEST))
                Toast.makeText(getApplicationContext(),"server is OK!",Toast.LENGTH_SHORT).show();
            }
        }
    };


    public static MediaProjection getmMediaProjection(){
        return mMediaProjection;
    }

}
