package com.setik.kampertee;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.widget.Toast;
import android.content.Context;

import java.io.IOException;

public class UnityRecorder extends UnityPlayerActivity {
    public static MediaRecorder mRecorder;
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mProjectionManager;
    private VirtualDisplay mVirtualDisplay;
    private DisplayMetrics mDisplayMetrics;
    private String mFilePath, mFileName, mAppDir, mGameObject, mMethodName;
    private int mBitRate, mFps;

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
                showToast("Cannot record");
                UnityPlayer.UnitySendMessage(this.mGameObject, this.mMethodName, "OnError");
                return;
            }
            this.mMediaProjection = this.mProjectionManager.getMediaProjection(resultCode, data);
            this.mVirtualDisplay = createVirtualDisplay();
        }
    }

    //this func is used by Unity side to start recording
    public void startRecording() {
        mRecorder.start();
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
    }

    //this func is used by Unity side to set video bitrate and fps
    public void setupVideo(int bitRate, int fps) {
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

    //this func init thr ProjectionManager to create a virtual Display and start record screen
    private void shareScreen() {
        if (this.mMediaProjection == null) {
            startActivityForResult(this.mProjectionManager.createScreenCaptureIntent(), 200);
            return;
        }
        this.mVirtualDisplay = createVirtualDisplay();
    }

    private VirtualDisplay createVirtualDisplay() {
        int screenWidth=mDisplayMetrics.widthPixels>720?720:mDisplayMetrics.widthPixels;
        int screenHeight=mDisplayMetrics.widthPixels>720?(int)(mDisplayMetrics.heightPixels*720f/mDisplayMetrics.widthPixels):mDisplayMetrics.heightPixels;
        return this.mMediaProjection.createVirtualDisplay("UnityRecorder", screenWidth, screenHeight, this.mDisplayMetrics.densityDpi, 16, this.mRecorder.getSurface(), null, null);
    }

    //this func prepare the mediarecorder to record audio from mic and video from screen
    private void initRecorder() throws IOException {
        this.mRecorder = new MediaRecorder();
        int screenWidth=mDisplayMetrics.widthPixels>720?720:mDisplayMetrics.widthPixels;
        int screenHeight=mDisplayMetrics.widthPixels>720?(int)(mDisplayMetrics.heightPixels*720f/mDisplayMetrics.widthPixels):mDisplayMetrics.heightPixels;
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

    public void prepareRecord() {
        try {
            initRecorder();
            shareScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cleanUpRecord() {
        this.mVirtualDisplay.release();
        if (this.mMediaProjection != null) {
            this.mMediaProjection.stop();
            this.mMediaProjection = null;
        }
        this.mRecorder.release();
        this.mRecorder = null;
    }

    //this func move the recorded video to gallery
    private void addRecordingToMediaLibrary() {
        ContentValues values = new ContentValues(3);
        values.put("title", this.mFileName);
        values.put("mime_type", "video/mp4");
        values.put("_data", this.mFilePath);
        getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        showToast("Video has saved to gallery");
    }

    //helper func to show toast
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}