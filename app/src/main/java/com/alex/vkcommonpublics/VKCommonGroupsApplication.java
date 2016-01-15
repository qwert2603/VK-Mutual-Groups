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
    }

}