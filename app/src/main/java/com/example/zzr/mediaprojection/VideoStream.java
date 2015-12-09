package com.example.zzr.mediaprojection;

import android.content.SharedPreferences;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.example.zzr.mediaprojection.exceptions.ConfNotSupportedException;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Administrator on 2015/12/2.
 */
public class VideoStream extends MediaStream
{
    private VirtualDisplay mVirtualDisplay;
    private int mScreenDensity;
    protected MediaRecorder mMediaRecorder;
    private MediaProjectionManager mProjectionManager;
    private MainActivity.MediaProjectionCallback mMediaProjectionCallback;
    private MediaProjection mMediaProjection;
    private MP4Config mConfig;
    protected int mRtpPort = 0, mRtcpPort = 0;
    protected String mMimeType;
    protected String mEncoderName;
    protected int mEncoderColorFormat;
    protected int mMaxFps = 0;
    protected int mVideoEncoder = 0;
    protected SharedPreferences mSettings = null;
    protected VideoQuality mRequestedQuality = VideoQuality.DEFAULT_VIDEO_QUALITY.clone();
    protected boolean mUpdated = false;
    protected int mRequestedOrientation = 0, mOrientation = 0;
    protected VideoQuality mQuality = mRequestedQuality.clone();
    private boolean flag = false;
    public synchronized String getSessionDescription() throws IllegalStateException{
        if (mConfig == null) throw new IllegalStateException("You need to call configure() first !");
        return "m=video "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
                "a=rtpmap:96 H264/90000\r\n" +
                "a=fmtp:96 packetization-mode=1;profile-level-id="+mConfig.getProfileLevel()+";sprop-parameter-sets="+mConfig.getB64SPS()+","+mConfig.getB64PPS()+";\r\n";

    }
    public  int[] getDestinationPorts() {
        return new int[] {
                mRtpPort,
                mRtcpPort
        };
    }

    
//
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode != PERMISSION_CODE) {
//            Log.e(TAG, "Unknown request code: " + requestCode);
//            return;
//        }
//        if (resultCode != RESULT_OK) {
// //           Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
//            Log.d(TAG,"Screen Cast Permission Denied");
//            flag=false;
//            return;
//        }
//        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
//        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
//        mVirtualDisplay = createVirtualDisplay();
//        mMediaRecorder.start();
//    }
//    private VirtualDisplay createVirtualDisplay() {
//        return mMediaProjection.createVirtualDisplay("MainActivity",
//                MP4Config.DISPLAY_WIDTH, MP4Config.DISPLAY_HEIGHT, mScreenDensity,
//                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
//    }
    @Override
    protected void encodeWithMediaRecorder() throws IOException {

            Log.d(TAG, "Video encoded using the MediaRecorder API");
            // We need a local socket to forward data output by the camera to the packetizer
            createSockets();
//        if(mMediaProjection == null)
//        {
//            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
//            return;
//        }
//        mVirtualDisplay = createVirtualDisplay();
//        mMediaRecorder.start();
            try {

                mMediaRecorder = new MediaRecorder();
                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mMediaRecorder.setVideoEncoder(mVideoEncoder);
                mMediaRecorder.setVideoSize(mConfig.DISPLAY_WIDTH, mConfig.DISPLAY_HEIGHT);
                mMediaRecorder.setVideoFrameRate(60);

                // The bandwidth actually consumed is often above what was requested
                mMediaRecorder.setVideoEncodingBitRate(10000000);

                // We write the output of the camera in a local socket instead of a file !
                // This one little trick makes streaming feasible quiet simply: data from the camera
                // can then be manipulated at the other end of the socket
                FileDescriptor fd;
                if (sPipeApi == PIPE_API_PFD) {
                    fd = mParcelWrite.getFileDescriptor();
                } else  {
                    fd = mSender.getFileDescriptor();
                }
                mMediaRecorder.setOutputFile(fd);
                mMediaRecorder.prepare();
                mMediaRecorder.start();

            } catch (Exception e) {
                throw new ConfNotSupportedException(e.getMessage());
            }

            InputStream is;

            if (sPipeApi == PIPE_API_PFD) {
                is = new ParcelFileDescriptor.AutoCloseInputStream(mParcelRead);
            } else  {
                is = mReceiver.getInputStream();
            }

            // This will skip the MPEG4 header if this step fails we can't stream anything :(
            try {
                byte buffer[] = new byte[4];
                // Skip all atoms preceding mdat atom
                while (!Thread.interrupted()) {
                    while (is.read() != 'm');
                    is.read(buffer,0,3);
                    if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
                }
            } catch (IOException e) {
                Log.e(TAG, "Couldn't skip mp4 header :/");
                stop();
                throw e;
            }

            // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
            mPacketizer.setInputStream(is);
            mPacketizer.start();
            mStreaming = true;

    }
    @Override
    protected void encodeWithMediaCodec() throws IOException {
    Log.v(TAG,"using ecodeWithMediaCodec !!! WTF");
    }

    public void setPreferences(SharedPreferences prefs) {
        mSettings = prefs;
    }
    public void setVideoQuality(VideoQuality videoQuality) {
        if (!mRequestedQuality.equals(videoQuality)) {
            mRequestedQuality = videoQuality.clone();
            mUpdated = false;
        }
    }
    public void setPreviewOrientation(int orientation) {
        mRequestedOrientation = orientation;
        mUpdated = false;
    }

//    class MyThread extends Thread{
//        public void run(){
//            Looper.prepare();
//            Looper looper=Looper.getMainLooper();
//            handler=new MainActivity.Myhandler(looper);
//        }
//    }
}
