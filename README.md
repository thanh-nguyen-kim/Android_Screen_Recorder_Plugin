# Android_Utils_Plugin  
A simple android library project for Unity which contains some useful features:  
+ Capture android screen as a video.  
+ Request android permissions at runtime.  
+ Refresh and open gallery after take a screen shot or screen capture.  

# Build guide:
+ Clone this project and open it using Android Studio.  
+ In Gradle project: Navigate to Android_Screen_Recorder/unityrecorder/Tasks/build and double-click to "assembleRelease".  
+ Wait for build process to complete then get your plugin in Android_Screen_Recorder/unityrecorder/build/generated/outputs /aar/unityrecorder-release.aar  
+ Use 7-zip to open this aar plugin and delete class.jar inside "libs" folder. You can read this blog to understand the reason which force us to delete the class.jar file.  
+ Copy the modified unityrecorder-release.aar to Plugins/Android folder inside your project.  
+ Add a AndroidManifest.xml in the same folder which have the content below:  

```xml

<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="your package name">
  <application android:icon="@drawable/app_icon" android:launchMode="singleTask" android:label="@string/app_name">
    <activity android:name="com.setik.kampertee.AndroidUtils" android:label="@string/app_name" android:configChanges="fontScale|keyboard|keyboardHidden|locale|mnc|mcc|navigation|orientation|screenLayout|screenSize|smallestScreenSize|uiMode|touchscreen">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
  </application>
</manifest>

```
Now your plugin is ready to use in any Unity project.  

# Screen Record guide.  

To record android screen you have to follow this instruction:  

### 1.Set-up recorder.  
Call this inside Start() function of your script.  
```cs

using (AndroidJavaClass unityClass = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
{
    androidRecorder = unityClass.GetStatic<AndroidJavaObject>("currentActivity");
    int width = (int)(Screen.width > SCREEN_WIDTH ? SCREEN_WIDTH : Screen.width);
    int height = Screen.width > SCREEN_WIDTH ? (int)(Screen.height * SCREEN_WIDTH / Screen.width) : Screen.height;
    androidRecorder.Call("setupVideo", width, height,(int)(1f * width * height / 100 * 240 * 7), 30);
	androidRecorder.Call("setCallback","AndroidUtilsController","VideoRecorderCallback");//this line set up the callback from java to Unity for more information please google it.
}
	
```

### 2. Start record.  
Call 2 lines of code to prepare for record.  
```cs

androidRecorder.Call("setFileName", VIDEO_NAME);//androidRecorder is a private variable which we have get reference inside Start() function.
androidRecorder.Call("prepareRecorder");

```

Then call this to start record screen.  
```cs
androidRecorder.Call("startRecording");	
```

### 3. Stop record.  
Call  
```
androidRecorder.Call("stopRecording");
```

### 4. Handle callback  
Create a function name VideoRecorderCallback(string message) inside your project. It will receive callback from java side.  
List of messages from java:  

+ init_record_error  
+ init_record_success  
+ start_record  
+ stop_record  

# Gallery refresh guide.  

This function refresh your gallery so pictures or videos capture inside your game will show up next time you open gallery, or file explore app. Call this on your unity script.  
```cs

using(AndroidJavaClass javaClass = new AndroidJavaClass("com.unity3d.player.UnityPlayer")){
	javaClass.GetStatic<AndroidJavaObject>("currentActivity").Call("refreshGallery", path);
}

```
This function will open your gallery app.  
```cs

using(AndroidJavaClass javaClass = new AndroidJavaClass("com.unity3d.player.UnityPlayer")){
	javaClass.GetStatic<AndroidJavaObject>("currentActivity").Call("openGallery");
}

```
# Runtime Permissions guide  

Runtime permission allow you to request android permission at runtime(for android 6.0 and above). Example: When your game need to access location service, instead of request this permission at the first runtime now you can delay it until the moment your app actually need to use the permission.  
* Note that. Your app can only request the permissions which have been declared in AndroidManifest.xml  

###  1. You need to declare the permissions in AndroidManifest.xml. Then add this below meta-data to skip request permission dialog when you open app first time.  
```xml

<meta-data android:name="unityplayer.SkipPermissionsDialog" android:value="true" />

```
the final AndroidManifest.xml must look similar like this.  

```xml

<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="vn.adt.amazingvideo">
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
  <application android:icon="@drawable/app_icon" android:launchMode="singleTask" android:label="@string/app_name">
    <activity android:name="com.setik.kampertee.AndroidUtils" android:label="@string/app_name" android:configChanges="fontScale|keyboard|keyboardHidden|locale|mnc|mcc|navigation|orientation|screenLayout|screenSize|smallestScreenSize|uiMode|touchscreen">
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


### 3.To request a permission.  
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

### 4. Helper function to convert from enum to android permission string.  
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

### 6. Implement 3 functions below to receive callback when you request a permission.
```cs

private void OnAllow(){}
private void OnDeny(){}
private void OnDenyAndNeverAskAgain(){}

```
