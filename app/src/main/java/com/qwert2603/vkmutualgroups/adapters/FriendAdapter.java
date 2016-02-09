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
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.calculatingMutual;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;

public class FriendAdapter extends ArrayAdapter<VKApiUserFull> {
    private DataManager mDataManager;
    private PhotoManager mPhotoManager;
    private Activity mActivity;

    public FriendAdapter(Activity activity, VKUsersArray users) {
        super(activity, 0, users);
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

        VKApiUserFull friend = getItem(position);
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.mPosition = position;
        if (mPhotoManager.getPhoto(mPhotoManager.getUserPhotoUrl(friend)) != null) {
            viewHolder.mPhotoImageView.setImageBitmap(mPhotoManager.getPhoto(mPhotoManager.getUserPhotoUrl(friend)));
        }
        else {
            viewHolder.mPhotoImageView.setImageBitmap(null);
            //mPhotoManager.setPhotoToImageViewHolder(viewHolder, getPhotoURL(position));
        }

        viewHolder.mTitleTextView.setText(mActivity.getString(R.string.friend_name, friend.first_name, friend.last_name));

        if (mDataManager.getFetchingState() == calculatingMutual || mDataManager.getFetchingState() == finished) {
            VKApiCommunityArray groups = mDataManager.getGroupsMutualWithFriend(friend.id);
            if (groups != null) {
                viewHolder.mMutualsTextView.setText(mActivity.getString(R.string.mutual, groups.size()));
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
