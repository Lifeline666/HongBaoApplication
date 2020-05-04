package com.codeboy.qianghongbao;

import android.os.Bundle;
//import android.support.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
/*    protected void onCreate(@Nullable Bundle savedInstanceState) {*/
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QHBApplication.activityCreateStatistics(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        QHBApplication.activityResumeStatistics(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        QHBApplication.activityPauseStatistics(this);
    }
}
