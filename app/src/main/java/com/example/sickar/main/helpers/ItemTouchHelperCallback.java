package com.example.sickar.main.helpers;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sickar.main.adapters.ItemRecyclerViewAdapter;

import java.util.Collections;

/**
 * Callback that handles the gestures on recyclerView ViewHolders
 */
@SuppressWarnings("JavadocReference")
public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private static final String TAG = "app_" + ItemTouchHelperCallback.class.getSimpleName();

    private ItemRecyclerViewAdapter adapter;

    /**
     * Construct an instance without an adapter Note that the adapter must be set afterward
     */
    public ItemTouchHelperCallback() {
    }

    /**
     * Construct an instance with a recyclerViewAdapter
     *
     * @param recyclerViewAdapter adapter
     */
    public ItemTouchHelperCallback(ItemRecyclerViewAdapter recyclerViewAdapter) {
        adapter = recyclerViewAdapter;
    }

    /**
     * Set the recyclerViewAdapter
     *
     * @param recyclerViewAdapter adapter
     */
    public void setAdapter(ItemRecyclerViewAdapter recyclerViewAdapter) {
        adapter = recyclerViewAdapter;
    }

    /**
     * Should return a composite flag which defines the enabled move directions in each state
     * (idle, swiping, dragging).
     * <p>
     * Instead of composing this flag manually, you can use {@link #makeMovementFlags(int,
     * int)}
     * or {@link #makeFlag(int, int)}.
     * <p>
     * This flag is composed of 3 sets of 8 bits, where first 8 bits are for IDLE state, next
     * 8 bits are for SWIPE state and third 8 bits are for DRAG state.
     * Each 8 bit sections can be constructed by simply OR'ing direction flags defined in
     * {@link ItemTouchHelper}.
     * <p>
     * For example, if you want it to allow swiping LEFT and RIGHT but only allow starting to
     * swipe by swiping RIGHT, you can return:
     * <pre>
     *      makeFlag(ACTION_STATE_IDLE, RIGHT) | makeFlag(ACTION_STATE_SWIPE, LEFT | RIGHT);
     * </pre>
     * This means, allow right movement while IDLE and allow right and left movement while
     * swiping.
     *
     * @param recyclerView The RecyclerView to which ItemTouchHelper is attached.
     * @param viewHolder   The ViewHolder for which the movement information is necessary.
     * @return flags specifying which movements are allowed on this ViewHolder.
     * @see #makeMovementFlags(int, int)
     * @see #makeFlag(int, int)
     */
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT |
                        ItemTouchHelper.DOWN | ItemTouchHelper.UP,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
    }

    /**
     * Returns whether ItemTouchHelper should start a swipe operation if a pointer is swiped
     * over the View.
     * <p>
     * Default value returns true but you may want to disable this if you want to start
     * swiping on a custom view touch using {@link #startSwipe(ViewHolder)}.
     *
     * @return True if ItemTouchHelper should start swiping an item when user swipes a pointer
     * over the View, false otherwise. Default value is <code>true</code>.
     * @see #startSwipe(ViewHolder)
     */
    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    /**
     * Called when ItemTouchHelper wants to move the dragged item from its old position to
     * the new position.
     * <p>
     * If this method returns true, ItemTouchHelper assumes {@code viewHolder} has been moved
     * to the adapter position of {@code target} ViewHolder
     * ({@link ViewHolder#getAdapterPosition()
     * ViewHolder#getAdapterPosition()}).
     * <p>
     * If you don't support drag & drop, this method will never be called.
     *
     * @param recyclerView The RecyclerView to which ItemTouchHelper is attached to.
     * @param viewHolder   The ViewHolder which is being dragged by the user.
     * @param target       The ViewHolder over which the currently active item is being
     *                     dragged.
     * @return True if the {@code viewHolder} has been moved to the adapter position of
     * {@code target}.
     * @see #onMoved(RecyclerView, ViewHolder, int, ViewHolder, int, int, int)
     */
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        // get the from and to positions
        int from = viewHolder.getAdapterPosition();
        int to = target.getAdapterPosition();

        // Swap the items and notify the adapter
        Collections.swap(adapter.getItemData(), from, to);
        adapter.notifyItemMoved(from, to);
        return true;
    }

    /**
     * Called when a ViewHolder is swiped by the user.
     * <p>
     * If you are returning relative directions ({@link #START} , {@link #END}) from the
     * {@link #getMovementFlags(RecyclerView, ViewHolder)} method, this method
     * will also use relative directions. Otherwise, it will use absolute directions.
     * <p>
     * If you don't support swiping, this method will never be called.
     * <p>
     * ItemTouchHelper will keep a reference to the View until it is detached from
     * RecyclerView.
     * As soon as it is detached, ItemTouchHelper will call
     * {@link #clearView(RecyclerView, ViewHolder)}.
     *
     * @param viewHolder The ViewHolder which has been swiped by the user.
     * @param direction  The direction to which the ViewHolder is swiped. It is one of
     *                   {@link #UP}, {@link #DOWN},
     *                   {@link #LEFT} or {@link #RIGHT}. If your
     *                   {@link #getMovementFlags(RecyclerView, ViewHolder)}
     *                   method
     *                   returned relative flags instead of {@link #LEFT} / {@link #RIGHT};
     *                   `direction` will be relative as well. ({@link #START} or {@link
     *                   #END}).
     */
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int index = viewHolder.getAdapterPosition();

        Item item = adapter.getItemData().get(index);
        item.setScanned(false); // removed from display
        if (item.isPlaced()) {
            // clear the AR display
            // detach from AR anchors
            item.detachFromAnchors();
        }
        // clear the switch reference
        item.setVisibleToggleReference(null);

        // Remove the item from the recyclerView adapter.
        // Note the item remains in the barcodeData cache
        adapter.getItemData().remove(index);
        // Notify the adapter.
        adapter.notifyItemRemoved(index);
        adapter.notifyItemRangeChanged(index, adapter.getItemCount());
        Log.i(TAG, "removing recycelerView item");
    }
}
