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

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.activities.vk_list_activities.AbstractVkListActivity;
import com.qwert2603.vkmutualgroups.activities.vk_list_activities.FriendsInGroupListActivity;
import com.qwert2603.vkmutualgroups.adapters.AbstractAdapter;
import com.qwert2603.vkmutualgroups.adapters.GroupAdapter;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.qwert2603.vkmutualgroups.util.VKApiCommunityArray_Fix;
import com.vk.sdk.api.model.VKApiCommunityFull;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;

/**
 * Отображает список групп, переданный в {@link #newInstance(VKApiCommunityArray_Fix, String)}
 */
public class GroupsListFragment extends AbstractVkListFragment<VKApiCommunityFull> {

    @SuppressWarnings("unused")
    private static final String TAG = "GroupsListFragment";

    private static final String groupsKey = "groupsKey";
    private static final String emptyListTextKey = "emptyListTextKey";

    public static GroupsListFragment newInstance(VKApiCommunityArray_Fix groups, String emptyListText) {
        GroupsListFragment result = new GroupsListFragment();
        Bundle args = new Bundle();
        args.putParcelable(groupsKey, groups);
        args.putString(emptyListTextKey, emptyListText);
        result.setArguments(args);
        return result;
    }

    private DataManager mDataManager;
    private PhotoManager mPhotoManager;

    private VKApiCommunityArray_Fix mGroups;

    private GroupAdapter mGroupsAdapter;

    @Override
    protected String getEmptyListText() {
        return getArguments().getString(emptyListTextKey);
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
    protected AbstractAdapter<VKApiCommunityFull> getListAdapter() {
        return mGroupsAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mDataManager = DataManager.get(getActivity());
        mPhotoManager = PhotoManager.get(getActivity());

//        mGroups = new VKApiCommunityArray();
//        Parcelable parcelable = getArguments().getParcelable(groupsKey);
//        if (parcelable != null) {
//            for (Object group : ((VKList) parcelable)) {
//                mGroups.add((VKApiCommunityFull) group);
//            }
//        }

        mGroups = getArguments().getParcelable(groupsKey);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setListViewOnItemClickListener((parent, view1, position, id) -> {
            if (mDataManager.getFetchingState() == finished) {
                VKApiCommunityFull group = mGroups.get(position);
                if (mDataManager.getUsersGroupById(group.id) != null) {
                    Intent intent = new Intent(getActivity(), FriendsInGroupListActivity.class);
                    intent.putExtra(FriendsInGroupListActivity.EXTRA_GROUP, group);
                    startActivity(intent);
                }
            }
        });
        setListViewChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        setListViewMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            private int mActionedPosition;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // чтобы выделялось не более 1 группы.
                if (getListViewCheckedItemCount() > 1) {
                    mode.finish();
                } else {
                    mActionedPosition = position;

                    if (mDataManager.getUsersGroupById(mGroups.get(mActionedPosition).id) != null) {
                        mode.getMenu().findItem(R.id.menu_join_group).setVisible(false);
                    } else {
                        mode.getMenu().findItem(R.id.menu_leave_group).setVisible(false);
                    }
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                if (mDataManager.getFetchingState() != finished) {
                    mode.finish();
                    return false;
                }
                mode.getMenuInflater().inflate(R.menu.one_group, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                VKApiCommunityFull group = mGroups.get(mActionedPosition);
                switch (item.getItemId()) {
                    case R.id.menu_open_in_browser:
                        ((AbstractVkListActivity) getActivity()).navigateTo("http://vk.com/" + group.screen_name);
                        return true;
                    case R.id.menu_leave_group:
                        ((AbstractVkListActivity) getActivity()).leaveGroup(group);
                        mode.finish();
                        return true;
                    case R.id.menu_join_group:
                        ((AbstractVkListActivity) getActivity()).joinGroup(group);
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
        setListViewAdapter(mGroupsAdapter);

        return view;
    }

}
