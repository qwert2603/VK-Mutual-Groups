package com.qwert2603.vkmutualgroups.adapters;

import android.app.Activity;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.qwert2603.vkmutualgroups.util.VKApiCommunityArray_Fix;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKUsersArray;

public class GroupAdapter extends AbstractAdapter<VKApiCommunityFull> {
    private DataManager mDataManager;
    private PhotoManager mPhotoManager;
    private Activity mActivity;

    public GroupAdapter(Activity activity, VKApiCommunityArray_Fix groups) {
        super(activity, groups);
        mActivity = activity;
        mDataManager = DataManager.get(mActivity);
        mPhotoManager = PhotoManager.get(mActivity);
    }

    @Override
    protected String getTitle(VKApiCommunityFull item) {
        return item.name;
    }

    @Override
    protected String getMutualsText(VKApiCommunityFull item) {
        VKUsersArray friends = mDataManager.getFriendsInGroup(item.id);
        if (friends != null) {
            return mActivity.getString(R.string.friends, friends.size());
        } else {
            return "";
        }
    }

    @Override
    protected String getPhotoUrl(VKApiCommunityFull item) {
        return mPhotoManager.getGroupPhotoUrl(item);
    }

}
