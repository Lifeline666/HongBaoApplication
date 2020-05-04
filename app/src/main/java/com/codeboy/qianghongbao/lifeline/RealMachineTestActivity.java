package com.codeboy.qianghongbao.lifeline;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import com.codeboy.qianghongbao.BaseSettingsActivity;
import com.codeboy.qianghongbao.BaseSettingsFragment;

/**
 * 此类暂时不用
 */
public class RealMachineTestActivity extends BaseSettingsActivity {
    @Override
    public Fragment getSettingsFragment() {
        return new RealMachineTestActivity.RealMachineTestFragment();
    }

    public static class RealMachineTestFragment extends BaseSettingsFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent_p = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                    startActivity(intent_p);

                }
            };


        }
    }


}
