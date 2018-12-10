
package com.tencent.msdk.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import com.tencent.msdk.WeGame;
import com.tencent.msdk.api.refactor.Router;
import com.tencent.msdk.consts.CallbackFlag;
import com.tencent.msdk.consts.EPlatform;
import com.tencent.msdk.framework.MSDKEnv;
import com.tencent.msdk.framework.mlog.MLog;
import com.tencent.msdk.lifecycle.MSDKLifecycleManager;
import com.tencent.msdk.myapp.autoupdate.WGSaveUpdateObserver;
import com.tencent.msdk.notice.NoticeInfo;
import com.tencent.msdk.notice.eMSDK_SCREENDIR;
import com.tencent.msdk.qq.ApiName;
import com.tencent.msdk.stat.eBuglyLogLevel;
import com.tencent.msdk.tools.SharedPreferencesTool;
import com.tencent.msdk.weixin.BtnBase;
import com.tencent.msdk.weixin.MsgBase;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.special.httpdns.Resolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public final class WGPlatform {

    // MSDK代码入口加载so,避免加载不即时导致Crash
    static {
        MSDKEnv.getInstance().tryLoadSo();
    }

    public static boolean isInited = false;

    private static final String TAG = WGPlatform.class.getName();
    /**
     * 初始化方法 SDK
     *
     * @param activity 上下文
     */
    public static Boolean IsDifferentActivity(Activity activity) {
        return WeGame.getInstance().IsDifferentActivity(activity);
    }

    public static void DestroyActivity() {
    }

    /**
     * 初始化方法 SDK
     *
     * @param activity 上下文
     * @param baseInfo 基本配置信息
     * 		wx_appId 微信wx_appId
     * 		wxAppKey 微信wxAppKey
     * 		qq_appId Qzone qq_appId
     * 		qqAppKey Qzone qqAppKey
     * 		offerId 支付 offerid
     */
    public static void Initialized(Activity activity, MsdkBaseInfo baseInfo) {
        MSDKLifecycleManager.getInstance().init(activity);
        WeGame.getInstance().setmActivity(activity);
        WeGame.getInstance().setFirstGameActivity(activity);
        // 云控加载数据,统一在c++层加载，不需要先初始化 by qingcui
        //CloudMsgManager.getInstance().init(activity);
        Router.getInstance().loadConfig(); // 加载云控的新老版本控制
        // 初始化httpdns, revise by linkxzhou
        // MSDKDnsResolver.getInstance().init(activity.getApplicationContext());
        Resolver.getInstance().init(activity.getApplicationContext(), 1000, false);
        isInited = true;

        try {
            // 如果是安装后第一次启动，则预加载tbs的x5内核
            if (SharedPreferencesTool.getBoolean(activity.getApplication(), "first_launch", true)) {
                MLog.i("first launch, initX5Environment");
                QbSdk.initX5Environment(activity, null);
                SharedPreferencesTool.putBoolean(activity, "first_launch", false);
            }
        } catch (Exception e) {
            MLog.e(e);
        }

        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().Initialized(activity, baseInfo);

            // 初始化完成后上报X5内核版本号
            String qbsdkTbsVersion = "QbSdk.getTbsVersion:" + QbSdk.getTbsVersion(activity);
            MLog.i(qbsdkTbsVersion);
            WGPlatform.WGBuglyLog(eBuglyLogLevel.eBuglyLogLevel_I, qbsdkTbsVersion);
            return;
        }
    }

    /**
     * 设置java层委托
     * @param observer 委托的接口对象
	 *
	 * 回调有如下几种:
	 *   OnLoginNotify: 登陆回调
	 *   OnShareNotify: 分享回调
	 *   OnWakeupNotify: 被唤起回调
	 *   OnRelationNotify: 关系链查询回调
	 */
    public static void WGSetObserver(WGPlatformObserver observer) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSetObserver(observer);
            return;
        }
    }

    public static void WGSetRealNameAuthObserver(WGRealNameAuthObserver d) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSetRealNameAuthObserver(d);
            return;
        }
    }

    public static void WGSetWebviewObserver(WGWebviewObserver d) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSetWebviewObserver(d);
            return;
        }
    }

    /**
     *
     */
    public static void WGSetGroupObserver(WGGroupObserver Observer) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSetGroupObserver(Observer);
            return;
        }
    }


    /**
     * @param intent 大厅拉起游戏时传递的Intent
     */
    public static boolean wakeUpFromHall(Intent intent) {
        boolean ret = WeGame.getInstance().wakeUpFromHall(intent);

        Map<String, String> params = new HashMap<String, String>();
        params.put("flag", ret? "0":"-1");
        WeGame.getInstance().reportFunction(true,"wakeUpFromHall", params);
        return ret;
    }

    /**
     * 处理平台的拉起
     *
     * @param intent 平台拉起游戏时传入的Intent
     */
    public static void handleCallback(Intent intent) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().handleCallback(intent);
            return;
        }
    }

    public static void onRestart() {
        // 这个生命周期函数 暂时没有什么重要作用，考虑迁移到onStart
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().onRestart();
            return;
        }
    }

    public static void onResume() {
        MSDKLifecycleManager.getInstance().onResumeAdd(false);
        MLog.i("Lifecycle goto New Lifecycle onResume");

    }

    public static void onPause() {
        MSDKLifecycleManager.getInstance().onPausedAdd(false);
        MLog.i("Lifecycle goto New Lifecycle onPause");
    }

    public static void onStop() {
        MSDKLifecycleManager.getInstance().onStoppedAdd(false);
        MLog.i("Lifecycle goto New Lifecycle onStop");
    }

    public static void onDestory(Activity game) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            // 14以上走新的生命周期
            MLog.i("Lifecycle goto New Lifecycle onDestory");
            return;
        }
        // 反射方式目前无法出发ondestroy
        WeGame.getInstance().handlerOnDestroy(game);
    }

    // 在某些低端机上调用登录后，由于内存紧张导致APP被系统回收，登录成功后无法成功回传数据。
 	public static void onActivityResult(int requestCode, int resultCode,
 			Intent data) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().onActivityResult(requestCode, resultCode, data);
            return;
        }
 	}


    /**
     * 设置QZONE权限 ，QZONE登录的时候调用
     *
     * @param permissions 调用WGQZonePermissions类里的变量,多个权限用","连接 如
     *            WGQZonePermissions.eOPEN_PERMISSION_ADD_ALBUM
     *            +","+WGQZonePermissions.eOPEN_PERMISSION_ADD_ONE_BLOG
     */
    public static void WGSetPermission(int permissions) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSetPermission(permissions);
            return;
        }
    }

    /**
     * 返回当前SDK版本号
     * @return String 版本号码
     */
    public static String WGGetVersion() {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGGetVersion();
        }
        return "";
    }

    public static int WGGetLoginRecord(LoginRet ret) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGGetLoginRecord(ret);
        }
        return 0;
    }

    /**
	 * @return bool 返回值已弃用, 全都返回true
	 */
    public static boolean WGLogout() {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGLogout();
        }
        return true;
    }

    public static boolean WGSwitchUser(boolean switchToLaunchUser) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGSwitchUser(switchToLaunchUser);
        }
        return false;
    }

    public static void WGLogin(EPlatform platform) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGLogin(platform);
            return;
        }
    }

    public static int WGLoginOpt(EPlatform platform, int overtime) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGLoginOpt(platform, overtime);
        } else {
            MLog.i("WGLoginOpt in V2 would use WGLogin");
            WGLogin(platform);
            return CallbackFlag.eFlag_Succ;
        }
    }

    public static void WGQrCodeLogin(EPlatform platform) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGQrCodeLogin(platform);
            return;
        }
     }

    public static void WGSendToWeixin(String title, String desc, String mediaTagName,
            byte[] thumbData, int thumbDataLen,String messageExt) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToWeixin(title,desc,mediaTagName,thumbData,thumbDataLen,messageExt);
            return;
        }
    }

    public static void WGSendToWeixinWithUrl(eWechatScene scene, String title, String desc, String url, String mediaTagName, byte[] thumbImgData, int thumbImgDataLen, String messageExt){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToWeixinWithUrl(scene,title,desc,url,mediaTagName,thumbImgData,thumbImgDataLen,messageExt);
            return;
        }
    }


    public static void WGSendToWeixinWithPhoto(eWechatScene scene, String mediaTagName, byte[] imgData, int imgDataLen) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToWeixinWithPhoto(scene,mediaTagName,imgData,imgDataLen);
            return;
        }
    }

    public static void WGSendToWeixinWithPhoto(eWechatScene scene, String mediaTagName, byte[] imgData, int imgDataLen,String messageExt,String mediaAction) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToWeixinWithPhoto(scene,mediaTagName,imgData,imgDataLen,messageExt,mediaAction);
            return;
        }
    }

    public static void WGSendToWeixinWithPhotoPath(eWechatScene scene, String mediaTagName,
            String imgPath, String messageExt, String mediaAction) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToWeixinWithPhotoPath(scene,mediaTagName,imgPath,messageExt,mediaAction);
            return;
        }
    }

    public static void WGSendToWeixinWithMusic(eWechatScene scene, String  title, String  desc,
			String musicUrl, String musicDataUrl, String mediaTagName,
			byte[] imgData, int imgDataLen, String mediaExt, String mediaAction){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToWeixinWithMusic(scene,title,desc,musicUrl,musicDataUrl,mediaTagName,imgData,imgDataLen,mediaExt,mediaAction);
            return;
        }
    }

    public static void WGSendToQQWithMusic(eQQScene scene, String title,
					String desc, String musicUrl,
					String musicDataUrl,String imgUrl){

        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToQQWithMusic(scene,title,desc,musicUrl,musicDataUrl,imgUrl);
            return;
        }
    }

    public static void WGSendToQQ(eQQScene scene, String title, String desc, String url, String imgUrl, int imgUrlLen) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToQQ(scene,title,desc,url,imgUrl,imgUrlLen);
            return;
        }
    }

    public static void WGSendToQQWithPhoto(eQQScene scene, String imgFilePath) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToQQWithPhoto(scene,imgFilePath);
            return;
        }
    }

    public static void WGSendToQQWithRichPhoto(String summary, ArrayList<String> imgFilePaths) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToQQWithRichPhoto(summary,imgFilePaths);
            return;
        }
    }

    public static void WGSendToQQWithVideo(String summary, String videoPath) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToQQWithVideo(summary,videoPath);
            return;
        }
    }

    public static void WGSendToQQWithArk(eQQScene scene, String title,
                                         String desc, String url, String imgUrl, String jsonString){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToQQWithArk(eQQScene.QQScene_Session, title , desc ,url , imgUrl , jsonString);
            return;
        }
    }

    public static void WGFeedback(String body) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGFeedback(body);
            return;
        }
    }

    public static void WGEnableCrashReport(boolean bRdmEnable, boolean bMtaEnable) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGEnableCrashReport(bRdmEnable,bMtaEnable);
            return;
        }
    }

    public static void WGReportEvent(String name, String body, boolean isRealTime) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGReportEvent(name,body,isRealTime);
            return;
        }
    }

    public static void WGReportEvent(String name, HashMap<String, String> params, boolean isRealTime) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGReportEvent(name,params,isRealTime);
            return;
        }
    }

    public static void WGTestSpeed(ArrayList<String> addrList) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGTestSpeed(addrList);
            return;
        }
    }

    public static String WGGetChannelId() {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGGetChannelId();
        }
        return "";
    }

    public static String WGGetRegisterChannelId() {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGGetRegisterChannelId();
        }
        return "";
    }

    // 微信refreshtoken保持原样不变
    public static void WGRefreshWXToken() {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGRefreshWXToken();
            return;
        }
    }

    public static boolean WGIsPlatformInstalled(EPlatform platform) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGIsPlatformInstalled(platform);
        }
        return true;
    }

    public static boolean WGIsPlatformSupportApi(EPlatform platform) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGIsPlatformSupportApi(platform);
        }
        return true;
    }

    public static String WGGetPf(String gameCustomInfo) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGGetPf(gameCustomInfo);
        }
        return "";
    }

    public static String WGGetPfKey() {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGGetPfKey();
        }
        return "";
    }

    public static boolean WGCheckApiSupport(ApiName api){
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGCheckApiSupport(api);
        }
        return true;
    }

    public static void WGLogPlatformSDKVersion() {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGLogPlatformSDKVersion();
            return;
        }
    }

    public static boolean WGSendToQQGameFriend(int act, String friendOpenId, String title, String summary,
            String targetUrl, String imageUrl, String previewText, String gameTag) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGSendToQQGameFriend(act,friendOpenId,title, summary,targetUrl,imageUrl,previewText,gameTag);
        }
        return true;
    }

    public static boolean WGSendToQQGameFriend(int act, String friendOpenId, String title, String summary,
            String targetUrl, String imageUrl, String previewText, String gameTag, String msdkExtInfo) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGSendToQQGameFriend(act,friendOpenId,title, summary,targetUrl,imageUrl,previewText,gameTag,msdkExtInfo);
        }

        return true;
    }

    public static boolean WGSendToWXGameFriend(
            String friendOpenid,
            String title,
            String description,
            String messageExt,
            String mediaTagName,
            String thumbMediaId) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGSendToWXGameFriend(friendOpenid,title,description,messageExt,mediaTagName,thumbMediaId);
        }
        return true;
    }

    public static boolean WGSendToWXGameFriend(
            String friendOpenId,
            String title,
            String description,
            String messageExt,
            String mediaTagName,
            String thumbMediaId,
            String msdkExtInfo) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGSendToWXGameFriend(friendOpenId,title,description,messageExt,mediaTagName,thumbMediaId,msdkExtInfo);
        }
        return true;
    }

    public static boolean WGQueryQQMyInfo() {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGQueryQQMyInfo();
        }
        return true;
    }

    public static boolean WGQueryQQGameFriendsInfo() {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGQueryQQGameFriendsInfo();
        }
        return true;
    }

    public static boolean WGQueryWXMyInfo() {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGQueryWXMyInfo();
        }
        return true;
    }

    public static boolean WGQueryWXGameFriendsInfo() {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGQueryWXGameFriendsInfo();
        }
        return true;
    }

    public static Vector<NoticeInfo> WGGetNoticeData(String scene){
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGGetNoticeData(scene);
        }
        return new Vector<NoticeInfo>();
    }

    public static void WGShowNotice(String scene) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGShowNotice(scene);
            return;
        }
    }

    public static void WGHideScrollNotice() {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGHideScrollNotice();
            return;
        }
    }


    public static void WGOpenUrl(String url) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGOpenUrl(url);
            return;
        }
    }

    public static void WGOpenUrl(String url,eMSDK_SCREENDIR screendir) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGOpenUrl(url,screendir);
            return;
        }
    }


    public static String WGGetEncodeUrl(String url) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGGetEncodeUrl(url);
        }
        return "";
    }

    public static boolean WGOpenAmsCenter(String params) {
        MLog.e("WGOpenAmsCenter is been disable, please use WGOpenUrl");
        return false;
    }

    public static void WGLoginWithLocalInfo(){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGLogin(EPlatform.ePlatform_None);
            return;
        }
    }

    public static void WGGetNearbyPersonInfo(){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGGetNearbyPersonInfo();
            return;
        }
    }

	public static boolean WGGetLocationInfo(){
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGGetLocationInfo();
        }
    	return true;
    }

    public static boolean WGCleanLocation() {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGCleanLocation();
        }
    	return true;
    }

    public static int WGGetPaytokenValidTime() {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGGetPaytokenValidTime();
        }
    	return 0;
    }

    public static boolean WGSendMessageToWechatGameCenter(String friendOpenId, String title,
            String content, MsgBase pInfo, BtnBase pButton, String msdkExtInfo) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGSendMessageToWechatGameCenter(friendOpenId,title,content,pInfo,pButton,msdkExtInfo);
        }
        return true;
    }

    public static void WGStartSaveUpdate(boolean isUseYYB) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGStartSaveUpdate(isUseYYB);
            return;
        }
    }

    public static void WGSetSaveUpdateObserver(WGSaveUpdateObserver observer) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSetSaveUpdateObserver(observer);
            return;
        }
    }

    public static void WGCheckNeedUpdate() {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGCheckNeedUpdate();
            return;
        }
    }

    public static int WGCheckYYBInstalled() {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGCheckYYBInstalled();
        }
        return 0;
    }

    public static void WGJoinQQGroup(String qqGroupKey){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGJoinQQGroup(qqGroupKey);
            return;
        }
   }

    public static String WGGetPlatformAPPVersion(EPlatform platform) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGGetPlatformAPPVersion(platform);
        }
        return "";
    }

    public static void WGAddGameFriendToQQ(String fopenid, String desc, String message){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGAddGameFriendToQQ(fopenid,desc,message);
            return;
        }
    }

    public static void WGBindQQGroup(
    		String unionid,
			String union_name,
			String zoneid,
			String signature){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGBindQQGroup(unionid,union_name,zoneid,signature);
            return;
        }
    }

    public static void WGUnbindQQGroup(String groupOpenid,String unionid){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGUnbindQQGroup(groupOpenid,unionid);
            return;
        }
    }

    public static void WGQueryQQGroupInfo(String unionid,String zoneid){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGQueryQQGroupInfo(unionid,zoneid);
            return;
        }
    }

    public static void WGQueryQQGroupKey(String groupOpenid){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGQueryQQGroupKey(groupOpenid);
            return;
        }
    }

    public static void WGOpenWeiXinDeeplink(String link) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGOpenWeiXinDeeplink(link);
            return;
        }
    }

    public static void WGAddCardToWXCardPackage(String cardId, String timestamp, String sign){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGAddCardToWXCardPackage(cardId,timestamp,sign);
            return;
        }
    }

    public static void WGStartGameStatus(String gameStatus){
        if(Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGStartGameStatus(gameStatus);
        }
    }

    public static void WGEndGameStatus(String gameStatus, int succ, int errorCode){
        if(Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGEndGameStatus(gameStatus,succ,errorCode);
        }
    }

    public static void WGCreateWXGroup(String unionid,String chatRoomName,String chatRoomNickName) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGCreateWXGroup(unionid,chatRoomName,chatRoomNickName);
            return;
        }
    }

    public static void WGJoinWXGroup(String unionid,String chatRoomNickName) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGJoinWXGroup(unionid,chatRoomNickName);
            return;
        }
    }

    public static void WGQueryWXGroupInfo(String unionid,String openIdList){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGQueryWXGroupInfo(unionid,openIdList);
            return;
        }
    }

	public static void WGUnbindWeiXinGroup(String unionid) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGUnbindWeiXinGroup(unionid);
            return;
        }
	}

	public static void WGQueryWXGroupStatus(String unionid, eStatusType type) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGQueryWXGroupStatus(unionid,type);
            return;
        }
	}

	public static void WGSendToWXGroup(int msgType, int subType, String unionid, String title, String description,
			String messageExt, String mediaTagName, String imgUrl, String msdkExtInfo) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToWXGroup(msgType,subType,unionid,title,description,messageExt,mediaTagName,imgUrl,msdkExtInfo);
            return;
        }
	}

    public static long WGAddLocalNotification(LocalMessage localMsg) {
        if (Router.getInstance().runCppCode()) {
            return Router.getInstance().getUnifyMSDK().WGAddLocalNotification(localMsg);
        }
		return 0;
	}
    public static void WGClearLocalNotifications() {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGClearLocalNotifications();
            return;
        }
    }

    public static void WGSetPushTag(String tag){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSetPushTag(tag);
            return;
        }
    }

    public static void WGDeletePushTag(String tag){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGDeletePushTag(tag);
            return;
        }
    }

    public static void WGBuglyLog(eBuglyLogLevel level, String log) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGBuglyLog(level,log);
        }
    }

    public static void WGRealNameAuth(RealNameAuthInfo info){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGRealNameAuth(info);
            return;
        }
    }


    /**
     * android 6.0以上系统，targetAPI >= 23,系统会启用运行时权限检查
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    /*public static void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults){
    	WeGame.getInstance().onRequestPermissionsResult(requestCode, permissions, grantResults);
    }*/
    /**android 6.0以上系统，targetAPI >= 23,系统会启用运行时权限检查
     * 检查是否具有某个权限
     * @param permission
     * @return
     */
    /*public static boolean WGCheckPermission(String permission){
    	return PermissionManage.getInstance().checkPermission(permission);
    }*/
    /**android 6.0以上系统，targetAPI >= 23,系统会启用运行时权限检查
     * 申请授予某个权限
     * @param permissions
     * @param requestCode
     */
    /*public static void WGRequestPermissions(String permissions,int requestCode){
    	PermissionManage.getInstance().requestPermissions(permissions, requestCode);
    }*/
    /**
     * android 6.0以上系统，targetAPI >= 23,系统会启用运行时权限检查
     * 设置用户授予权限的回调
     */
    /*
    public static void WGSetPermissionsObserver(WGPermissionsObserver observer){
    	PermissionManage.getInstance().setPermissionsListener(observer);
    }*/

    public static void  WGShareToWXGameline(byte[] data,String gameExtra){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGShareToWXGameline(data,gameExtra);
            return;
        }
    }

    public static void WGSendToWeixinWithVideo(eWechatScene scene, String  title, String  desc,
                                               String thumbUrl, String videoUrl, String filePath,String mediaTagName ,
                                               String mediaAction,String mediaExt){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToWeixinWithVideo(scene, title, desc, thumbUrl, videoUrl, filePath, mediaTagName, mediaAction, mediaExt);
            return;
        }
    }

    public static void WGSendToQQWithPhoto(eQQScene scene,String imgFilePath,String extraScene,String messageExt) {
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToQQWithPhoto(scene,imgFilePath, extraScene, messageExt);
            return;
        }
    }

    public static void WGSendToWXWithMiniApp(eWechatScene scene, String title, String desc,byte[] thumbImgData, int
            lens,String webpageUrl,String userName,String path,boolean withShareTicket,String messageExt, String
            messageAction){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGSendToWXWithMiniApp(scene.value, title, desc,thumbImgData,lens,
                    webpageUrl, userName, path,withShareTicket,messageExt,messageAction);
            return;
        }
    }

    public static void WGCreateQQGroupV2(GameGuild gameGuild){
        if (Router.getInstance().runCppCode()) {
            MLog.i("WGCreateQQGroupV2");
            Router.getInstance().getUnifyMSDK().WGCreateQQGroupV2(gameGuild);
            return;
        }
    }
    public static void WGJoinQQGroupV2(GameGuild gameGuild,String groupId){
        if (Router.getInstance().runCppCode()) {
            MLog.i("WGJoinQQGroupV2");
            Router.getInstance().getUnifyMSDK().WGJoinQQGroupV2(gameGuild,groupId);
            return;
        }
    }
    public static void WGUnbindQQGroupV2(GameGuild gameGuild){
        if (Router.getInstance().runCppCode()) {
            MLog.i("WGUnbindQQGroupV2");
            Router.getInstance().getUnifyMSDK().WGUnbindQQGroupV2(gameGuild);
            return;
        }
    }
    public static void WGQueryQQGroupInfoV2(String groupId){
        if (Router.getInstance().runCppCode()) {
            Router.getInstance().getUnifyMSDK().WGQueryQQGroupInfoV2( groupId);
            return;
        }

    }
    public static void WGBindExistQQGroupV2(GameGuild gameGuild,String groupId,String groupName){
        if (Router.getInstance().runCppCode()) {
            MLog.i("WGBindExistQQGroupV2");
            Router.getInstance().getUnifyMSDK().WGBindExistQQGroupV2(gameGuild,groupId,groupName);
            return;
        }
    }
    public static void WGGetQQGroupCodeV2(GameGuild gameGuild){
        if (Router.getInstance().runCppCode()) {
            MLog.i("WGGetQQGroupCodeV2");
            Router.getInstance().getUnifyMSDK().WGGetQQGroupCodeV2(gameGuild);
            return;
        }
    }
    public static void WGQueryBindGuildV2(String groupId, int type){
        if (Router.getInstance().runCppCode()) {
            MLog.i("WGQueryBindGuildV2");
            Router.getInstance().getUnifyMSDK().WGQueryBindGuildV2(groupId,type);
            return;
        }
    }
    public static void WGGetQQGroupListV2(){
        if (Router.getInstance().runCppCode()) {
            MLog.i("WGGetQQGroupListV2");
            Router.getInstance().getUnifyMSDK().WGGetQQGroupListV2();
            return;
        }
    }
    public static void WGRemindGuildLeaderV2(GameGuild gameGuild){
        if (Router.getInstance().runCppCode()) {
            MLog.i("WGRemindGuildLeaderV2");
            Router.getInstance().getUnifyMSDK().WGRemindGuildLeaderV2(gameGuild);
            return;
        }
    }

    public static void WGOpenFullScreenWebViewWithJson(String jsonStr){
        if (Router.getInstance().runCppCode()) {
            MLog.i("WGOpenFullScreenWebViewWithJson");
            Router.getInstance().getUnifyMSDK().WGOpenFullScreenWebViewWithJson(jsonStr);
            return;
        }
    }

    public static void WGReportPrajna(String serialNumber){
        if (Router.getInstance().runCppCode()) {
            MLog.i("WGReportPrajna");
            Router.getInstance().getUnifyMSDK().WGReportPrajna(serialNumber);
            return;
        }
    }
    public static void WGReportException(eExceptionType exceptionType, String exceptionName, String exceptionMsg,
                                         String exceptionStack, HashMap<String, String>extInfo){
        if (Router.getInstance().runCppCode()) {
            MLog.i("WGReportException");
            Router.getInstance().getUnifyMSDK().WGReportException(exceptionType, exceptionName, exceptionMsg,
                    exceptionStack, extInfo);
            return;
        }
    }
}
