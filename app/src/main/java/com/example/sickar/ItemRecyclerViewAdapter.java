package com.example.sickar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;


public class ItemRecyclerViewAdapter extends RecyclerView.Adapter<ItemRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "app_" + ItemRecyclerViewAdapter.class.getSimpleName();
    private Context mContext;
    private ArrayList<Item> mItemData;

    /**
     * Constructor that passes in the item data and context
     *
     * @param mContext,  context
     * @param mItemData, item data
     */
    public ItemRecyclerViewAdapter(Context context, ArrayList<Item> mItemData) {
        this.mContext = context;
        this.mItemData = mItemData;
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
            for (int i = getItemCount() - 1; i >= Constants.CACHE_SIZE; i--) {
                mItemData.remove(mItemData.size() - 1);
                this.notifyItemRemoved(mItemData.size() - 1);
            }
        }
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
                .inflate(R.layout.list_item, parent, false), mContext);
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTitleText;
        private TextView mBodyText;
        private ImageButton mClearAR;
        private Switch mDisplayAR;

        private SystemsPageAdapter mPageAdapter;
        private ViewPager mViewPager;


        public ViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            // Initialize the views
            mTitleText = itemView.findViewById(R.id.title);
            mViewPager = itemView.findViewById(R.id.viewPager);
            mPageAdapter = new SystemsPageAdapter(((FragmentActivity) context)
                    .getSupportFragmentManager());
            mViewPager.setAdapter(mPageAdapter);
            TabLayout tabLayout = itemView.findViewById(R.id.tabLayout);
            tabLayout.setupWithViewPager(mViewPager);

            // AR controls
            mClearAR = itemView.findViewById(R.id.clear_ar);
            mDisplayAR = itemView.findViewById(R.id.display_ar);
        }

        void bindTo(Item item) {
            mTitleText.setText(item.getName());
            // iterate through the number of systems of item
            // only update the first time or for new systems
            if (item.getSystems().size() > mPageAdapter.getCount()) {
                for (String sys : item.getSystems()) {
                    item.setSystem(sys);
                    mPageAdapter.addFragment(
                            new SystemTabFragment(item.getAllPropsAsString()), sys);
                }
            }
            mPageAdapter.notifyDataSetChanged();
            mClearAR.setOnClickListener(v -> {
                item.detachFromAnchors();
                mDisplayAR.setChecked(false);
            });
            mDisplayAR.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    item.minimizeAR(isChecked);
                    item.setVisible_toggle(mDisplayAR);
            });
        }
    }
}
