package com.example.sickar.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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

/**
 * ImageActivity displays the images associated with a particular item. It is launched after
 * clicking the image icon in an Item's AR card or main information card.
 */
public class ImageActivity extends AppCompatActivity {
    /**
     * debugging TAG
     */
    private static final String TAG = "app_" + ImageActivity.class.getSimpleName();

    /**
     * ViewModel used to access data about a particular barcode item.
     */
    private DataViewModel viewModel;

    /**
     * The item which should have its images displayed.
     */
    private Item item;

    /**
     * Called on the creation of this Activity.
     *
     * @param savedInstanceState savedInstanceState contains saved bundles from the previous
     *                           instance if there were any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // start ViewModel
        viewModel = ViewModelProviders.of(this).get(DataViewModel.class);

        // populate this activity with pictures from a specific item
        String barcode = getIntent().getStringExtra("value");
        if (viewModel.getCacheData().getValue() != null && barcode != null) {
            Log.i(TAG, "barcode for pictures " + barcode);
            item = viewModel.getCacheData().getValue().get(barcode);
        }

        // initialize ViewPager
        ViewPager mViewPager = findViewById(R.id.image_viewpager);
        SystemsPagerAdapter mPagerAdapter = new SystemsPagerAdapter(this.getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        TabLayout tabLayout = findViewById(R.id.image_system_tablayout);
        tabLayout.setupWithViewPager(mViewPager);

        // add the appropriate fragments for each system
        if (item != null) {
            this.addFragmentsToPagerAdapter(mPagerAdapter, item);
        } else {
            // add blank fragment
            mPagerAdapter.addFragment(new ImageSystemPageFragment(), "no item");
            mPagerAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Called when the current {@link Window} of the activity gains or loses
     * focus.  This is the best indicator of whether this activity is visible
     * to the user.  The default implementation clears the key tracking
     * state, so should always be called.
     *
     * <p>Note that this provides information about global focus state, which
     * is managed independently of activity lifecycles.  As such, while focus
     * changes will generally have some relation to lifecycle changes (an
     * activity that is stopped will not generally get window focus), you
     * should not rely on any particular order between the callbacks here and
     * those in the other lifecycle methods such as {@link #onResume}.
     *
     * <p>As a general rule, however, a resumed activity will have window
     * focus...  unless it has displayed other dialogs or popups that take
     * input focus, in which case the activity itself will not have focus
     * when the other windows have it.  Likewise, the system may display
     * system-level windows (such as the status bar notification panel or
     * a system alert) which will temporarily take window input focus without
     * pausing the foreground activity.
     *
     * @param hasFocus Whether the window of this activity has focus.
     * @see #hasWindowFocus()
     * @see #onResume
     * @see View#onWindowFocusChanged(boolean)
     */
    @SuppressWarnings("JavadocReference")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );
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
     * Add fragments to pagerAdapter according to the systems of an item.
     * Issues a request through the ViewModel for images and will configure the pages upon
     * completion of the request.
     * (viewModel.getPicturesForItem returns a CompletableFuture)
     *
     * @param pagerAdapter SystemsPagerAdapter
     * @param item         Item
     */
    private void addFragmentsToPagerAdapter(SystemsPagerAdapter pagerAdapter, Item item) {
        viewModel.getPicturesForItem(item.getName()).thenAccept(jsonObject ->
        {
            findViewById(R.id.image_loading_progress).setVisibility(ProgressBar.GONE);

            //noinspection ConstantConditions
            Map<String, Map<String, String>> systemConfig =
                    viewModel.getCacheData().getValue().getSystemConfig();
            try {
                JSONObject results = jsonObject.getJSONObject("results");
                if (results != null) {
                    // response contains stuff (pictures are not guaranteed at this point)
                    for (String sys : item.getSystemList()) {
//                        String title = getResources().getString(R.string.system) + " " + sys;
                        String title = item.getProp(sys, "systemLabel");
                        if (!pagerAdapter.containsSystem(title)) {
                            JSONObject systemPics = results.getJSONObject(sys);
                            Map systemPicsMap = new Gson().fromJson(systemPics.toString(),
                                    HashMap.class);

                            Map<String, Bitmap> bitmaps = new HashMap<>();
                            for (Object dId : systemPicsMap.keySet()) {
                                String deviceId = (String) dId;
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
