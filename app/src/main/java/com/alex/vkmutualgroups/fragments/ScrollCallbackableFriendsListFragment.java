package com.alex.vkmutualgroups.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

/**
 * Расширяет FriendsListFragment.
 * Передает содержащей Activity обратные вызовы скроллинга списка друзей.
 */
public class ScrollCallbackableFriendsListFragment extends FriendsListFragment {

    public static ScrollCallbackableFriendsListFragment newInstance(int groupId) {
        ScrollCallbackableFriendsListFragment result = new ScrollCallbackableFriendsListFragment();
        FriendsListFragment friendsListFragment = FriendsListFragment.newInstance(groupId);
        result.setArguments(friendsListFragment.getArguments());
        return result;
    }

    public interface Callbacks {
        void onListViewScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Создаем обертку вокруг слушателя прокрутки родительского класса.
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                mListViewOnScrollListener.onScrollStateChanged(view, scrollState);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                mListViewOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                if (getActivity() instanceof Callbacks) {
                    ((Callbacks) getActivity()).onListViewScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                }
            }
        });

        return view;
    }
}
