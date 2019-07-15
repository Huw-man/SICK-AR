package com.example.sickar.image;

import android.os.Bundle;
import android.util.Log;
import android.view.ScaleGestureDetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.example.sickar.R;
import com.example.sickar.main.DataViewModel;
import com.example.sickar.main.adapters.SystemsPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

public class ImageActivity extends AppCompatActivity {
    private static final String TAG = "app_" + ImageActivity.class.getSimpleName();

    private DataViewModel mDataModel;

    private ScaleGestureDetector mScaleGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getResources().getString(R.string.app_name));

        // start ViewModel and attach observers to liveData and errorLiveData
        mDataModel = ViewModelProviders.of(this).get(DataViewModel.class);

        // populate this activity with pictures from a specific item
        String barcode = getIntent().getStringExtra("value");
        Log.i(TAG, "barcode for pictures " + barcode);

        ViewPager mViewPager = findViewById(R.id.image_viewpager);
        SystemsPagerAdapter mPagerAdapter = new SystemsPagerAdapter(this.getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        TabLayout tabLayout = findViewById(R.id.image_system_tablayout);
        tabLayout.setupWithViewPager(mViewPager);

        // add the appropriate fragments for each system
        mPagerAdapter.addFragment(new ImageSystemPageFragment(), "system 1");
        mPagerAdapter.addFragment(new ImageSystemPageFragment(), "system 2");
        mPagerAdapter.addFragment(new ImageSystemPageFragment(), "system 3");
        mPagerAdapter.addFragment(new ImageSystemPageFragment(), "system 4");
        mPagerAdapter.notifyDataSetChanged();

    }


}
