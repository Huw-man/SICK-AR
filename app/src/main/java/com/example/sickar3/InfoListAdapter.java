package com.example.sickar3;

import android.content.Context;
import android.icu.text.IDNA;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class InfoListAdapter extends RecyclerView.Adapter<InfoListAdapter.ViewHolder> {
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize the views
            mTitleText = itemView.findViewById(R.id.title);
            mBodyText = itemView.findViewById(R.id.body);
        }

        void bindTo(Item item) {
            mTitleText.setText(item.getName());
            mBodyText.setText(item.getAllPropsAsString());
        }
    }
}
