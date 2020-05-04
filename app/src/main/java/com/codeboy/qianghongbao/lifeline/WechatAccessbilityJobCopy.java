package com.codeboy.qianghongbao.lifeline;


import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

//import androidx.annotation.RequiresApi;
import com.codeboy.qianghongbao.BuildConfig;
import com.codeboy.qianghongbao.Config;
import com.codeboy.qianghongbao.IStatusBarNotification;
import com.codeboy.qianghongbao.QHBApplication;
import com.codeboy.qianghongbao.QiangHongBaoService;
import com.codeboy.qianghongbao.job.BaseAccessbilityJob;
import com.codeboy.qianghongbao.util.AccessibilityHelper;
import com.codeboy.qianghongbao.util.NotifyHelper;

import java.util.List;


public class WechatAccessbilityJobCopy extends BaseAccessbilityJob {

    private static final String TAG = "WechatAccessbilityJob";

    /** 微信的包名*/
    public static final String WECHAT_PACKAGENAME = "com.tencent.mm";

    /** 红包消息的关键字*/
    private static final String HONGBAO_TEXT_KEY = "[微信红包]";

    private static final String BUTTON_CLASS_NAME = "android.widget.Button";


    /** 不能再使用文字匹配的最小版本号 */
    private static final int USE_ID_MIN_VERSION = 700;// 6.3.8 对应code为680,6.3.9对应code为700

    private static final int WINDOW_NONE = 0;
    private static final int WINDOW_LUCKYMONEY_RECEIVEUI = 1;
    private static final int WINDOW_LUCKYMONEY_DETAIL = 2;
    private static final int WINDOW_LAUNCHER = 3;
    private static final int WINDOW_OTHER = -1;//与人聊天界面

    private int mCurrentWindow = WINDOW_NONE;

    private boolean isReceivingHongbao;
    private PackageInfo mWechatPackageInfo = null;
    private Handler mHandler = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //更新安装包信息
            updatePackageInfo();
        }
    };

    @Override
    public void onCreateJob(QiangHongBaoService service) {
        super.onCreateJob(service);

        updatePackageInfo();

        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");

        getContext().registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onStopJob() {
        try {
            getContext().unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {}
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onNotificationPosted(IStatusBarNotification sbn) {
        Notification nf = sbn.getNotification();
        String text = String.valueOf(sbn.getNotification().tickerText);
        notificationEvent(text, nf);
    }

    @Override
    public boolean isEnable() {
        return getConfig().isEnableWechat();
    }

    @Override
    public String getTargetPackageName() {
        return WECHAT_PACKAGENAME;
    }

    //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceiveJob(AccessibilityEvent event) {
        //Log.d(TAG,"event接收事件情况"+event);
        final int eventType = event.getEventType();
        Log.i(TAG,"发起的event事件数字为"+eventType);
//        通知栏事件 sdk18 之前支持AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
//        最新的sdk 要获取通知栏信息要继承NotificationListenerService类
        if(eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Parcelable data = event.getParcelableData();
            if(data == null || !(data instanceof Notification)) {
                return;
            }
            if(QiangHongBaoService.isNotificationServiceRunning() && getConfig().isEnableNotificationService()) { //开启快速模式，不处理
                return;
            }
            List<CharSequence> texts = event.getText();
            if(!texts.isEmpty()) {
                String text = String.valueOf(texts.get(0));
                notificationEvent(text, (Notification) data);
            }
        //} else if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
        } else if(eventType == 32) {
            Log.d(TAG,"在窗口状态"+AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            openHongBao(event);
     //   } else if(eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
        } else if(eventType == 2048) {
            Log.d(TAG,"当前所在位置"+mCurrentWindow);
            Log.d(TAG,"是否有红包"+isReceivingHongbao);
            if(mCurrentWindow != WINDOW_LAUNCHER) { //不在聊天界面或聊天列表，不处理 3.是列表页面
                return;
            }
            if(isReceivingHongbao) {
                handleChatListHongBao();
            }
        }else if(eventType==1){
            Log.d(TAG,"状态为1直接点击");
            handleChatListHongBao();
        }
    }

    /** 是否为群聊天*/
    private boolean isMemberChatUi(AccessibilityNodeInfo nodeInfo) {
        if(nodeInfo == null) {
            return false;
        }
        // String id = "com.tencent.mm:id/ces";
        String id = "com.tencent.mm:id/cw7";
        int wv = getWechatVersion();
        Log.i(TAG,"当前版本号为:"+String.valueOf(wv));
        if(wv <= 680) {
            id = "com.tencent.mm:id/ew";
        } else if(wv <= 700) {
            id = "com.tencent.mm:id/cbo";
        }
        String title = null;
        AccessibilityNodeInfo target = AccessibilityHelper.findNodeInfosById(nodeInfo, id);
        if(target != null) {
            title = String.valueOf(target.getText());
        }
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("返回");

        if(list != null && !list.isEmpty()) {
            AccessibilityNodeInfo parent = null;
            for(AccessibilityNodeInfo node : list) {
                if(!"android.widget.ImageView".equals(node.getClassName())) {
                    continue;
                }
                String desc = String.valueOf(node.getContentDescription());
                if(!"返回".equals(desc)) {
                    continue;
                }
                parent = node.getParent();
                break;
            }
            if(parent != null) {
                parent = parent.getParent();
            }
            if(parent != null) {
                if( parent.getChildCount() >= 2) {
                    AccessibilityNodeInfo node = parent.getChild(1);
                    if("android.widget.TextView".equals(node.getClassName())) {
                        title = String.valueOf(node.getText());
                    }
                }
            }
        }


        if(title != null && title.endsWith(")")) {
            return true;
        }
        return false;
    }

    /** 通知栏事件*/
    private void notificationEvent(String ticker, Notification nf) {
        String text = ticker;
        int index = text.indexOf(":");
        if(index != -1) {
            text = text.substring(index + 1);
        }
        text = text.trim();
        if(text.contains(HONGBAO_TEXT_KEY)) { //红包消息
            newHongBaoNotification(nf);
        }
    }

    /** 打开通知栏消息*/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void newHongBaoNotification(Notification notification) {
        isReceivingHongbao = true;
        //以下是精华，将微信的通知栏消息打开
        PendingIntent pendingIntent = notification.contentIntent;
        boolean lock = NotifyHelper.isLockScreen(getContext());

        if(!lock) {
            NotifyHelper.send(pendingIntent);
        } else {
            NotifyHelper.showNotify(getContext(), String.valueOf(notification.tickerText), pendingIntent);
        }

        if(lock || getConfig().getWechatMode() != Config.WX_MODE_0) {
            NotifyHelper.playEffect(getContext(), getConfig());
        }
    }

   // @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openHongBao(AccessibilityEvent event) {
        Log.w(TAG, "开红包界面event.getclassName"+event.getClassName());
        //if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {
          if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI".equals(event.getClassName())) {
              Log.i(TAG, "点中了红包，下一步就是去拆红包,当前窗口状态改变前"+mCurrentWindow);
            mCurrentWindow = WINDOW_LUCKYMONEY_RECEIVEUI;
              Log.i(TAG, "点中了红包，下一步就是去拆红包,当前窗口状态改变后"+mCurrentWindow);
            //点中了红包，下一步就是去拆红包
            handleLuckyMoneyReceive();
        } else if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LUCKYMONEY_DETAIL;
            //拆完红包后看详细的纪录界面
            if(getConfig().getWechatAfterGetHongBaoEvent() == Config.WX_AFTER_GET_GOHOME) { //返回主界面，以便收到下一次的红包通知
                AccessibilityHelper.performHome(getService());
            }



//        } else if("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
//            mCurrentWindow = WINDOW_LAUNCHER;
//            //在聊天界面,去点中红包
//            handleChatListHongBao();
        } else if("android.widget.LinearLayout".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LAUNCHER;
            //在聊天界面,去点中红包
            handleChatListHongBao();
        } else {
            mCurrentWindow = WINDOW_OTHER;
        }
    }

    /**
     * 点击聊天里的红包后，显示的界面
     * */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void handleLuckyMoneyReceive() {
        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        Log.i(TAG, "聊天里点击红包后的nodeInfo "+nodeInfo);
        if(nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }

        AccessibilityNodeInfo targetNode = null;
        int event = getConfig().getWechatAfterOpenHongBaoEvent();
        Log.i(TAG, "微信打开红包后的事件Code"+event);
        int wechatVersion = getWechatVersion();
        Log.i(TAG, "当前微信的版本"+wechatVersion);
      //  Log.i(TAG, "当前微信的版本"+wechatVersion);
        if(event == Config.WX_AFTER_OPEN_HONGBAO) { //拆红包
            if (wechatVersion < USE_ID_MIN_VERSION) {
                 targetNode = AccessibilityHelper.findNodeInfosByText(nodeInfo, "拆红包");
               // targetNode = AccessibilityHelper.findNodeInfosByText(nodeInfo, "开");
            } else {
                String buttonId = "com.tencent.mm:id/dbr";

                if(wechatVersion == 700) {
                    buttonId = "com.tencent.mm:id/b2c";
                }

                if(buttonId != null) {
                    targetNode = AccessibilityHelper.findNodeInfosById(nodeInfo, buttonId);
                }

                if(targetNode == null) {
                    //分别对应固定金额的红包 拼手气红包
                    AccessibilityNodeInfo textNode = AccessibilityHelper.findNodeInfosByTexts(nodeInfo, "发了一个红包", "给你发了一个红包", "发了一个红包，金额随机");

                    if(textNode != null) {
                        for (int i = 0; i < textNode.getChildCount(); i++) {
                            AccessibilityNodeInfo node = textNode.getChild(i);
                            if (BUTTON_CLASS_NAME.equals(node.getClassName())) {
                                targetNode = node;
                                break;
                            }
                        }
                    }
                }

                if(targetNode == null) { //通过组件查找
                    targetNode = AccessibilityHelper.findNodeInfosByClassName(nodeInfo, BUTTON_CLASS_NAME);
                }
            }
        } else if(event == Config.WX_AFTER_OPEN_SEE) { //看一看
            if(getWechatVersion() < USE_ID_MIN_VERSION) { //低版本才有 看大家手气的功能
                targetNode = AccessibilityHelper.findNodeInfosByText(nodeInfo, "看看大家的手气");
            }
        } else if(event == Config.WX_AFTER_OPEN_NONE) {
            return;
        }

        if(targetNode != null) {
            Log.i(TAG, "targetNode的信息"+targetNode);
            final AccessibilityNodeInfo n = targetNode;
            long sDelayTime = getConfig().getWechatOpenDelayTime();
            Log.i(TAG, "拆红包延迟时间"+sDelayTime);
            if(sDelayTime != 0) {
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AccessibilityHelper.performClick(n);
                    }
                }, sDelayTime);
            } else {
                AccessibilityHelper.performClick(n);
            }
            Log.i(TAG, "开红包后eventCode为"+event);
            if(event == Config.WX_AFTER_OPEN_HONGBAO) {
                QHBApplication.eventStatistics(getContext(), "open_hongbao");
            } else {
                QHBApplication.eventStatistics(getContext(), "open_see");
            }
        }
    }

    /**
     * 收到聊天里的红包
     * */
   // @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void handleChatListHongBao() {
        Log.i(TAG,"进入子方法执行");
        int mode = getConfig().getWechatMode();
        Log.i(TAG,"mode值为"+mode);
        if(mode == Config.WX_MODE_3) { //只通知模式
            return;
        }


        getService().getRootInActiveWindow();

        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        Log.w(TAG, "nodeInfo信息>>>>>>>>>>>>>"+nodeInfo);
        QiangHongBaoService service = getService();
        List<AccessibilityWindowInfo> windows = service.getWindows();
        Log.w(TAG, "集合的信息>>>>>>>>>>>>>"+windows);


        if(nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }

        if(mode != Config.WX_MODE_0) {
            boolean isMember = isMemberChatUi(nodeInfo);
            if(mode == Config.WX_MODE_1 && isMember) {//过滤群聊
                return;
            } else if(mode == Config.WX_MODE_2 && !isMember) { //过滤单聊
                return;
            }
        }

        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("微信红包");
        //List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("[微信红包]");
        Log.i(TAG, "节点信息>>>>>>>>>>>>>"+list.toString());
        AccessibilityHelper.performClick(nodeInfo);
        if(list != null && list.isEmpty()) {
            // 从消息列表查找红包
            AccessibilityNodeInfo node = AccessibilityHelper.findNodeInfosByText(nodeInfo, "[微信红包]");
            Log.w(TAG, "list不为null 不为空情况node 信息>>>>>>>>>>>>>"+node);
            if(node != null) {
                Log.i(TAG, "又一次[微信红包]查找情况>>>>>>>>>>>>>");
                if(BuildConfig.DEBUG) {
                    Log.i(TAG, "-->微信红包:" + node);
                }
                isReceivingHongbao = true;
                Log.w(TAG, "开始执行点击红包事件>>>>>>>>>>>>>");
                AccessibilityHelper.performClick(nodeInfo);
            }
        } else if(list != null) {
            Log.w(TAG, "领取红包不为空时>>>>>>>>>>>>>");
            if (isReceivingHongbao){
                //最新的红包领起
                AccessibilityNodeInfo node = list.get(list.size() - 1);
                AccessibilityHelper.performClick(node);
                isReceivingHongbao = false;
            }
        }else{
            Log.w(TAG, "领取红包信息没有>>>>>>>>>>>>>"+list.toString());
        }
    }

    private Handler getHandler() {
        if(mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    /** 获取微信的版本*/
    private int getWechatVersion() {
        Log.i(TAG, "当前微信包信息拿版本用"+mWechatPackageInfo);
        if(mWechatPackageInfo == null) {
            return 0;
        }
        return mWechatPackageInfo.versionCode;
    }

    /**
     * 获取应用版本号
     * @param context
     * @return
     */
    public static int getVersionCode(Context context) throws Exception {
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        return info.versionCode;
    }



    /** 更新微信包信息*/
    //想要监听那款软件，也可以配置在配置文件中
    private void updatePackageInfo() {
        try {
            mWechatPackageInfo = getContext().getPackageManager().getPackageInfo(WECHAT_PACKAGENAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
