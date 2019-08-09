package com.example.sickar.tutorial;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.sickar.R;

import java.util.ArrayList;
import java.util.List;

/**
 * PagerAdapter that holds all the slides of the tutorial.
 */
public class TutorialPagerAdapter extends FragmentPagerAdapter {
    private static final String TAG = "app_" + TutorialPagerAdapter.class.getSimpleName();

    private List<Fragment> fragmentList = new ArrayList<>();

    /**
     * Construct a pagerAdapter with a fragment manager
     *
     * @param fm FragmentManager
     */
    TutorialPagerAdapter(FragmentManager fm) {
        super(fm);
        int[] pageList = new int[]{
                R.layout.fragment_tutorial_page_1,
                R.layout.fragment_tutorial_page_2,
                R.layout.fragment_tutorial_page_3,
                R.layout.fragment_tutorial_page_4,
                R.layout.fragment_tutorial_page_5
        };
        for (int id : pageList) {
            fragmentList.add(new TutorialPage(id));
        }
        fragmentList.add(new TutorialPageLast());
    }


    /**
     * Return the Fragment associated with a specified position.
     *
     * @param position position in viewpager
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
}
