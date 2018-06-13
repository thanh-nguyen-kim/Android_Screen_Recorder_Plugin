package com.setik.kampertee;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.widget.Toast;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;
import android.app.Activity;

import java.io.IOException;

public class AndroidUtils extends UnityPlayerActivity {
    public static MediaRecorder mRecorder;
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mProjectionManager;
    private VirtualDisplay mVirtualDisplay;
    private DisplayMetrics mDisplayMetrics;
    private String mFilePath, mFileName, mAppDir, mGameObject, mMethodName;
    private int mBitRate, mFps,screenWidth,screenHeight;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(this.mDisplayMetrics);
        this.mProjectionManager = ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE));
        this.mAppDir = getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200) {
            if (resultCode != -1) {
                Toast.makeText(this, "Can't init recorder", Toast.LENGTH_SHORT).show();
                UnityPlayer.UnitySendMessage(this.mGameObject, this.mMethodName, "init_record_error");
                return;
            }else {
                UnityPlayer.UnitySendMessage(this.mGameObject, this.mMethodName, "init_record_success");
                this.mMediaProjection = this.mProjectionManager.getMediaProjection(resultCode, data);
                this.mVirtualDisplay = createVirtualDisplay();
            }
        }
    }
    //this func is used by Unity side to start recording
    public void startRecording() {
        mRecorder.start();
        UnityPlayer.UnitySendMessage(mGameObject,mMethodName, "start_record");
    }
    //this func is used by Unity side to stop recording
    public void stopRecording() {
        this.mVirtualDisplay.release();
        if (this.mMediaProjection != null) {
            this.mMediaProjection.stop();
            this.mMediaProjection = null;
        }
        this.mRecorder.stop();
        this.mRecorder.release();
        this.mRecorder = null;
        UnityPlayer.UnitySendMessage(this.mGameObject, this.mMethodName, "stop_record");
    }
    //this func is used by Unity side to set video bitrate and fps. bitrates=width*height/168000
    public void setupVideo(int width,int height,int bitRate, int fps) {
        this.screenWidth=width;
        this.screenHeight=height;
        this.mBitRate = bitRate;
        this.mFps = fps;
    }
    //this func is used by Unity side to set video name
    public void setFileName(String fileName) {
        this.mFileName = fileName;
        this.mFilePath = (this.mAppDir + "/" + fileName + ".mp4");
    }
    //this func is used by Unity side to set callback when record status changed
    public void setCallback(String gameObject, String methodName) {
        this.mGameObject = gameObject;
        this.mMethodName = methodName;
    }
    public void prepareRecorder() {
        try {
            initRecorder();
            shareScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //use this function when you don't use record anymore
    public void cleanUpRecorder() {
        this.mVirtualDisplay.release();
        if (this.mMediaProjection != null) {
            this.mMediaProjection.stop();
            this.mMediaProjection = null;
        }
        this.mRecorder.release();
        this.mRecorder = null;
    }
    //this func init thr ProjectionManager to create a virtual Display and start record screen
    private void shareScreen() {
        if (this.mMediaProjection == null) {
            startActivityForResult(this.mProjectionManager.createScreenCaptureIntent(), 200);
            return;
        }
        this.mVirtualDisplay = createVirtualDisplay();
    }
    private VirtualDisplay createVirtualDisplay() {
        return this.mMediaProjection.createVirtualDisplay("AndroidUtils", screenWidth, screenHeight, this.mDisplayMetrics.densityDpi, 16, this.mRecorder.getSurface(), null, null);
    }
    //this func prepare the mediarecorder to record audio from mic and video from screen
    private void initRecorder() throws IOException {
        this.mRecorder = new MediaRecorder();
        this.mRecorder.setAudioSource(1);
        this.mRecorder.setVideoSource(2);
        this.mRecorder.setOutputFormat(2);
        this.mRecorder.setOutputFile(this.mFilePath);
        this.mRecorder.setVideoSize(screenWidth, screenHeight);
        this.mRecorder.setVideoEncoder(2);
        this.mRecorder.setAudioEncoder(2);
        this.mRecorder.setVideoEncodingBitRate(this.mBitRate);
        this.mRecorder.setVideoFrameRate(this.mFps);
        this.mRecorder.prepare();
    }
    //this func move the recorded video to gallery
    private void addRecordingToMediaLibrary() {
        ContentValues values = new ContentValues(3);
        values.put("title", this.mFileName);
        values.put("mime_type", "video/mp4");
        values.put("_data", this.mFilePath);
        getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        Toast.makeText(this, "Video has saved to gallery", Toast.LENGTH_SHORT).show();
    }
    public void refreshGallery(String filePath){
        MediaScannerConnection.scanFile(UnityPlayer.currentActivity,
                new String[] { filePath }, null,
                new MediaScannerConnection.OnScanCompletedListener()
                {
                    public void onScanCompleted(String path, Uri uri)
                    {
                        Log.i("TAG", "Finished scanning " + path);
                    }
                });
    }
    public void openGallery(){
        Intent intent=new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setType("image/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        UnityPlayer.currentActivity.startActivity(intent);
    }
    @TargetApi(23)
    public void requestPermission(String permissionStr)
    {
        if ((!hasPermission(permissionStr)) && (android.os.Build.VERSION.SDK_INT >= 23)) {
            UnityPlayer.currentActivity.requestPermissions(new String[] { permissionStr }, 0);
        }
    }
    @TargetApi(23)
    public boolean hasPermission(String permissionStr)
    {
        if (android.os.Build.VERSION.SDK_INT < 23)
            return true;
        Context context = UnityPlayer.currentActivity.getApplicationContext();
        return context.checkCallingOrSelfPermission(permissionStr) == PackageManager.PERMISSION_GRANTED;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case 0:
                if (grantResults[0] == 0) {
                    UnityPlayer.UnitySendMessage(mGameObject,"OnAllow","");
                } else if (android.os.Build.VERSION.SDK_INT >= 23) {
                    if (shouldShowRequestPermissionRationale(permissions[0])) {
                        UnityPlayer.UnitySendMessage(mGameObject,"OnDeny","");
                    } else {
                        UnityPlayer.UnitySendMessage(mGameObject,"OnDenyAndNeverAskAgain","");
                    }
                }
                break;
        }
    }
}