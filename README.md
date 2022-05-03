# Android_Utils_Plugin

A simple android library project for Unity which contains some useful features:  

+ Capture android screen as a video.  
+ Request android permissions at runtime.  

Thank for the open source project <https://github.com/HBiSoft/HBRecorde> which I use as a record service in this repository.

Visit this site to get a details explaining of Unity_Android_Screen_Recorder.

<https://killertee.wordpress.com/2018/01/08/unity-android-record-game-screen-with-audio-using-your-ad-hoc-plug-in/>

**<p align="center">If you are using this library in one of your applications and would like to thank me:</p>**

<p align="center"><a href="https://www.buymeacoffee.com/KamperTee" target="_blank" ><img src="https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png" alt="Buy Me A Coffee" style="height: 41px !important;width: 164px !important;box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;-webkit-box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;" ></a></p>

<p>or via PayPal:</p>

<https://www.paypal.com/paypalme/kampertee>

## Build guide

+ Clone this project and open it using Android Studio.  
+ Make module androidutils.
+ Wait for build process to complete then get your plugin in Android_Screen_Recorder/unityrecorder/build/generated/outputs/aar/unityrecorder-release.aar  
+ Copy the unityrecorder-release.aar to Plugins/Android folder inside your project.  
+ Add a AndroidManifest.xml in the same folder which have the content below:  

```xml

<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="your package name">
  <application android:icon="@drawable/app_icon" android:launchMode="singleTask" android:label="@string/app_name">
    <activity android:name="com.setik.androidutils.AndroidUtils" android:label="@string/app_name" android:configChanges="fontScale|keyboard|keyboardHidden|locale|mnc|mcc|navigation|orientation|screenLayout|screenSize|smallestScreenSize|uiMode|touchscreen">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
  </application>
</manifest>

```

Now your plugin is ready to use in any Unity project.  

## Screen Record guide

To record android screen you have to follow this instruction:  

### 1.Set-up recorder

Call this inside Start() function of your script.

```cs

#if UNITY_ANDROID && !UNITY_EDITOR
        using (AndroidJavaClass unityClass = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
        {
            androidRecorder = unityClass.GetStatic<AndroidJavaObject>("currentActivity");
            androidRecorder.Call("setUpSaveFolder","Tee");//custom your save folder to Movies/Tee, by defaut it will use Movies/AndroidUtils
            int width = (int)(Screen.width > SCREEN_WIDTH ? SCREEN_WIDTH : Screen.width);
            int height = Screen.width > SCREEN_WIDTH ? (int)(Screen.height * SCREEN_WIDTH / Screen.width) : Screen.height;
            int bitrate = (int)(1f * width * height / 100 * 240 * 7);
            int fps = 30;
            bool audioEnable=true;
            androidRecorder.Call("setupVideo", width, height,bitrate, fps, audioEnable, VideoEncoder.H264.ToString());//this line manual sets the video record setting. You can use the defaut setting by comment this code block
        }
#endif

```

### 2. Start record

Then call this to start record screen.

```cs

#if UNITY_ANDROID && !UNITY_EDITOR
        if (!AndroidUtils.IsPermitted(AndroidPermission.RECORD_AUDIO))//RECORD_AUDIO is declared inside plugin manifest but we need to request it manualy
        {
            AndroidUtils.RequestPermission(AndroidPermission.RECORD_AUDIO);
            onAllowCallback = () =>
            {
                androidRecorder.Call("startRecording");
            };
            onDenyCallback = () => { ShowToast("Need RECORD_AUDIO permission to record voice");};
            onDenyAndNeverAskAgainCallback = () => { ShowToast("Need RECORD_AUDIO permission to record voice");};
        }
        else
            androidRecorder.Call("startRecording");
#endif
```

### 3. Stop record

Call

```cs

androidRecorder.Call("stopRecording");
```

### 4. Handle callback

Create a function name VideoRecorderCallback(string message) inside your project. It will receive callback from java side.  
List of messages from java:  

+ init_record_error  
+ start_record  
+ stop_record  

## Runtime Permissions guide

Runtime permission allow you to request android permission at runtime(for android 6.0 and above). Example: When your game need to access location service, instead of request this permission at the first runtime now you can delay it until the moment your app actually need to use the permission.  
Note that. Your app can only request the permissions which have been declared in AndroidManifest.xml  

### 1. You need to declare the permissions in AndroidManifest.xml. Then add this below meta-data to skip request permission dialog when you open app first time

```xml

<meta-data android:name="unityplayer.SkipPermissionsDialog" android:value="true" />

```

the final AndroidManifest.xml must look similar like this.  

```xml

<?xml version="1.0" encoding="utf-8"?>
<manifest 
  xmlns:android="http://schemas.android.com/apk/res/android" package="com.setik.androidutils">
  <application android:icon="@drawable/app_icon" android:label="@string/app_name">
    <activity android:name="com.setik.androidutils.AndroidUtils" android:label="@string/app_name" android:configChanges="fontScale|keyboard|keyboardHidden|locale|mnc|mcc|navigation|orientation|screenLayout|screenSize|smallestScreenSize|uiMode|touchscreen">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
    <meta-data android:name="unityplayer.SkipPermissionsDialog" android:value="true" />
  </application>
</manifest>

```

### 2.To check if your app has a permision

```cs

public static bool IsPermitted(AndroidPermission permission)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        using (var androidUtils = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
        {
            return androidUtils.GetStatic<AndroidJavaObject>("currentActivity").Call<bool>("hasPermission", GetPermissionStrr(permission));
        }
#endif
        return true;
    }

```

### 3.To request a permission

```cs

public static void RequestPermission(AndroidPermission permission, UnityAction onAllow = null, UnityAction onDeny = null, UnityAction onDenyAndNeverAskAgain = null)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        onAllowCallback = onAllow;
        onDenyCallback = onDeny;
        onDenyAndNeverAskAgainCallback = onDenyAndNeverAskAgain;
        using (var androidUtils = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
        {
            androidUtils.GetStatic<AndroidJavaObject>("currentActivity").Call("requestPermission", GetPermissionStrr(permission));
        }
#endif
    }
```

### 4. Helper function to convert from enum to android permission string

```cs

	private static string GetPermissionStrr(AndroidPermission permission)
    {
        return "android.permission." + permission.ToString();
    }
 ```

### 5. List of android permissions

```cs

public enum AndroidPermission
{
    ACCESS_COARSE_LOCATION,
    ACCESS_FINE_LOCATION,
    ADD_VOICEMAIL,
    BODY_SENSORS,
    CALL_PHONE,
    CAMERA,
    GET_ACCOUNTS,
    PROCESS_OUTGOING_CALLS,
    READ_CALENDAR,
    READ_CALL_LOG,
    READ_CONTACTS,
    READ_EXTERNAL_STORAGE,
    READ_PHONE_STATE,
    READ_SMS,
    RECEIVE_MMS,
    RECEIVE_SMS,
    RECEIVE_WAP_PUSH,
    RECORD_AUDIO,
    SEND_SMS,
    USE_SIP,
    WRITE_CALENDAR,
    WRITE_CALL_LOG,
    WRITE_CONTACTS,
    WRITE_EXTERNAL_STORAGE
}

```

### 6. Implement 3 functions below to receive callback when you request a permission

```cs

private void OnAllow(){}
private void OnDeny(){}
private void OnDenyAndNeverAskAgain(){}

```
