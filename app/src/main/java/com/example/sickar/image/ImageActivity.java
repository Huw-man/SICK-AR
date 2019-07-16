package com.example.sickar.image;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.ScaleGestureDetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.example.sickar.R;
import com.example.sickar.main.DataViewModel;
import com.example.sickar.main.adapters.SystemsPagerAdapter;
import com.example.sickar.main.helpers.Item;
import com.google.android.material.tabs.TabLayout;

public class ImageActivity extends AppCompatActivity {
    private static final String TAG = "app_" + ImageActivity.class.getSimpleName();

    private DataViewModel mDataModel;
    private Item mItem;

    private ScaleGestureDetector mScaleGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // start ViewModel and attach observers to liveData and errorLiveData
        mDataModel = ViewModelProviders.of(this).get(DataViewModel.class);

        // populate this activity with pictures from a specific item
        String barcode = getIntent().getStringExtra("value");
        if (mDataModel.getLiveData().getValue() != null) {
            mItem = mDataModel.getLiveData().getValue().get(barcode);
        }
        Log.i(TAG, "barcode for pictures " + barcode);

        ViewPager mViewPager = findViewById(R.id.image_viewpager);
        SystemsPagerAdapter mPagerAdapter = new SystemsPagerAdapter(this.getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        TabLayout tabLayout = findViewById(R.id.image_system_tablayout);
        tabLayout.setupWithViewPager(mViewPager);

        // add the appropriate fragments for each system
        if (mItem != null) {
            this.addFragmentsToPagerAdapter(mPagerAdapter, mItem);
        }

    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     *
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.</p>
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "menu select " + item.getItemId() + " " + R.id.homeAsUp);
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return false;
    }


    private void addFragmentsToPagerAdapter(SystemsPagerAdapter pagerAdapter, Item item) {
        for (String sys : item.getSystemList()) {
            if (!pagerAdapter.containsSystem(sys)) {
                item.setSystem(sys);
                pagerAdapter.addFragment(new ImageSystemPageFragment(), sys);
            }
        }
        pagerAdapter.notifyDataSetChanged();
    }

}
