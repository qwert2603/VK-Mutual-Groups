package com.qwert2603.vkmutualgroups.adapters;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.qwert2603.vkmutualgroups.R;
import com.qwert2603.vkmutualgroups.data.DataManager;
import com.qwert2603.vkmutualgroups.photo.ImageViewHolder;
import com.qwert2603.vkmutualgroups.photo.PhotoManager;
import com.vk.sdk.api.model.VKApiCommunityArray;
import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKUsersArray;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.calculatingMutual;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;

public class GroupAdapter extends ArrayAdapter<VKApiCommunityFull> {
    private DataManager mDataManager;
    private PhotoManager mPhotoManager;
    private Activity mActivity;

    public GroupAdapter(Activity activity, VKApiCommunityArray groups) {
        super(activity, 0, groups);
        mActivity = activity;
        mDataManager = DataManager.get(mActivity);
        mPhotoManager = PhotoManager.get(mActivity);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mActivity.getLayoutInflater().inflate(R.layout.list_item, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.mPhotoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
            viewHolder.mTitleTextView = (TextView) convertView.findViewById(R.id.item_title);
            viewHolder.mMutualsTextView = (TextView) convertView.findViewById(R.id.common_count);
            convertView.setTag(viewHolder);
        }

        VKApiCommunityFull group = getItem(position);
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.mPosition = position;
        if (mPhotoManager.getPhoto(mPhotoManager.getGroupPhotoUrl(group)) != null) {
            viewHolder.mPhotoImageView.setImageBitmap(mPhotoManager.getPhoto(mPhotoManager.getGroupPhotoUrl(group)));
        }
        else {
            viewHolder.mPhotoImageView.setImageBitmap(null);
            //mPhotoManager.setPhotoToImageViewHolder(viewHolder, getPhotoURL(position));
        }

        viewHolder.mTitleTextView.setText(group.name);

        if (mDataManager.getFetchingState() == calculatingMutual || mDataManager.getFetchingState() == finished) {
            VKUsersArray friends = mDataManager.getFriendsInGroup(group.id);
            if (friends != null) {
                viewHolder.mMutualsTextView.setText(mActivity.getString(R.string.friends, friends.size()));
            } else {
                viewHolder.mMutualsTextView.setText("");
            }
        } else {
            viewHolder.mMutualsTextView.setText("");
        }

        return convertView;
    }

    private static class ViewHolder implements ImageViewHolder {
        int mPosition;
        ImageView mPhotoImageView;
        TextView mTitleTextView;
        TextView mMutualsTextView;

        @Override
        public int getPosition() {
            return mPosition;
        }

        @Override
        public ImageView getImageView() {
            return mPhotoImageView;
        }
    }

}
