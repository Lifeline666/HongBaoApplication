package com.codeboy.qianghongbao.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class WeChatHelper {
    private WeChatHelper() {

    }

    private static final WeChatHelper instance = new WeChatHelper();

    public static WeChatHelper init() {
        return instance;
    }

    /**打开微信主界面*/
    public static void openWechat(Context context) {
        try {
            Intent intent = new Intent();
           // ComponentName cmp = new ComponentName(WechatUI.WECHAT_PACKAGE_NAME, WechatUI.UI_LUANCHER);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
           // intent.setComponent(cmp);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取微信的版本
     */
    public static int getWechatVersion(Context context) {
        PackageInfo mWechatPackageInfo = getPackageInfo(context);
        if (mWechatPackageInfo == null) {
            return 0;
        }
        return mWechatPackageInfo.versionCode;
    }

    /**
     * 获取微信的版本名称
     */
    public static String getWechatVersionName(Context context) {
        PackageInfo mWechatPackageInfo = getPackageInfo(context);
        if (mWechatPackageInfo == null) {
            return "";
        }
        return mWechatPackageInfo.versionName;
    }


//    /**微信7.0.0以上*/
//    public static boolean above700() {
//        return getWechatVersion(LibInstance.getInstance().getTaskListener().provideContext()) >= 1380;
//    }
//
//    /**微信6.7.3以上*/
//    public static boolean above673() {
//        return getWechatVersion(LibInstance.getInstance().getTaskListener().provideContext()) >= 1360;
//    }
    /**
     * 更新微信包信息
     */
    private static PackageInfo getPackageInfo(Context context) {
        PackageInfo mWechatPackageInfo = null;
        try {
           // mWechatPackageInfo = context.getPackageManager().getPackageInfo(WechatUI.WECHAT_PACKAGE_NAME, 0);
       // } catch (PackageManager.NameNotFoundException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mWechatPackageInfo;
    }
}
