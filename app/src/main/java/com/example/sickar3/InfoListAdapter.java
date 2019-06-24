package com.example.sickar3;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class InfoListAdapter extends RecyclerView.Adapter<InfoListAdapter.ViewHolder> {
    private static final String TAG = "app_" + MainActivity.class.getSimpleName();
    private Context mContext;
    private ArrayList<Item> mItemData;

    /**
     * Constructor that passes in the item data and context
     *
     * @param mContext,  context
     * @param mItemData, item data
     */
    public InfoListAdapter(Context context, ArrayList<Item> mItemData) {
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
    public InfoListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull InfoListAdapter.ViewHolder holder, int position) {
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize the views
            mTitleText = itemView.findViewById(R.id.title);
            mBodyText = itemView.findViewById(R.id.body);
            mClearAR = itemView.findViewById(R.id.clear_ar);
            mDisplayAR = itemView.findViewById(R.id.display_ar);
            mDisplayAR.setChecked(true); // initialize as true
        }

        void bindTo(Item item) {
            mTitleText.setText(item.getName());
            mBodyText.setText(item.getAllPropsAsString());
            mClearAR.setOnClickListener(v -> item.detachFromAnchors());
            mDisplayAR.setOnCheckedChangeListener((buttonView, isChecked) -> item.minimizeAR(isChecked));
        }
    }
}
