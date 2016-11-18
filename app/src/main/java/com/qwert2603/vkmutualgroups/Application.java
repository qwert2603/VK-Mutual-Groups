package com.qwert2603.vkmutualgroups;

import android.util.Log;

import com.vk.sdk.VKSdk;
import com.vk.sdk.util.VKUtil;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(this);
        for (String s : VKUtil.getCertificateFingerprint(this, this.getPackageName())) {
            Log.d("CertificateFingerprint", "CertificateFingerprint == " + s);
        }
        //InternalStorageViewer.print(this);
    }

}