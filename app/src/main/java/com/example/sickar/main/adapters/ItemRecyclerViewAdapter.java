package com.example.sickar.main.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.example.sickar.Constants;
import com.example.sickar.R;
import com.example.sickar.image.ImageActivity;
import com.example.sickar.libs.EnhancedWrapContentViewPager;
import com.example.sickar.main.helpers.Item;
import com.example.sickar.main.helpers.SystemPageFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;


public class ItemRecyclerViewAdapter extends RecyclerView.Adapter<ItemRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "app_" + ItemRecyclerViewAdapter.class.getSimpleName();
    private Context mContext;
    private ArrayList<Item> mItemData;
    private ItemTouchHelper mItemTouchHelper;

    /**
     * Constructor that passes in the item data and context
     *
     * @param context,  context
     * @param itemData, item data
     */
    public ItemRecyclerViewAdapter(Context context, ArrayList<Item> itemData,
                                   ItemTouchHelper itemTouchHelper) {
        mContext = context;
        mItemData = itemData;
        mItemTouchHelper = itemTouchHelper;
    }

    public ArrayList<Item> getItemData() {
        return mItemData;
    }

    /**
     * Get the list of items as an arraylist of barcode strings
     *
     * @return ArrayList<String> barcodes
     */
    public ArrayList<String> getItemDataStrings() {
        ArrayList<String> barcodes = new ArrayList<>();
        for (Item item : mItemData) {
            barcodes.add(item.getName());
        }
        return barcodes;
    }

    public void addItem(Item item) {
        // add to the top of recyclerView
        mItemData.add(0, item);
        this.notifyItemInserted(0);
        // check if over item limit
        if (getItemCount() > Constants.CACHE_SIZE) {
            // remove oldest ones at bottom
            int oldSize = getItemCount();
            mItemData.subList(Constants.CACHE_SIZE, oldSize);
            this.notifyItemRangeRemoved(Constants.CACHE_SIZE, oldSize - Constants.CACHE_SIZE);
        }
    }

    public void updateItem(Item newItem) {
        // item comparison is by barcode so the newItem should be found in the list if its the same
        // barcode as the one it is trying to replace
        int index = mItemData.indexOf(newItem);
        mItemData.set(index, newItem);
        this.notifyItemChanged(index);
    }

    /**
     * Required method for creating the viewholder objects.
     *
     * @param parent   The ViewGroup into which the new View will be added
     *                 after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return The newly created ViewHolder.
     */
    @NonNull
    @Override
    public ItemRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.cardview_item, parent, false), mContext);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemRecyclerViewAdapter.ViewHolder holder, int position) {
        // get current item
        Item currentItem = mItemData.get(position);
        // Populate the textviews with data
        holder.bindTo(currentItem);
    }

    @Override
    public int getItemCount() {
        return mItemData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTitleText;
        private ImageButton mClearAR;
        private Switch mDisplayAR;
        private ImageButton mImages;

        private SystemsPagerAdapter mPageAdapter;
        private ViewPager mViewPager;
        private Context mContext;

        @SuppressLint("ClickableViewAccessibility")
        ViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            mContext = context;
            // Initialize the viewPager
            ConstraintLayout root = itemView.findViewById(R.id.cardLayout);
            // must create a new instance of view pager for each item
            mViewPager = new EnhancedWrapContentViewPager(context);
            mViewPager.setId(View.generateViewId());
            mViewPager.setSaveFromParentEnabled(false);
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mViewPager.setLayoutParams(params);
            mViewPager.setBackgroundColor(context.getResources().getColor(R.color.card_body_background, null));
            root.addView(mViewPager, mViewPager.getLayoutParams());

            // Constrain viewPager to the bottom of tabLayout
            ConstraintSet set = new ConstraintSet();
            set.clone(root);
            set.connect(mViewPager.getId(), ConstraintSet.TOP, R.id.tabLayout, ConstraintSet.BOTTOM);
            set.applyTo(root);

            // setup pagerAdapter
            mPageAdapter = new SystemsPagerAdapter(((FragmentActivity) context)
                    .getSupportFragmentManager());
            mViewPager.setAdapter(mPageAdapter);
            TabLayout tabLayout = itemView.findViewById(R.id.tabLayout);
            tabLayout.setupWithViewPager(mViewPager);

            // reference title textView
            mTitleText = itemView.findViewById(R.id.title);
            mTitleText.setOnTouchListener((v, ev) -> {
                mItemTouchHelper.startSwipe(this);
                return true;
            });
            // AR controls
            mClearAR = itemView.findViewById(R.id.clear_ar);
            mDisplayAR = itemView.findViewById(R.id.display_ar);
            mImages = itemView.findViewById(R.id.images_launch_button);

            Log.i(TAG, "new Viewholder");
        }

        void bindTo(Item item) {
            mTitleText.setText(item.getName());
            // iterate through the number of systems of an item
            // only update the first time or for new systems
            Log.i(TAG, "new bind");
//            mPageAdapter.clear();
            for (String sys : item.getSystemList()) {
                if (!mPageAdapter.containsSystem(sys)) {
                    item.setSystem(sys);
                    mPageAdapter.addFragment(
                            new SystemPageFragment(item.getOneSystemData()), sys);
                }
            }
            mPageAdapter.notifyDataSetChanged();
            mClearAR.setOnClickListener(v -> item.detachFromAnchors());
            mDisplayAR.setOnCheckedChangeListener((buttonView, isChecked) -> item.minimizeAR(isChecked));
            item.setVisibleToggleReference(mDisplayAR);

            mImages.setOnClickListener((v) -> {
                Intent imageActivityIntent = new Intent(mContext, ImageActivity.class);
                imageActivityIntent.putExtra("value", item.getName());
                mContext.startActivity(imageActivityIntent);
            });
        }
    }
}
