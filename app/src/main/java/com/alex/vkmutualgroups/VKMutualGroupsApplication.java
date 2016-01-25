package com.alex.vkmutualgroups;

import android.app.Application;
import android.util.Log;

import com.vk.sdk.VKSdk;
import com.vk.sdk.util.VKUtil;

public class VKMutualGroupsApplication extends Application {
    
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(this);
        for (String s : VKUtil.getCertificateFingerprint(this, "com.alex.vkcommonpublics")) {
            Log.d("CertificateFingerprint", "CertificateFingerprint == " + s);
        }
    }

}