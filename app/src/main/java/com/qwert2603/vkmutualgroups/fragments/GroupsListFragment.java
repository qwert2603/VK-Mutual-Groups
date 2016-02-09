package com.qwert2603.vkmutualgroups.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.activities.BaseVkActivity;
import com.qwert2603.vkmutualgroups.activities.FriendsInGroupListActivity;
import com.qwert2603.vkmutualgroups.adapters.GroupAdapter;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiCommunityFull;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;

/**
 * Отображает список общих групп, переданный в {@link #newInstance(int, VKApiCommunityArray)}
 */
public class GroupsListFragment extends AbstractVkListFragment<VKApiCommunityFull> {

    @SuppressWarnings("unused")
    private static final String TAG = "GroupsListFragment";

    private static final String friendIdKey = "friendIdKey";
    private static final String groupsKey = "groupsKey";

    /**
     * friendId - id друга. В списке будут выведены группы, общие с этим другом.
     * Если friendId == 0, будут выведены группы пользователя в текущем порядке сортировки из mDataManager.
     */
    public static GroupsListFragment newInstance(int friendId, VKApiCommunityArray groups) {
        GroupsListFragment result = new GroupsListFragment();
        Bundle args = new Bundle();
        args.putInt(friendIdKey, friendId);
        args.putParcelable(groupsKey, groups);
        result.setArguments(args);
        return result;
    }

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    private VKApiCommunityArray mGroups;
    private int mFriendId;

    private GroupAdapter mGroupsAdapter;

    @Override
    protected String getEmptyListText() {
        return getString(R.string.no_groups);
    }

    @Override
    protected int getItemsCount() {
        return mGroups.size();
    }

    @Override
    protected String getPhotoURL(int index) {
        return mPhotoManager.getGroupPhotoUrl(mGroups.get(index));
    }

    @Override
    protected ArrayAdapter<VKApiCommunityFull> getListAdapter() {
        return mGroupsAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mDataManager = DataManager.get(getActivity());
        mPhotoManager = PhotoManager.get(getActivity());

        mFriendId = getArguments().getInt(friendIdKey);
        mGroups = getArguments().getParcelable(groupsKey);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mListView.setOnItemClickListener((parent, view1, position, id) -> {
            if (mDataManager.getFetchingState() == finished) {
                VKApiCommunityFull group = (VKApiCommunityFull) mListView.getAdapter().getItem(position);
                if (mDataManager.getUsersGroupById(group.id) != null) {
                    Intent intent = new Intent(getActivity(), FriendsInGroupListActivity.class);
                    intent.putExtra(FriendsInGroupListActivity.EXTRA_GROUP_ID, group.id);
                    startActivity(intent);
                }
            }
        });
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            private int mActionedPosition;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // чтобы выделялось не более 1 группы.
                // И чтобы не выделялись те группы, в которых пользователь не состоит.
                if (mListView.getCheckedItemCount() > 1 || mDataManager.getUsersGroupById(mGroups.get(position).id) == null) {
                    mode.finish();
                } else {
                    mActionedPosition = position;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                if (mDataManager.getFetchingState() != finished) {
                    mode.finish();
                    return false;
                }
                mode.getMenuInflater().inflate(R.menu.groups_list_item_action_mode, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                int groupId = mGroups.get(mActionedPosition).id;
                switch (item.getItemId()) {
                    case R.id.menu_leave_group:
                        ((BaseVkActivity) getActivity()).leaveGroup(groupId);
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });

        mGroupsAdapter = new GroupAdapter(getActivity(), mGroups);
        mListView.setAdapter(mGroupsAdapter);

        mActionButton.setIcon(R.drawable.message);
        mActionButton.setOnClickListener((v) -> ((BaseVkActivity) getActivity()).sendMessage(mFriendId));

        return view;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        boolean b = mFriendId != 0 && mDataManager.getUsersFriendById(mFriendId) != null && mDataManager.getFetchingState() == finished;
        mActionButton.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }

}
