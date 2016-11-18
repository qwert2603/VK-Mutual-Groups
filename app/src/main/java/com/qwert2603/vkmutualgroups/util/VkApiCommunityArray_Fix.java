package com.qwert2603.vkmutualgroups.util;

import android.os.Parcel;
import android.support.annotation.Nullable;

import com.vk.sdk.api.model.VKApiCommunityFull;
import com.vk.sdk.api.model.VKApiModel;
import com.vk.sdk.api.model.VKList;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Список групп.
 * Реализация в vk-sdk сожержит подобный класс, но не соответствует Parcelable.
 * Данный класс создан по подобию {@link com.vk.sdk.api.model.VKUsersArray}.
 */
public class VKApiCommunityArray_Fix extends VKList<VKApiCommunityFull> {

    @Override
    public VKApiModel parse(JSONObject response) throws JSONException {
        fill(response, VKApiCommunityFull.class);
        return this;
    }

    @SuppressWarnings("unused")
    public VKApiCommunityArray_Fix() {
    }

    public VKApiCommunityArray_Fix(Parcel in) {
        super(in);
    }

    public static Creator<VKApiCommunityArray_Fix> CREATOR = new Creator<VKApiCommunityArray_Fix>() {
        public VKApiCommunityArray_Fix createFromParcel(Parcel source) {
            return new VKApiCommunityArray_Fix(source);
        }

        public VKApiCommunityArray_Fix[] newArray(int size) {
            return new VKApiCommunityArray_Fix[size];
        }
    };

    public VKApiCommunityArray_Fix(@Nullable VKList<VKApiCommunityFull> communityFulls) {
        if (communityFulls != null) {
            for (Object group : communityFulls) {
                add((VKApiCommunityFull) group);
            }
        }
    }
}
