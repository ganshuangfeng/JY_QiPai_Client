package com.cymj.huawei;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Toast;

import com.huawei.my.SDKController;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.unity3d.player.UnityPlayer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import androidx.core.content.ContextCompat;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class UnityPlayerActivity extends Activity
{
    protected UnityPlayer mUnityPlayer; // don't change the name of this variable; referenced from native code

    protected final int REQUEST_ALL_PERMISSION = 1000;
    private final String AudioFlag = ".amr";

	private Vibrator m_vibrator;
	private RxPermissions m_rxPermissions;

	private LocationListener m_locationListener;
	private String m_locationProvider;

    private AssetManager m_assetManager;
    private byte[] m_assetBuffer;

    private String m_deviceID;
    public String DeviceID() {
        m_deviceID = "";
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tm = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                m_deviceID = tm.getImei();
            else
                m_deviceID = tm.getDeviceId();
        }
        if(m_deviceID == null)
            m_deviceID = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        //else
        //    Settings.Secure.putString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID, m_deviceID);
        return m_deviceID;
    }

	private String m_deeplink;
	private void UpdateDeeplink() {
        Uri data = getIntent().getData();
        if(data == null)
            return;

        try {
            String scheme = data.getScheme(); // "will"
            String host = data.getHost(); // "share"
            m_deeplink = new String(data.toString());
            getIntent().setData(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	public String Deeplink()
	{
        m_deeplink = "";
        UpdateDeeplink();
	    return m_deeplink;
	}
	public String PushDeviceToken() {
	    return "";
    }
	public void RunVibrator(long tt)
	{
		m_vibrator.vibrate(tt);
	}
	public void CallUp(String val)
	{
		Intent intent = new Intent(Intent.ACTION_DIAL);
		Uri data = Uri.parse("tel:" + val);
		intent.setData(data);
		startActivity(intent);		
	}
	public void QueryingCityName(float[] param) {
        List<Address> addList = null;
        Geocoder ge = new Geocoder(this);
        try {
            addList = ge.getFromLocation(param[0], param[1], 1);
        } catch(IOException e) {
            e.printStackTrace();
        }

        if(addList != null && addList.size() > 0) {
            Address first = addList.get(0);
            String locality = first.getLocality();
            String sublocality = first.getSubLocality();
            String thoroughfare = first.getThoroughfare();

            String detail = locality;
            if(sublocality != null)
                detail += sublocality;
            if(thoroughfare != null)
                detail += thoroughfare;

            UnityPlayer.UnitySendMessage("SDK_callback", "OnUpdCityName", detail);
        }
    }

    private Criteria getCriteria() {
        Criteria criteria = new Criteria();
        // ????????????????????? Criteria.ACCURACY_COARSE???????????????Criteria.ACCURACY_FINE???????????????
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // ????????????????????????
        criteria.setSpeedRequired(false);
        // ?????????????????????????????????
        criteria.setCostAllowed(false);
        // ??????????????????????????????
        criteria.setBearingRequired(false);
        // ??????????????????????????????
        criteria.setAltitudeRequired(false);
        // ????????????????????????
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }
    public void QueryingGPS() {

        LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        m_locationProvider = locationManager.getBestProvider(getCriteria(), true);
        m_locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                UpdateGPS(location);

                locationManager.removeUpdates(this);
                //locationManager.setTestProviderEnabled(m_locationProvider, false);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.i("onStatusChanged", "come in");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.i("onProviderEnabled", "come in");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.i("onProviderDisabled", "come in");
            }
        };
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //locationManager.setTestProviderEnabled(m_locationProvider, true);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, (float)20.0, m_locationListener);
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, (float)20.0, m_locationListener);
        }
    }

    private class ReverseGeocodingTask extends AsyncTask<Location, Void, Void> {
        Context mContext;

        public ReverseGeocodingTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected Void doInBackground(Location... params) {
            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());

            Location loc = params[0];
            List<Address> addresses = null;
            try {
                // Call the synchronous getFromLocation() method by passing in the lat/long values.
                addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            } catch (IOException e) {
                e.printStackTrace();
                // Update UI field with the exception.
            }
            if (addresses != null && addresses.size() > 0) {
                Address first = addresses.get(0);
                // Format the first line of address (if available), city, and country name.

                String locality = first.getLocality();
                if(locality == null)
                    locality = first.getAdminArea();

                String sublocality = first.getSubLocality();
                String thoroughfare = first.getThoroughfare();

                String detail = locality;
                if(sublocality != null)
                    detail += sublocality;
                if(thoroughfare != null)
                    detail += thoroughfare;

                float latitude = (float)loc.getLatitude();
                float longitude = (float)loc.getLongitude();
                String result = new String(latitude + "#" + longitude + "#" + detail);
                UnityPlayer.UnitySendMessage("SDK_callback", "OnGPS", result);
            }
            return null;
        }
    }

    private class ReverseMapTask extends AsyncTask<Location, Void, String> {
        Context mContext;

        public ReverseMapTask(Context context) {
            super();
            mContext = context;
        }
        @Override
        protected String doInBackground(Location... params) {
            HttpURLConnection connection = null;
            try {
                String urlMap = "http://api.map.baidu.com/geocoder?output=json&location=39.913542,116.379763&ak=esNPFDwwsXWtsQfw4NMNmur1";
                URL url = new URL(urlMap);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);

                InputStream in = connection.getInputStream();
                // ??????????????????????????????????????????
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                // ????????????????????????????????????Message???
                return response.toString();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String m_list) {
            super.onPostExecute(m_list);
            Log.e("str", m_list.toString());
            String city = "";
//                if (m_list != null && m_list.size() > 0) {
//                    city = m_list.get(0).getLocality();//????????????
//                }
            city = m_list;
        }
    }

    private void UpdateGPS(Location location) {
	    float latitude = (float)location.getLatitude();
	    float longitude = (float)location.getLongitude();

        String result = new String(latitude + "#" + longitude + "#" + "");
        UnityPlayer.UnitySendMessage("SDK_callback", "OnGPS", result);

        //(new ReverseMapTask(this)).execute(new Location[]{location});


	    /*if(Geocoder.isPresent()) {
            (new ReverseGeocodingTask(this)).execute(new Location[] {location});
        }*/

        /*List<Address> addList = null;
        Geocoder ge = new Geocoder(this);
        try {
            addList = ge.getFromLocation(latitude, longitude, 1);
        } catch(IOException e) {
            e.printStackTrace();
        }

        if(addList != null && addList.size() > 0) {
            Address first = addList.get(0);
            String locality = first.getLocality();
            if(locality == null)
                locality = first.getAdminArea();

            String sublocality = first.getSubLocality();
            String thoroughfare = first.getThoroughfare();

            String detail = locality;
            if(sublocality != null)
                detail += sublocality;
            if(thoroughfare != null)
                detail += thoroughfare;

            String result = new String(latitude + "#" + longitude + "#" + detail);
            UnityPlayer.UnitySendMessage("SDK_callback", "OnGPS", result);
        }*/
    }
    /*private String m_recordFile;
	private MediaRecorder m_recorder;
	private boolean m_isRecording;
    public int StartRecording(String fileName)
    {
        m_recordFile = new String(fileName + ".amr");
        if(m_isRecording) {
            m_recorder.release();
            m_recorder = null;
        }

        try {
            m_recorder = new MediaRecorder();
            m_recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            m_recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            m_recorder.setOutputFile(fileName);
            m_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            try {
                m_recorder.prepare();
                m_recorder.start();
                m_isRecording = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return 1;
    }
    public void StopRecording(boolean callback) {
        try {
            m_recorder.stop();
            m_recorder.release();
            m_recorder = null;
            m_isRecording = false;

            if(callback)
                UnityPlayer.UnitySendMessage("SDK_callback", "OnRecord", m_recordFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
    private String m_recordFile;
    public int StartRecording(String fileName) {
        m_recordFile = fileName;

        String fullName = fileName;
        if(!fileName.endsWith(AudioFlag))
            fullName += AudioFlag;

        if(Mic.startAudioRecord(fullName))
            return 1;
        else
            return -1;
    }
    public void StopRecording(boolean callback) {
        if(Mic.stopAudioRecord()) {
            if(callback)
                UnityPlayer.UnitySendMessage("SDK_callback", "OnRecord", m_recordFile);
        }else {
            if(callback)
                UnityPlayer.UnitySendMessage("SDK_callback", "OnRecord", "");
        }
    }
    public int PlayingRecord(String fileName) {
        String fullName = fileName;
        if(!fileName.endsWith(AudioFlag))
            fullName += AudioFlag;

        if(Mic.playAudio(fullName))
            return 1;
        else
            return -1;
    }
    public void StopPlayingRecord() {
        Mic.stopPlayAudio();
    }

    public int CanLocation() {
        LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(isGPS && isNetwork) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return 0;
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION))
                   return 2;
            }
            return 1;
        } else {
            if(isGPS && !isNetwork)
                return 2;
            else {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION))
                        return 2;
                }
            }
            return 1;
        }
    }
    public int CanVoice() {
        int result = 0;
	    int bufferSizeInBytes = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
	    AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
	    try {
            audioRecord.startRecording();
            //if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
            //    result = 2;
            audioRecord.stop();
        } catch(IllegalStateException e) {
	        e.printStackTrace();
            result = 1;
        }

        audioRecord.release();
        audioRecord = null;

        if(result == 1) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO))
                    result = 2;
            }
        }

        return result;
    }

    public int CanCamera(boolean deep) {
        /*int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(permission == PackageManager.PERMISSION_GRANTED)
            return 0;
        if(!deep && permission == PackageManager.PERMISSION_DENIED)
            return 2;
        if(deep && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                return 2;
        }
        return 1;*/
        return 1;
    }

	@TargetApi(19)
    private boolean isNotificationEnabled() {

        String CHECK_OP_NO_THROW = "checkOpNoThrow";
        String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

        AppOpsManager mAppOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        ApplicationInfo appInfo = getApplicationInfo();
        String pkg = getApplicationContext().getPackageName();
        int uid = appInfo.uid;

        Class appOpsClass = null;
        /* Context.APP_OPS_MANAGER */
        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());
            Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE,
                    String.class);
            Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);

            int value = (Integer) opPostNotificationValue.get(Integer.class);
            return ((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

	public int CanPushNotification() {
		if(Build.VERSION.SDK_INT >= 19) {
				if (isNotificationEnabled())
					return 0;
				else
					return 1;
			}
		return 0;
	}

    public void OpeningLocation() {
        String[] mPermissionList = new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        m_rxPermissions
                .request(mPermissionList)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.i("", "onSubscribe");
                    }
                    @Override
                    public void onNext(Boolean value) {
                        //  value ???ture  ??????????????????????????????????????????????????????????????? ?????????false
                        if (value) {
                            //Toast.makeText(UnityPlayerActivity.this, "??????", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(UnityPlayerActivity.this, "????????????", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("", "onError" + e.toString());
                    }

                    @Override
                    public void onComplete() {
                        Log.i("", "onComplete");
                    }
                });
    }

    public void OpeningVoice() {
        m_rxPermissions
                .request(Manifest.permission.RECORD_AUDIO)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.i("", "onSubscribe");
                    }
                    @Override
                    public void onNext(Boolean value) {
                        //  value ???ture  ??????????????????????????????????????????????????????????????? ?????????false
                        if (value) {
                            //Toast.makeText(UnityPlayerActivity.this, "??????", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(UnityPlayerActivity.this, "????????????", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("", "onError" + e.toString());
                    }

                    @Override
                    public void onComplete() {
                        Log.i("", "onComplete");
                    }
                });
    }

    public void OpeningCamera() {
        /*m_rxPermissions
                .request(Manifest.permission.CAMERA)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.i("", "onSubscribe");
                    }
                    @Override
                    public void onNext(Boolean value) {
                        //  value ???ture  ??????????????????????????????????????????????????????????????? ?????????false
                        if (value) {
                            //Toast.makeText(UnityPlayerActivity.this, "??????", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(UnityPlayerActivity.this, "????????????", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("", "onError" + e.toString());
                    }

                    @Override
                    public void onComplete() {
                        Log.i("", "onComplete");
                    }
                });*/
    }

    //android????????????????????????
    public void GoingSetScene(String mode) {
        LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(mode.equals("GPS") &&(!isGPS || !isNetwork)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        } else if(mode.equals("PUSH")) {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= 26) {
                // android 8.0??????
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
            } else if (Build.VERSION.SDK_INT >= 21) {
                // android 5.0-7.0
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", getPackageName());
                intent.putExtra("app_uid", getApplicationInfo().uid);
            } else {
                // ??????
                intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.fromParts("package", getPackageName(), null));
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Intent localIntent = new Intent();
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 9) {
                localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                localIntent.setData(Uri.fromParts("package", getPackageName(), null));
            } else if (Build.VERSION.SDK_INT <= 8) {
                localIntent.setAction(Intent.ACTION_VIEW);
                localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                localIntent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
            }
            startActivity(localIntent);
        }
    }

    public byte[] LoadingFile(String fileName) {
        try {
            InputStream inputStream = m_assetManager.open(fileName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int length = 0;
            while((length = inputStream.read(m_assetBuffer)) != -1)
                outputStream.write(m_assetBuffer, 0, length);
            outputStream.close();
            inputStream.close();
            return outputStream.toByteArray();
        } catch(IOException e) {
            UnityPlayer.UnitySendMessage("SDK_callback", "Log","LoadingFile Exception: " + e.toString());
            return null;
        }
    }

	public void ForceQuiting() {
		System.exit(0);
	}


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        /*if(requestCode == REQUEST_ALL_PERMISSION)
            PushAgent.getInstance(this).onAppStart();
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);*/
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SDKController.GetInstance().onActivityResult(requestCode, resultCode, data);
    }

    //???????????????????????????
    public boolean IsWXInstall() {
        final PackageManager packageManager = this.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        if(pinfo != null) {
            for(int idx = 0; idx < pinfo.size(); ++idx) {
                String pn = pinfo.get(idx).packageName;
                if(pn.equals("com.tencent.mm"))
                    return true;
            }
        }
        return false;
    }

    public void HandleInit(String json_data) {
        SDKController.GetInstance().HandleInit(json_data);
    }
    public void HandleLogin(String json_data) {
        SDKController.GetInstance().HandleLogin(json_data);
    }
    public void HandleLoginOut(String json_data) {
        SDKController.GetInstance().HandleLoginOut(json_data);
    }
	public void HandleRelogin(String json_data) {
        SDKController.GetInstance().HandleRelogin(json_data);
    }
    public void HandlePay(String json_data) {
        SDKController.GetInstance().HandlePay(json_data);
    }
    public void HandlePostPay(String json_data) {
        SDKController.GetInstance().HandlePostPay(json_data);
    }
    public void HandleShare(String json_data) {
        SDKController.GetInstance().HandleShare(json_data);
    }
    public void HandleShowAccountCenter(String json_data) {
        SDKController.GetInstance().HandleShowAccountCenter(json_data);
    }

    public void HandleSetupAD(String json_data) {
        SDKController.GetInstance().HandleSetupAD(json_data);
    }

	public void StartAc(String appId) {
		/*Toast.makeText(MainActivity.Instance, "////////////",
				Toast.LENGTH_SHORT).show();*/
		Toast.makeText(this, "////////////",
				Toast.LENGTH_SHORT).show();
	}

	private void checkPermission() {
        String[] mPermissionList = new String[] {
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.READ_PHONE_STATE,
                // Manifest.permission.ACCESS_FINE_LOCATION,
                // Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,

                Manifest.permission.VIBRATE,
                //Manifest.permission.REQUEST_INSTALL_PACKAGES
        };

        m_rxPermissions = new RxPermissions(this);

        /*m_rxPermissions.requestEach(mPermissionList).subscribe(new Consumer<com.tbruyelle.rxpermissions2.Permission>() {
                @Override
                public void accept(com.tbruyelle.rxpermissions2.Permission permission) throws Exception {
                    Log.i("Notice:", permission.name + permission.granted);
                }
            });*/

        m_rxPermissions
                .request(mPermissionList)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.i("", "onSubscribe");
                    }
                    @Override
                    public void onNext(Boolean value) {
                        //  value ???ture  ??????????????????????????????????????????????????????????????? ?????????false
                        if (value) {
                            //Toast.makeText(UnityPlayerActivity.this, "??????", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(UnityPlayerActivity.this, "????????????", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("", "onError" + e.toString());
                    }

                    @Override
                    public void onComplete() {
                        Log.i("", "onComplete");
                    }
                });
    }
    // Setup activity layout
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        //if(Build.VERSION.SDK_INT >= 23) {
        // requestPermissions(mPermissionList, REQUEST_ALL_PERMISSION);
        //}

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        mUnityPlayer = new UnityPlayer(this);
        setContentView(mUnityPlayer);
        mUnityPlayer.requestFocus();

        checkPermission();

		m_vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        m_assetManager = getAssets();
        m_assetBuffer = new byte[2048];

        SDKController.GetInstance().onActivityCreate(this);
    }

    @Override protected void onNewIntent(Intent intent)
    {
        // To support deep linking, we need to make sure that the client can get access to
        // the last sent intent. The clients access this through a JNI api that allows them
        // to get the intent set on launch. To update that after launch we have to manually
        // replace the intent with the one caught here.
        setIntent(intent);
    }

    // Quit Unity
    @Override protected void onDestroy ()
    {
        mUnityPlayer.quit();
        super.onDestroy();
    }

    @Override protected void onRestart() {
        super.onRestart();
    }

    // Resume Unity
    @Override protected void onResume()
    {
        super.onResume();
        mUnityPlayer.resume();
        SDKController.GetInstance().onResume();
    }

    // Pause Unity
    @Override protected void onPause()
    {
        super.onPause();
        mUnityPlayer.pause();
        SDKController.GetInstance().onPause();
    }

    @Override protected void onStop()
    {
        super.onStop();
        SDKController.GetInstance().onStop();
        mUnityPlayer.stop();
    }


    @Override protected void onStart()
    {
        super.onStart();
        mUnityPlayer.start();
        SDKController.GetInstance().onStart();
    }

    // Low Memory Unity
    @Override public void onLowMemory()
    {
        super.onLowMemory();
        mUnityPlayer.lowMemory();
    }

    // Trim Memory Unity
    @Override public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_RUNNING_CRITICAL)
        {
            mUnityPlayer.lowMemory();
        }
    }

    // This ensures the layout will be correct.
    @Override public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mUnityPlayer.configurationChanged(newConfig);
    }

    // Notify Unity of the focus change.
    @Override public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
    }

    // For some reason the multiple keyevent type is not supported by the ndk.
    // Force event injection by overriding dispatchKeyEvent().
    @Override public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
            return mUnityPlayer.injectEvent(event);
        return super.dispatchKeyEvent(event);
    }

    // Pass any events not handled by (unfocused) views straight to UnityPlayer
    @Override public boolean onKeyUp(int keyCode, KeyEvent event)     { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event)   { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onTouchEvent(MotionEvent event)          { return mUnityPlayer.injectEvent(event); }
    /*API12*/ public boolean onGenericMotionEvent(MotionEvent event)  { return mUnityPlayer.injectEvent(event); }
}
