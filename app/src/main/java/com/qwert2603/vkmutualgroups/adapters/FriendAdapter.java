package com.qwert2603.vkmutualgroups.adapters;

import android.app.Activity;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

public class FriendAdapter extends AbstractAdapter<VKApiUserFull> {
    private DataManager mDataManager;
    private PhotoManager mPhotoManager;
    private Activity mActivity;

    public FriendAdapter(Activity activity, VKUsersArray users) {
        super(activity, users);
        mActivity = activity;
        mDataManager = DataManager.get(mActivity);
        mPhotoManager = PhotoManager.get(mActivity);
    }

    @Override
    protected String getTitle(VKApiUserFull item) {
        return mActivity.getString(R.string.friend_name, item.first_name, item.last_name);
    }

    @Override
    protected String getMutualsText(VKApiUserFull item) {
        VKApiCommunityArray groups = mDataManager.getGroupsMutualWithFriend(item.id);
        if (groups != null) {
            return mActivity.getString(R.string.mutual, groups.size());
        } else {
            return "";
        }
    }

    @Override
    protected String getPhotoUrl(VKApiUserFull item) {
        return mPhotoManager.getUserPhotoUrl(item);
    }

}
