package com.qwert2603.vkmutualgroups.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.qwert2603.vkmutualgroups.activities.vk_list_activities.LoadingFriendsListActivity;
import com.vk.sdk.VKSdk;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent;
        if (VKSdk.isLoggedIn()) {
            intent = new Intent(this, LoadingFriendsListActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }

}
