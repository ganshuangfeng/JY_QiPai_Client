package com.wxsdk.my;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.gaoshouhuyu.cymj.vivo.Config;
import com.gaoshouhuyu.cymj.vivo.UnityPlayerActivity;
import com.gaoshouhuyu.cymj.vivo.VivoSign;
import com.gaoshouhuyu.cymj.vivo.VivoUnionHelper;
import com.gaoshouhuyu.cymj.vivo.bean.OrderBean;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.unity3d.player.UnityPlayer;

import org.json.JSONObject;

import java.io.File;


//import com.bytedance.sdk.openadsdk.TTAdConfig;
//import com.bytedance.sdk.openadsdk.TTAdConstant;
//import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.vivo.unionsdk.open.OrderResultInfo;
import com.vivo.unionsdk.open.VivoAccountCallback;
import com.vivo.unionsdk.open.VivoConstants;
import com.vivo.unionsdk.open.VivoExitCallback;
import com.vivo.unionsdk.open.VivoPayCallback;
import com.vivo.unionsdk.open.VivoPayInfo;
import com.vivo.unionsdk.open.VivoRealNameInfoCallback;
import com.vivo.unionsdk.open.VivoUnionSDK;

/**
 * Created by Administrator on 2016/9/6 0006.
 */
public class SDKController extends Activity {
    private static SDKController _instance;
    private  SDKController(){};

    private boolean m_isLogining = false;
    public boolean isLogining() { return m_isLogining; }
    public void markLogining(boolean value) { m_isLogining = value; }

	private boolean m_isRelogin = false;
    public boolean isRelogin() { return m_isRelogin; }
    public void markRelogin(boolean value) { m_isRelogin = value; }

    private UnityPlayerActivity mainActivity;
    public static SDKController GetInstance(){
        if(_instance == null)
        {
            _instance = new SDKController();
        }
        return _instance;
    }

//    private MiitHelper miitHelper;

    private final String WXID = "wxf64ec3fb99c28771";
    public String getWXID() { return WXID; }
    private IWXAPI api;

    public void RegisterWeChat(Context context) {
        api = WXAPIFactory.createWXAPI(context, WXID);
        boolean issuccess =  api.registerApp(WXID);
        if (issuccess)
            UnityPlayer.UnitySendMessage("SDK_callback", "Log","[SDK] RegisterWeChat OK:" + WXID);
        else
            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] RegisterWeChat Fail:" + WXID);
    }

    public void HandleInit(String json_data) {
        int result = 0;
        try {
            //JSONObject jsonObject = new JSONObject(json_data);

        }catch(Exception e) {
            result = -1;
            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] HandleInit exception:" + e.getMessage());
        }

        UnityPlayer.UnitySendMessage("SDK_callback", "InitResult", String.format("{\"result\":%d}", result));
    }
    public void HandleLogin(String json_data) {
        int result = 0;
        try {
            VivoUnionSDK.login(mainActivity);

        }catch(Exception e) {
            result = -1;
            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] HandleLogin exception:" + e.getMessage());
        }

        if(result != 0)
            UnityPlayer.UnitySendMessage("SDK_callback", "LoginResult", String.format("{\"result\":%d}", result));
    }
    public void HandleLoginOut(String json_data) {
        int result = 0;
        try {
            //JSONObject jsonObject = new JSONObject(json_data);

        }catch(Exception e) {
            result = -1;
            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] HandleLoginOut exception:" + e.getMessage());
        }
        UnityPlayer.UnitySendMessage("SDK_callback", "LoginOutResult", String.format("{\"result\":%d}", result));

    }
	public void HandleRelogin(String json_data) {
        int result = 0;
        try {
            //JSONObject jsonObject = new JSONObject(json_data);
            do {
                if(!api.isWXAppInstalled()) {
                    result = -2;
                    break;
                }

                final SendAuth.Req req = new SendAuth.Req();
                req.transaction = Transaction.RequestLogin;
                req.scope = "snsapi_userinfo";   // ????????????????????????????????????????????????????????????snsapi_userinfo
                req.state = "wechat_sdk_demo_test";
                if(!api.sendReq(req)) {
                    result = -3;
                    break;
                }

                markLogining(true);
				markRelogin(true);

                UnityPlayer.UnitySendMessage("SDK_callback", "Log","[SDK] HandleRelogin send req");
            }while(false);

        }catch(Exception e) {
            result = -1;
            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] HandleRelogin exception:" + e.getMessage());
        }

        if(result != 0)
            UnityPlayer.UnitySendMessage("SDK_callback", "ReloginResult", String.format("{\"result\":%d}", result));
    }

    //---------------------------------------------------------vivo??????--------------------------------------------------------
    //??????
    private String amount;
    //?????????openId??????
    private String vivoOpenId;
    //?????????
    private String developerPayload;
    //---------------------------------------------------------vivo??????--------------------------------------------------------

    public void HandlePay(String json_data) {
        try
        {
            JSONObject jsonObject = new JSONObject(json_data);
            String productName = jsonObject.getString("productName");
            amount = jsonObject.getString("amount");
            developerPayload = jsonObject.getString("developerPayload");
            String productDesc = jsonObject.getString("productDesc");
            String notifyUrl = jsonObject.getString("notifyUrl");
            //??????????????????
            OrderBean  order_bean  = new OrderBean(developerPayload, notifyUrl,amount,productName,productDesc);
            VivoPayInfo vivoPayInfo = VivoSign.createPayInfo(vivoOpenId,order_bean);;
            VivoUnionSDK.payV2(mainActivity,vivoPayInfo,mVivoPayCallback);
        }
        catch(Exception e)
        {
            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] HandlePay exception:" + e.getMessage());
        }
    }

    private VivoPayCallback mVivoPayCallback = new VivoPayCallback() {
        // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????
        @Override
        public void onVivoPayResult(int i, OrderResultInfo orderResultInfo)
        {
            if (i == VivoConstants.PAYMENT_RESULT_CODE_SUCCESS) {
                Toast.makeText(mainActivity, "????????????", Toast.LENGTH_SHORT).show();
            }
            else if (i == VivoConstants.PAYMENT_RESULT_CODE_CANCEL){
                Toast.makeText(mainActivity, "????????????", Toast.LENGTH_SHORT).show();
                Log.i("?????????????????????????????????vivo???????????? ??????", "onVivoPayResult: " + orderResultInfo.toString());
                VivoUnionHelper.SendPayResult(developerPayload, orderResultInfo.getTransNo(), amount);
            }
            else if (i == VivoConstants.PAYMENT_RESULT_CODE_UNKNOWN) { }
            else {
                Toast.makeText(mainActivity, "????????????", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void HandleShare(String json_data) {
        int result = 0;
        try {
            JSONObject jsonObject = new JSONObject(json_data);
            int shareType = jsonObject.getInt("type");
            switch (shareType) {
                case SDKController.Type.WeiChatInterfaceType_ShareUrl:
                    result = ShareLinkUrl(jsonObject);
                    break;
                case SDKController.Type.WeiChatInterfaceType_ShareImage:
                    result = ShareImage(jsonObject);
                    break;
                default:
                    result = -6;
                    break;
            }

        }catch(Exception e) {
            result = -1;
            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] HandleShare exception:" + e.getMessage());
        }

        if(result != 0)
            UnityPlayer.UnitySendMessage("SDK_callback", "ShareResult", String.format("{\"result\":%d}", result));
    }
    public void HandleShowAccountCenter(String json_data) {
        int result = 0;
        try {
            //JSONObject jsonObject = new JSONObject(json_data);

        }catch(Exception e) {
            result = -1;
            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] HandleShowAccountCenter exception:" + e.getMessage());
        }

        UnityPlayer.UnitySendMessage("SDK_callback", "ShowAccountCenterResult", String.format("{\"result\":%d}", result));
    }

    public void HandleSetupAD(String json_data) {
//        int result = 0;
//        try {
//            JSONObject jsonObject = new JSONObject(json_data);
//
//            String appId = jsonObject.getString("appId");
//            String appName = jsonObject.getString("appName");
//            boolean isDebug = jsonObject.getBoolean("isDebug");
//
//            TTAdConfig config = new TTAdConfig.Builder()
//                    .appId(appId)
//                    .appName(appName)
//                    .debug(isDebug)
//                    .useTextureView(false)
//                    .allowShowNotify(true)
//                    .allowShowPageWhenScreenLock(true)
//                    .directDownloadNetworkType(TTAdConstant.NETWORK_STATE_WIFI, TTAdConstant.NETWORK_STATE_3G)
//                    .supportMultiProcess(false)
//                    .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK)
//                    .build();
//            TTAdSdk.init(mainActivity, config);
//
//            UnityPlayer.UnitySendMessage("SDK_callback", "Log","[SDK] HandleSetupAD setup ok:" + appId);
//        }catch(Exception e) {
//            result = -1;
//            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] HandleSetupAD exception:" + e.getMessage());
//        }
//        UnityPlayer.UnitySendMessage("SDK_callback", "HandleSetupADResult", String.format("{\"result\":%d}", result));
    }

    public void HandleScanFile(String filePath) {
        int result = 0;
        try{
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(new File(filePath)));
        }catch (Exception e){
            result = -1;
            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] HandleScanFile exception:" + e.getMessage());
        }
        UnityPlayer.UnitySendMessage("SDK_callback", "HandleScanFileResult", String.format("{\"result\":%d}", result));
    }

    public void onAppCreate(Application application) {
    }
    public void onAppDestroy() {
    }

    public void onActivityCreate(UnityPlayerActivity activity) {
        mainActivity = activity;
        //RegisterWeChat(activity);
        VivoUnionSDK.registerAccountCallback(mainActivity, new VivoAccountCallback()
        {
            @Override
            public void onVivoAccountLogin(String userName, String openId, String authToken)
            {
                //???????????????oppenId???????????????????????????
                VivoUnionSDK.getRealNameInfo(mainActivity, new VivoRealNameInfoCallback()
                {
                    @Override
                    public void onGetRealNameInfoSucc(boolean isRealName, int age)
                    {
                        if(isRealName){
                            UnityPlayer.UnitySendMessage("SDK_callback", "Log", "[SDK] gamePlayExtra ok:" );
                            try
                            {
                                JSONObject jsonResult = new JSONObject();
                                jsonResult.put("result", 0);
                                jsonResult.put("userName", userName);
                                jsonResult.put("openId", openId);
                                jsonResult.put("authToken", authToken);
                                UnityPlayer.UnitySendMessage("SDK_callback", "LoginResult", jsonResult.toString());
                                Log.d(jsonResult.toString(), "onVivoAccountLogin: ");
                                vivoOpenId = openId ;
                            }
                            catch (Exception e)
                            {
                                UnityPlayer.UnitySendMessage("SDK_callback", "LoginError", String.format("{\"result\":%d}", -2));
                            }
                        }
                        else
                            UnityPlayer.UnitySendMessage("SDK_callback", "LoginResult",String.format("{\"result\":%d}", -1));
                    }

                    @Override
                    public void onGetRealNameInfoFailed()
                    {
                        UnityPlayer.UnitySendMessage("SDK_callback", "LoginResult",String.format("{\"result\":%d}", -5));
                    }
                });
            }
            @Override
            public void onVivoAccountLogout(int i) {
                //????????????
                UnityPlayer.UnitySendMessage("SDK_callback", "LoginResult", String.format("{\"result\":%d}", -3));
            }

            @Override
            public void onVivoAccountLoginCancel() {
                //????????????
                UnityPlayer.UnitySendMessage("SDK_callback", "LoginResult", String.format("{\"result\":%d}", -4));
            }
        });
    }
    public void onActivityDestroy() {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    public void onResume() {
        if(isLogining()) {
            markLogining(false);
            UnityPlayer.UnitySendMessage("SDK_callback", "LoginResult", String.format("{\"result\":%d}", -4));
        }
    }

    public void onPause() {
    }

    public void onStart() {
    }
    public void onStop() {
    }

    public void onResp(BaseResp resp) {
        try {
            JSONObject jsonResult = null;
            switch (resp.transaction) {
                case SDKController.Transaction.RequestLogin:
                    markLogining(false);

                    jsonResult = new JSONObject();
                    if(resp.errCode == 0) {
                        SendAuth.Resp auth = (SendAuth.Resp) resp;
                        jsonResult.put("result", 0);
                        jsonResult.put("token", auth.code);
						jsonResult.put("appid", WXID);
                    } else {
                        jsonResult.put("result", -5);
                        jsonResult.put("errno", resp.errCode);
                    }
					if(isRelogin())
						UnityPlayer.UnitySendMessage("SDK_callback", "ReloginResult", jsonResult.toString());
					else
	                    UnityPlayer.UnitySendMessage("SDK_callback", "LoginResult", jsonResult.toString());

                    break;
                case SDKController.Transaction.ShareImage:
                case SDKController.Transaction.ShareUrl:
                    jsonResult = new JSONObject();
                    if(resp.errCode == 0) {
                        jsonResult.put("result", 0);
                    } else {
                        jsonResult.put("result", -5);
                        jsonResult.put("errno", resp.errCode);
                    }
                    UnityPlayer.UnitySendMessage("SDK_callback", "ShareResult", jsonResult.toString());

                    break;
            }
        }catch (Exception e) {
            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] onResp exception:" + e.getMessage());
        }
    }

    public int ShareLinkUrl(JSONObject jsonObject) {
        try {
            String url = jsonObject.getString("url");
            String title = jsonObject.getString("title");
            String description = jsonObject.getString("description");
            String icon = jsonObject.getString("icon");
            boolean isCircleOfFriends = jsonObject.getBoolean("isCircleOfFriends");

            WXWebpageObject webpage = new WXWebpageObject();
            webpage.webpageUrl = url;
            WXMediaMessage msg = new WXMediaMessage(webpage);
            msg.title = title;
            msg.description = description;
            Resources re = mainActivity.getResources();
            Bitmap bmp = null;
            if(!icon.isEmpty())
                bmp = BitmapFactory.decodeFile(icon);
            if(bmp == null) {
                bmp = BitmapFactory.decodeResource(re, re.getIdentifier("app_icon", "drawable", mainActivity.getPackageName()));
                UnityPlayer.UnitySendMessage("SDK_callback", "LogError", "[SDK] ShareLinkUrl Load icon Failed:" + icon);
            }
            if(bmp != null) {
                Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 100, 100, true);
                bmp.recycle();
                msg.thumbData = Util.bmpToByteArray(thumbBmp, true);
            }

            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.transaction = Transaction.ShareUrl;
            req.message = msg;
            req.scene = isCircleOfFriends ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
            if(!api.sendReq(req))
                return -3;
        }catch (Exception e) {
            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] ShareLinkUrl exception:" + e.getMessage());
        }
        return 0;
    }

    public int ShareImage (JSONObject jsonObject) {
        try {
            String imgFile = jsonObject.getString("imgFile");
            boolean isCircleOfFriends = jsonObject.getBoolean("isCircleOfFriends");

            Resources re = mainActivity.getResources();
            Bitmap bmp = BitmapFactory.decodeFile(imgFile);
            if(bmp == null) {
                UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] ShareLinkUrl Load imgFile failed:" + imgFile);
                return -7;
            }

            WXImageObject imgObj = new WXImageObject(bmp);
            WXMediaMessage msg = new WXMediaMessage();
            msg.mediaObject = imgObj;

            // ????????????????????????
            Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 100, 100, true);
            bmp.recycle();
            msg.thumbData = Util.bmpToByteArray(thumbBmp, true);
            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.scene = isCircleOfFriends ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
            req.transaction = Transaction.ShareImage;
            req.message = msg;
            if(!api.sendReq(req))
                return -3;

            UnityPlayer.UnitySendMessage("SDK_callback", "Log","[SDK] ShareImage Environment path:" + Environment.getExternalStorageDirectory().getAbsolutePath());
        }catch (Exception e) {
            UnityPlayer.UnitySendMessage("SDK_callback", "LogError","[SDK] ShareImage exception:" + e.getMessage());
        }

        return 0;
    }

    /*//????????????
    public void ShareText(JSONObject jsonObject) {
        //String description = "";
        String text = "";
        boolean isCircleOfFriends = false;
        try {
            //description = jsonObject.getString("description");
            text = jsonObject.getString("text");
            isCircleOfFriends = jsonObject.getBoolean("isCircleOfFriends");
        }catch (Exception e) {
            UnityPlayer.UnitySendMessage("SDK_callback", "Log","ShareText failure: " + e.toString());
            //Toast.makeText(mainActivity, e.toString(), Toast.LENGTH_SHORT).show();
            //Toast.makeText(MainActivity.Instance, e.toString(), Toast.LENGTH_SHORT).show();
        }
        WXTextObject textObj = new WXTextObject();
        textObj.text = text;
        // ???WXTextObject?????????????????????WXMediaMessage??????
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = textObj;
        // ?????????????????????????????????????title??????????????????
//         msg.title = "Will be ignored";
        //msg.description = description;
        // ????????????????Req
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = Transaction.ShareText; // transaction??????????????????????????????????????
        req.message = msg;
        req.scene = isCircleOfFriends ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
        // ??????api???????????????????????????????
        SendReq(req);
    }

    public void ShareVideo (JSONObject jsonObject) {
        String url = "";
        String title = "";
        String description = "";
        boolean isCircleOfFriends = false;
        try {
            url = jsonObject.getString("url");
            title = jsonObject.getString("title");
            description = jsonObject.getString("description");
            isCircleOfFriends = jsonObject.getBoolean("isCircleOfFriends");
        }catch (Exception e) {
            //Toast.makeText(mainActivity, e.toString(), Toast.LENGTH_SHORT).show();
            //Toast.makeText(MainActivity.Instance, e.toString(), Toast.LENGTH_SHORT).show();
        }

        WXVideoObject video = new WXVideoObject();
        video.videoUrl = url;

        Resources re = mainActivity.getResources();
        Bitmap bmp = BitmapFactory.decodeResource(re, re.getIdentifier("app_icon", "drawable", mainActivity.getPackageName()));
        //Resources re = MainActivity.Instance.getResources();
        //Bitmap bmp = BitmapFactory.decodeResource(re, re.getIdentifier("app_icon", "drawable", MainActivity.Instance.getPackageName()));

        WXMediaMessage msg = new WXMediaMessage();
        msg.title = title;
        msg.description = description;
        msg.mediaObject = video;

        // ????????????????????????
        Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 100, 100, true);
        bmp.recycle();
        msg.thumbData = Util.bmpToByteArray(thumbBmp, true);
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.scene = isCircleOfFriends ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
        req.transaction = Transaction.ShareVideo;
        req.message = msg;
        SendReq(req);
    }

    public void ShareMusic (JSONObject jsonObject) {
        @SuppressWarnings("unused")
		String url = "";
        String title = "";
        String description = "";
        boolean isCircleOfFriends = false;
        try {
            url = jsonObject.getString("url");
            title = jsonObject.getString("title");
            description = jsonObject.getString("description");
            isCircleOfFriends = jsonObject.getBoolean("isCircleOfFriends");
        }catch (Exception e) {
            //Toast.makeText(MainActivity.Instance, e.toString(), Toast.LENGTH_SHORT).show();
            Toast.makeText(mainActivity, e.toString(), Toast.LENGTH_SHORT).show();
        }
        WXMusicObject music = new WXMusicObject();
        music.musicUrl = "url";

        Resources re = mainActivity.getResources();
        Bitmap bmp = BitmapFactory.decodeResource(re, re.getIdentifier("app_icon", "drawable", mainActivity.getPackageName()));
        //Resources re = MainActivity.Instance.getResources();
        //Bitmap bmp = BitmapFactory.decodeResource(re, re.getIdentifier("app_icon", "drawable", MainActivity.Instance.getPackageName()));

        WXMediaMessage msg = new WXMediaMessage();
        msg.title = title;
        msg.description = description;

        msg.mediaObject = music;

        // ????????????????????????
        Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 100, 100, true);
        bmp.recycle();
        msg.thumbData = Util.bmpToByteArray(thumbBmp, true);
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.scene = isCircleOfFriends ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
        req.transaction = Transaction.ShareMusic;
        req.message = msg;
        SendReq(req);
    }

    */
    /*public void WeChatLogin()
    {
        SendAuth.Req req = new SendAuth.Req();
        req.transaction = Transaction.RequestLogin;
        req.scope = "snsapi_userinfo";   // ????????????????????????????????????????????????????????????snsapi_userinfo
        req.state = "wechat_sdk_demo_test";
        SendReq(req);
        UnityPlayer.UnitySendMessage("SDK_callback", "Log","SendReq ~~~~~~~~~");
        //UnityPlayer.UnitySendMessage("Android", "CallBack", "SendReq ~~~~~~~~~");
    }
    public void SendReq(BaseReq req, String callbackName)
    {
        boolean issuccess = api.sendReq(req);
        if (!issuccess)
        {
            UnityPlayer.UnitySendMessage("SDK_callback", "OnWeChatError", "SendReqFail" + ":" + req.transaction);

            UnityPlayer.UnitySendMessage("SDK_callback", "Log","SendReq ~~~~~~~~~ fail");
            //UnityPlayer.UnitySendMessage("Android", "CallBack", "SendReq ~~~~~~~~~ fail");
        }else{
            UnityPlayer.UnitySendMessage("SDK_callback", "Log","SendReq ~~~~~~~~~ succes");
            //UnityPlayer.UnitySendMessage("Android", "CallBack", "SendReq ~~~~~~~~~ succes");
        }
    }*/

    private String m_pushDeviceToken = "";
    public void SavePushDeviceToken(String s) {
        m_pushDeviceToken = s;
    }
    public String GetPushDeviceToken() {
        return m_pushDeviceToken;
    }

    private String m_oaid = "";
    public void SaveOAID(String value) { m_oaid = value; }
    public String GetOAID() { return m_oaid; }

    public interface Type {
        int WeiChatInterfaceType_IsWeiChatInstalled = 1; //????????????????????????
        int WeiChatInterfaceType_RequestLogin = 2; //????????????
        int WeiChatInterfaceType_ShareUrl = 3; //????????????
        int WeiChatInterfaceType_ShareText = 4; //????????????
        int WeiChatInterfaceType_ShareMusic = 5;//????????????
        int WeiChatInterfaceType_ShareVideo = 6;//????????????
        int WeiChatInterfaceType_ShareImage = 7;//????????????
    }

    public interface Transaction {
        String IsWeiChatInstalled = "isInstalled"; //????????????????????????
        String RequestLogin = "login"; //????????????
        String ShareUrl = "shareUrl"; //????????????
        String ShareText = "shareText"; //????????????
        String ShareMusic = "shareMusic";//????????????
        String ShareVideo = "shareVideo";//????????????
        String ShareImage = "shareImage";//????????????
    }
}

