package com.alex.vkcommonpublics;

import android.app.Application;

import com.vk.sdk.VKSdk;

public class VKCommonGroupsApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        VKSdk.initialize(this);
    }

}