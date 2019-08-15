package com.example.sickar.main.adapters;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;

import com.example.sickar.main.helpers.SystemPageFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * This PagerAdapter hold the displays for multiple Systems.
 */
public class SystemsPagerAdapter extends FragmentPagerAdapter {
    /**
     * Debugging TAG
     */
    private static final String TAG = "app_" + SystemPageFragment.class.getSimpleName();

    /**
     * Holds the fragments for each system.
     */
    private List<Fragment> fragmentList = new ArrayList<>();

    /**
     * Holds the title for each fragment that will be displayed in the TabLayout tabs.
     */
    private List<String> fragmentTitleList = new ArrayList<>();

    /**
     * FragmentManager for clearing the fragments.
     */
    private FragmentManager fragmentManager;

    /**
     * Construct a this pagerAdapter from a fragment manager
     *
     * @param fm FragmentManager
     */
    public SystemsPagerAdapter(FragmentManager fm) {
        super(fm);
        fragmentManager = fm;
    }

    /**
     * Add a new fragment to this pager adapter
     *
     * @param fragment new fragment
     * @param title    title for the TabLayout display
     */
    public void addFragment(Fragment fragment, String title) {
        fragmentList.add(fragment);
        fragmentTitleList.add(title);
    }

    /**
     * Check if this pager adapter already contains a fragment
     *
     * @param title title of the fragment
     * @return true if fragment is already contained, false if not
     */
    public boolean containsSystem(String title) {
        return fragmentTitleList.contains(title);
    }

    /**
     * Return the Fragment associated with a specified position.
     *
     * @param position position of element to get
     */
    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    /**
     * Return the number of views available.
     */
    @Override
    public int getCount() {
        return fragmentList.size();
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
        return fragmentTitleList.get(position);
    }

    /**
     * Clears this adapter of all its fragments
     */
    public void clear() {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment fragment : fragmentList) {
            transaction.remove(fragment);
        }
        fragmentList.clear();
        fragmentTitleList.clear();
        transaction.commitAllowingStateLoss();
    }
}
