package com.example.sickar.main.adapters;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;

import com.example.sickar.main.helpers.SystemPageFragment;

import java.util.ArrayList;
import java.util.List;

public class SystemsPagerAdapter extends FragmentPagerAdapter {
    private static final String TAG = "app_" + SystemPageFragment.class.getSimpleName();
    private List<Fragment> mFragmentList = new ArrayList<>();
    private List<String> mFragmentTitleList = new ArrayList<>();
    private FragmentManager mFragmentManager;

    public SystemsPagerAdapter(FragmentManager fm) {
        super(fm);
        mFragmentManager = fm;
    }

    public void addFragment(Fragment fragment, String title) {
        mFragmentList.add(fragment);
        mFragmentTitleList.add(title);
    }

    public boolean containsSystem(String title) {
        return mFragmentTitleList.contains(title);
    }

    /**
     * Return the Fragment associated with a specified position.
     *
     * @param position position of element to get
     */
    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    /**
     * Return the number of views available.
     */
    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    /**
     * This method may be called by the ViewPager to obtain a title string
     * to describe the specified page. This method may return null
     * indicating no title for this page. The default implementation returns
     * null.
     *
     * @param position The position of the title requested
     * @return A title for the requested page
     */
    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentTitleList.get(position);
    }

    /**
     * Clears this adapter of all its fragments
     */
    public void clear() {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        for (Fragment fragment : mFragmentList) {
            transaction.remove(fragment);
        }
        mFragmentList.clear();
        mFragmentTitleList.clear();
        transaction.commitAllowingStateLoss();
    }
}
