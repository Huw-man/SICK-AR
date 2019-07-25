package com.example.sickar.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.example.sickar.R;
import com.example.sickar.main.DataViewModel;
import com.example.sickar.main.adapters.SystemsPagerAdapter;
import com.example.sickar.main.helpers.Item;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ImageActivity extends AppCompatActivity {
    private static final String TAG = "app_" + ImageActivity.class.getSimpleName();

    private DataViewModel mDataModel;
    private Item mItem;

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
        if (mDataModel.getCacheData().getValue() != null && barcode != null) {
            Log.i(TAG, "barcode for pictures " + barcode);
            mItem = mDataModel.getCacheData().getValue().get(barcode);
        }

        ViewPager mViewPager = findViewById(R.id.image_viewpager);
        SystemsPagerAdapter mPagerAdapter = new SystemsPagerAdapter(this.getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        TabLayout tabLayout = findViewById(R.id.image_system_tablayout);
        tabLayout.setupWithViewPager(mViewPager);

        // add the appropriate fragments for each system
        if (mItem != null) {
            this.addFragmentsToPagerAdapter(mPagerAdapter, mItem);
        } else {
            // add blank fragment
            mPagerAdapter.addFragment(new ImageSystemPageFragment(), "no item");
            mPagerAdapter.notifyDataSetChanged();
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
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return false;
    }

    /**
     * Add fragments to pagerAdapter according to the systems of an item
     *
     * @param pagerAdapter SystemsPagerAdapter
     * @param item         Item
     */
    private void addFragmentsToPagerAdapter(SystemsPagerAdapter pagerAdapter, Item item) {
        mDataModel.getPicturesForItem(item.getName()).thenAccept(jsonObject -> {
            findViewById(R.id.image_loading_progress).setVisibility(ProgressBar.GONE);

            //noinspection ConstantConditions
            Map<String, Map<String, String>> systemConfig =
                    mDataModel.getCacheData().getValue().getSystemConfig();
            try {
                JSONObject results = jsonObject.getJSONObject("results");
                if (results != null) {
                    // response contains stuff (pictures are not guaranteed at this point)
                    for (String sys : item.getSystemList()) {
                        String title = getResources().getString(R.string.system) + " " + sys;
                        if (!pagerAdapter.containsSystem(title)) {
                            JSONObject systemPics = results.getJSONObject(sys);
                            Map systemPicsMap = new Gson().fromJson(systemPics.toString(),
                                    HashMap.class);

                            Map<String, Bitmap> bitmaps = new HashMap<>();
                            for (Object dId : systemPicsMap.keySet()) {
                                String deviceId = (String) dId;
                                if (systemPicsMap.containsKey(deviceId)) {
                                    String pureBase64 = (String) systemPicsMap.get(deviceId);
                                    String cleanedBase64 =
                                            pureBase64.substring(Objects.requireNonNull(pureBase64).indexOf(",") + 1);
                                    byte[] decodedString = Base64.decode(cleanedBase64,
                                            Base64.DEFAULT);
                                    Bitmap decodedByte =
                                            BitmapFactory.decodeByteArray(decodedString, 0,
                                                    decodedString.length);

                                    //noinspection ConstantConditions
                                    bitmaps.put(
                                            systemConfig.get(sys).get(deviceId)
                                            , decodedByte
                                    );
                                }
                            }

//                            item.setSystem(sys);
                            pagerAdapter.addFragment(new ImageSystemPageFragment(bitmaps), title);
                        }
                    }
                    pagerAdapter.notifyDataSetChanged();
                } else {
                    // no item
                    Log.i(TAG, "backend returned null results for pictures");
                }
            } catch (JSONException | NullPointerException e) {
                e.printStackTrace();
                Log.i(TAG, "during adding fragments to imageActivity " + e.toString());
            }
        });
    }

}
