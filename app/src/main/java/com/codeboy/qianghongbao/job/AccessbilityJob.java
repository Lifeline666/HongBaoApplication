package com.codeboy.qianghongbao.job;

import android.view.accessibility.AccessibilityEvent;

import com.codeboy.qianghongbao.IStatusBarNotification;
import com.codeboy.qianghongbao.QiangHongBaoService;


public interface AccessbilityJob {
    String getTargetPackageName();
    void onCreateJob(QiangHongBaoService service);
    void onReceiveJob(AccessibilityEvent event);
    void onStopJob();
    void onNotificationPosted(IStatusBarNotification service);
    boolean isEnable();
}
