package com.qwert2603.vkmutualgroups.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.activities.vk_list_activities.LoadingListActivity;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

public class LoginActivity extends AppCompatActivity {

    public static final String[] LOGIN_SCOPE = new String[] { VKScope.FRIENDS, VKScope.GROUPS, VKScope.MESSAGES };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VKSdk.login(this, LOGIN_SCOPE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (! VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                startActivity(new Intent(LoginActivity.this, LoadingListActivity.class));
                finish();
            }

            @Override
            public void onError(VKError error) {
                Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                VKSdk.login(LoginActivity.this, LOGIN_SCOPE);
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
