package com.codeboy.qianghongbao;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.job.JobInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

//import androidx.annotation.RequiresApi;

import com.codeboy.qianghongbao.job.AccessbilityJob;
//import com.codeboy.qianghongbao.job.WechatAccessbilityJob;
import com.codeboy.qianghongbao.lifeline.WechatAccessbilityJobCopy;
import com.codeboy.qianghongbao.util.AccessibilityHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class QiangHongBaoService extends AccessibilityService {
    /**
     * 不在聊天列表属性
     */
    private static final int WINDOW_LAUNCHER = 3;
    private static final int WINDOW_NONE = 0;
    private int mCurrentWindow = WINDOW_NONE;
    private boolean isReceivingHongbao;


    private static final String TAG = QiangHongBaoService.class.getSimpleName();

    private static final Class[] ACCESSBILITY_JOBS= {
            WechatAccessbilityJobCopy.class
    };

    private static QiangHongBaoService service;

    private List<AccessbilityJob> mAccessbilityJobs;
    private HashMap<String, AccessbilityJob> mPkgAccessbilityJobMap;

    @Override
    public void onCreate() {
        super.onCreate();

        mAccessbilityJobs = new ArrayList<>();
        mPkgAccessbilityJobMap = new HashMap<>();

        //初始化辅助插件工作
        for(Class clazz : ACCESSBILITY_JOBS) {
            try {
                Object object = clazz.newInstance();
                if(object instanceof AccessbilityJob) {
                    AccessbilityJob job = (AccessbilityJob) object;
                    job.onCreateJob(this);
                    mAccessbilityJobs.add(job);
                    mPkgAccessbilityJobMap.put(job.getTargetPackageName(), job);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "qianghongbao service destory");
        if(mPkgAccessbilityJobMap != null) {
            mPkgAccessbilityJobMap.clear();
        }
        if(mAccessbilityJobs != null && !mAccessbilityJobs.isEmpty()) {
            for (AccessbilityJob job : mAccessbilityJobs) {
                job.onStopJob();
            }
            mAccessbilityJobs.clear();
        }

        service = null;
        mAccessbilityJobs = null;
        mPkgAccessbilityJobMap = null;
        //发送广播，已经断开辅助服务
        Intent intent = new Intent(Config.ACTION_QIANGHONGBAO_SERVICE_DISCONNECT);
        sendBroadcast(intent);
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "qianghongbao service interrupt");
        Toast.makeText(this, "中断抢红包服务", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        service = this;
        //发送广播，已经连接上了
        Intent intent = new Intent(Config.ACTION_QIANGHONGBAO_SERVICE_CONNECT);
        sendBroadcast(intent);
        Toast.makeText(this, "已连接抢红包服务", Toast.LENGTH_SHORT).show();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if(BuildConfig.DEBUG) {
            Log.d(TAG, "事件--->" + event );
        }
        String pkn = String.valueOf(event.getPackageName());
        if(mAccessbilityJobs != null && !mAccessbilityJobs.isEmpty()) {
            if(!getConfig().isAgreement()) {
                return;
            }
            for (AccessbilityJob job : mAccessbilityJobs) {
                Log.d(TAG, "mAccessbilityJobs集合单个对象包路径信息--->" +job.getTargetPackageName());
                Log.d(TAG, "mAccessbilityJobs集合单个对象是否开启-->" +job.isEnable());
                if(pkn.equals(job.getTargetPackageName()) && job.isEnable()) {
                    Log.d(TAG, "马上进入onReceive操作" );
/**
 * 这里找节点
 */
                    Context applicationContext = getApplicationContext();
                    Log.i(TAG, "此时上下文内容applicationContext" +applicationContext);
                    AccessibilityNodeInfo window = getRootInActiveWindow();
                    Log.i(TAG, "此时窗口的内容" +window);


                    if(job instanceof WechatAccessbilityJobCopy){
                        Log.d(TAG, "是weCopy就执行" );
                        job.onReceiveJob(event);
                    }
//                    else{
//                        Log.d(TAG, "不是强转也要执行" );
//                        ((WechatAccessbilityJobCopy) job).onReceiveJob(event);
//                    }


                }
            }
        }
    }
//    // 通过文本找到当前的节点
//    List<AccessibilityNodeInfo> nodes = getRootInActiveWindow().findAccessibilityNodeInfosByText(text);
//        if(nodes != null) {
//        for (AccessibilityNodeInfo node : nodes) {
//            if (node.getClassName().equals(widget) && node.isEnabled()) {
//                node.performAction(AccessibilityNodeInfo.ACTION_CLICK); // 执行点击
//                break;
//            }
//        }
//    }


//    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
//    clickLuckyMoney(rootNode);
//
//    {
//        if(rootNode != null) {
//            int count = rootNode.getChildCount();
//            for (int i = count - 1; i >= 0; i--) {  // 倒序查找最新的红包
//                AccessibilityNodeInfo node = rootNode.getChild(i);
//                if (node == null)
//                    continue;
//
//                CharSequence text = node.getText();
//                if (text != null && text.toString().equals("领取红包")) {
//                    AccessibilityNodeInfo parent = node.getParent();
//                    while (parent != null) {
//                        if (parent.isClickable()) {
//                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                            break;
//                        }
//                        parent = parent.getParent();
//                    }
//                }
//
//                clickLuckyMoney(node);
//            }
//        }
//    }























    public Config getConfig() {
        return Config.getConfig(this);
    }

    /** 接收通知栏事件*/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void handeNotificationPosted(IStatusBarNotification notificationService) {
        if(notificationService == null) {
            return;
        }
        if(service == null || service.mPkgAccessbilityJobMap == null) {
            return;
        }
        String pack = notificationService.getPackageName();
        AccessbilityJob job = service.mPkgAccessbilityJobMap.get(pack);
        if(job == null) {
            return;
        }
        job.onNotificationPosted(notificationService);
    }

    /**
     * 判断当前服务是否正在运行
     * */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean isRunning() {
        if(service == null) {
            return false;
        }
        AccessibilityManager accessibilityManager = (AccessibilityManager) service.getSystemService(Context.ACCESSIBILITY_SERVICE);
        AccessibilityServiceInfo info = service.getServiceInfo();
        if(info == null) {
            return false;
        }
        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        Iterator<AccessibilityServiceInfo> iterator = list.iterator();

        boolean isConnect = false;
        while (iterator.hasNext()) {
            AccessibilityServiceInfo i = iterator.next();
            if(i.getId().equals(info.getId())) {
                isConnect = true;
                break;
            }
        }
        if(!isConnect) {
            return false;
        }
        return true;
    }

    /** 快速读取通知栏服务是否启动*/
    public static boolean isNotificationServiceRunning() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return false;
        }
        //部份手机没有NotificationService服务
        try {
            return QHBNotificationService.isRunning();
        } catch (Throwable t) {}
        return false;
    }


}
