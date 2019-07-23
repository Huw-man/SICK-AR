package com.example.sickar.tutorial;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class TutorialPagerAdapter extends FragmentPagerAdapter {
    private static final String TAG = "app_" + TutorialPagerAdapter.class.getSimpleName();

    private List<Fragment> mFragmentList = new ArrayList<>();

    public TutorialPagerAdapter(FragmentManager fm) {
        super(fm);
        mFragmentList.add(new TutorialPage1());
        mFragmentList.add(new TutorialPage2());
        mFragmentList.add(new TutorialPage3());
        mFragmentList.add(new TutorialPageLast());
    }


    /**
     * Return the Fragment associated with a specified position.
     *
     * @param position position in viewpager
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
}
