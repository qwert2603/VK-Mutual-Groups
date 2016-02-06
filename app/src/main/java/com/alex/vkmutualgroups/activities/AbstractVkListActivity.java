package com.alex.vkmutualgroups.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.alex.vkmutualgroups.R;

/**
 * Activity, отображающая фрагмент-список (друзей или групп).
 */
public abstract class AbstractVkListActivity extends NavigableActivity {

    protected abstract String getActionBarTitle();
    protected abstract Fragment getListFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getActionBarTitle());
        }
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            fragment = getListFragment();
            fm.beginTransaction().add(R.id.fragment_container, fragment).commitAllowingStateLoss();
        }
    }

}
