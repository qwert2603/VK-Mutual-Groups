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
import com.vk.sdk.api.model.Identifiable;
import com.vk.sdk.api.model.VKApiModel;
import com.vk.sdk.api.model.VKList;

import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.calculatingMutual;
import static com.qwert2603.vkmutualgroups.data.DataManager.FetchingState.finished;

public abstract class AbstractAdapter<T extends VKApiModel & Identifiable> extends ArrayAdapter<T> {
    private DataManager mDataManager;
    private PhotoManager mPhotoManager;
    private Activity mActivity;

    public AbstractAdapter(Activity activity, VKList<T> list) {
        super(activity, 0, list);
        mActivity = activity;
        mDataManager = DataManager.get(mActivity);
        mPhotoManager = PhotoManager.get(mActivity);
    }

    protected abstract String getTitle(T item);
    protected abstract String getMutualsText(T item);
    protected abstract String getPhotoUrl(T item);

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

        T item = getItem(position);
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.mPosition = position;

        String photoUrl = getPhotoUrl(item);
        if (mPhotoManager.getPhoto(photoUrl) != null) {
            viewHolder.mPhotoImageView.setImageBitmap(mPhotoManager.getPhoto(photoUrl));
        }
        else {
            viewHolder.mPhotoImageView.setImageBitmap(null);
            //mPhotoManager.setPhotoToImageViewHolder(viewHolder, photoUrl);
        }

        viewHolder.mTitleTextView.setText(getTitle(item));

        if (mDataManager.getFetchingState() == calculatingMutual || mDataManager.getFetchingState() == finished) {
            viewHolder.mMutualsTextView.setText(getMutualsText(item));
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
