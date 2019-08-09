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
import java.util.List;

/**
 * RecyclerView Adapter that contains all the items that are scanned and displayed.
 */
public class ItemRecyclerViewAdapter extends RecyclerView.Adapter<ItemRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "app_" + ItemRecyclerViewAdapter.class.getSimpleName();

    private Context context;
    /**
     * contains the Items in RecyclerView
     */
    private ArrayList<Item> itemData;

    /**
     * Handles the touch events that deal with swipe to dismiss on the title bar
     */
    private ItemTouchHelper itemTouchHelper;

    /**
     * Constructor that passes in the item data and context
     *
     * @param context,  context
     * @param itemData, item data
     */
    public ItemRecyclerViewAdapter(Context context, ArrayList<Item> itemData,
                                   ItemTouchHelper itemTouchHelper) {
        this.context = context;
        this.itemData = itemData;
        this.itemTouchHelper = itemTouchHelper;
    }

    /**
     * Get the list of items contained in this adapter
     *
     * @return itemData
     */
    public List<Item> getItemData() {
        return itemData;
    }

    /**
     * Get the list of items as an list of barcode strings
     *
     * @return List<String> barcodes
     */
    public ArrayList<String> getItemDataStrings() {
        ArrayList<String> barcodes = new ArrayList<>();
        for (Item item : itemData) {
            barcodes.add(item.getName());
        }
        return barcodes;
    }

    /**
     * Adds a specified item into the recyclerView at the top position.
     *
     * @param item item to be inserted in recyclerView
     */
    public void addItem(Item item) {
        // add to the top of recyclerView
        itemData.add(0, item);
        this.notifyItemInserted(0);
        // check if over item limit
        if (getItemCount() > Constants.CACHE_SIZE) {
            // remove oldest ones at bottom
            int oldSize = getItemCount();
            itemData.subList(Constants.CACHE_SIZE, oldSize);
            this.notifyItemRangeRemoved(Constants.CACHE_SIZE, oldSize - Constants.CACHE_SIZE);
        }
    }

    /**
     * Updates an item if it is already contained in the recyclerView
     *
     * @param newItem item to be updated
     */
    public void updateItem(Item newItem) {
        // item comparison is by barcode so the newItem should be found in the list if its the same
        // barcode as the one it is trying to replace
        int index = itemData.indexOf(newItem);
        itemData.set(index, newItem);
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
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.cardview_item, parent, false), context);
    }

    /**
     * Called to bind the ViewHolder to a particular item in a position
     *
     * @param holder   ViewHolder
     * @param position position in recyclerView
     */
    @Override
    public void onBindViewHolder(@NonNull ItemRecyclerViewAdapter.ViewHolder holder, int position) {
        // get current item
        Item currentItem = itemData.get(position);
        // Populate the textviews with data
        holder.bindTo(currentItem);
    }

    /**
     * Get the number of items in the recyclerView
     */
    @Override
    public int getItemCount() {
        return itemData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView titleText;
        private ImageButton clearAR;
        private Switch displayAR;
        private ImageButton images;

        private SystemsPagerAdapter pagerAdapter;
        private ViewPager viewPager;
        private Context context;

        /**
         * Construct a ViewHolder from a view and its context
         *
         * @param itemView View
         * @param context  Context
         */
        @SuppressLint("ClickableViewAccessibility")
        ViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;
            // Initialize the viewPager
            ConstraintLayout root = itemView.findViewById(R.id.cardLayout);
            // must create a new instance of view pager for each item
            viewPager = new EnhancedWrapContentViewPager(context);
            viewPager.setId(View.generateViewId());
            viewPager.setSaveFromParentEnabled(false);
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            viewPager.setLayoutParams(params);
            viewPager.setBackgroundColor(context.getResources().getColor(R.color.card_body_background, null));
            root.addView(viewPager, viewPager.getLayoutParams());

            // Constrain viewPager to the bottom of tabLayout
            ConstraintSet set = new ConstraintSet();
            set.clone(root);
            set.connect(viewPager.getId(), ConstraintSet.TOP, R.id.tabLayout, ConstraintSet.BOTTOM);
            set.applyTo(root);

            // setup pagerAdapter
            pagerAdapter = new SystemsPagerAdapter(((FragmentActivity) context)
                    .getSupportFragmentManager());
            viewPager.setAdapter(pagerAdapter);
            TabLayout tabLayout = itemView.findViewById(R.id.tabLayout);
            tabLayout.setupWithViewPager(viewPager);

            // reference title textView
            titleText = itemView.findViewById(R.id.title);
            titleText.setOnTouchListener((v, ev) -> {
                itemTouchHelper.startSwipe(this);
                return true;
            });
            // AR controls
            clearAR = itemView.findViewById(R.id.clear_ar);
            displayAR = itemView.findViewById(R.id.display_ar);
            images = itemView.findViewById(R.id.images_launch_button);

            Log.i(TAG, "new Viewholder");
        }

        /**
         * Called to bind this ViewHolder to a specific item
         *
         * @param item Item
         */
        void bindTo(Item item) {
            titleText.setText(item.getName());
            // iterate through the number of systems of an item
            // only update the first time or for new systems
            Log.i(TAG, "new bind");
//            pagerAdapter.clear();
            for (String sys : item.getSystemList()) {
                if (!pagerAdapter.containsSystem(sys)) {
                    item.setSystem(sys);
                    pagerAdapter.addFragment(
                            new SystemPageFragment(item.getOneSystemData()), sys);
                }
            }
            pagerAdapter.notifyDataSetChanged();
            clearAR.setOnClickListener(v -> item.detachFromAnchors());
            displayAR.setOnCheckedChangeListener((buttonView, isChecked) -> item.minimizeAR(isChecked));
            item.setVisibleToggleReference(displayAR);

            images.setOnClickListener((v) -> {
                Intent imageActivityIntent = new Intent(context, ImageActivity.class);
                imageActivityIntent.putExtra("value", item.getName());
                context.startActivity(imageActivityIntent);
            });
        }
    }
}
