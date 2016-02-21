package com.qwert2603.vkmutualgroups.activities.vk_list_activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.fragments.GroupsListFragment;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiUserFull;

/**
 * Группы, общие с другом.
 */
public class MutualGroupsListActivity extends GroupsListActivity {

    private VKApiUserFull mFriend;

    private DataManager mDataManager;

    private DataManager.DataLoadingListener mDataLoadingListener = new DataManager.DataLoadingListener() {
        @Override
        public void onLoadingStarted() {
            updateActionButtonVisibility();
        }

        @Override
        public void onCompleted(Void v) {
            updateActionButtonVisibility();
        }

        @Override
        public void onError(String e) {
            Log.e(TAG, e);
            updateActionButtonVisibility();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFriend = getIntent().getParcelableExtra(EXTRA_FRIEND);

        mDataManager = DataManager.get(this);
        mDataManager.addDataLoadingListener(mDataLoadingListener);

        setErrorTextViewVisibility(View.INVISIBLE);
        setRefreshLayoutEnable(false);

        setActionButtonIcon(R.drawable.message);
        setActionButtonOnClickListener((v) -> sendMessage(mFriend.id));

        VKApiCommunityArray groups;
        if (mFriend.id != 0) {
            groups = mDataManager.getGroupsMutualWithFriend(mFriend.id);
        } else {
            groups = mDataManager.getUsersGroups();
        }

        if (groups == null) {
            groups = new VKApiCommunityArray();
        }
        setListFragment(GroupsListFragment.newInstance(groups, getString(R.string.no_mutual_groups)));
    }

    @Override
    protected void onDestroy() {
        mDataManager.removeDataLoadingListener(mDataLoadingListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateActionButtonVisibility();
    }

    @Override
    protected void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        updateActionButtonVisibility();
    }

    private void updateActionButtonVisibility() {
        boolean b = (mDataManager.getFetchingState() == DataManager.FetchingState.finished)
                && (mDataManager.getUsersFriendById(mFriend.id) != null);
        setActionButtonVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }

}

