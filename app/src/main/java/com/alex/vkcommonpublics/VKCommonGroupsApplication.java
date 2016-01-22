package com.alex.vkcommonpublics;

import android.app.Application;
import android.util.Log;

import com.vk.sdk.VKSdk;
import com.vk.sdk.util.VKUtil;

public class VKCommonGroupsApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(this);
        for (String s : VKUtil.getCertificateFingerprint(this, "com.alex.vkcommonpublics")) {
            Log.d("CertificateFingerprint", "CertificateFingerprint == " + s);
        }
    }

}