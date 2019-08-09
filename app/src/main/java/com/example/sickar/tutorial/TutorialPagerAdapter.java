package com.example.sickar.tutorial;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.sickar.R;

import java.util.ArrayList;
import java.util.List;

public class TutorialPagerAdapter extends FragmentPagerAdapter {
    private static final String TAG = "app_" + TutorialPagerAdapter.class.getSimpleName();

    private List<Fragment> mFragmentList = new ArrayList<>();

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
            mFragmentList.add(new TutorialPage(id));
        }
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
