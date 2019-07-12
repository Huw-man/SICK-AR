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

        mPagerAdapter.addFragment(new ImageSystemPageFragment(), "system 1");
        mPagerAdapter.addFragment(new ImageSystemPageFragment(), "system 2");
        mPagerAdapter.addFragment(new ImageSystemPageFragment(), "system 3");
        mPagerAdapter.addFragment(new ImageSystemPageFragment(), "system 4");
        mPagerAdapter.notifyDataSetChanged();

        mScaleGestureDetector = new ScaleGestureDetector(this,
                new ScaleListener());


    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private static final float MAX_SCALE = 10f;
        private static final float MIN_SCALE = 0.1f;
        private float mScaleFactor = 1f;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.i(TAG, "scale factor" + detector.getScaleFactor());
            mScaleFactor = detector.getScaleFactor();
            mScaleFactor = Math.max(MIN_SCALE, Math.min(MAX_SCALE, mScaleFactor));
//            top.setScaleX(mScaleFactor);
//            top.setScaleY(mScaleFactor);
            return super.onScale(detector);
        }
    }
}
