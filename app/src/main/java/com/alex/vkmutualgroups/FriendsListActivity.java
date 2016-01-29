package com.alex.vkmutualgroups;

import android.app.Fragment;
import android.os.Bundle;

import com.vk.sdk.api.model.VKApiCommunityFull;

public class FriendsListActivity extends AbstractVkListActivity {

    public static final String EXTRA_GROUP_ID = "com.alex.vkcommonpublics.EXTRA_GROUP_ID";

    private int mGroupId;

    @Override
    protected String getActionBarTitle() {
        VKApiCommunityFull group = DataManager.get(this).getGroupById(mGroupId);
        return (group == null) ? getString(R.string.app_name) : group.name;
    }

    @Override
    protected Fragment getListFragment() {
        return FriendsListFragment.newInstance(mGroupId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mGroupId = getIntent().getIntExtra(EXTRA_GROUP_ID, 0);
        super.onCreate(savedInstanceState);
    }

}
