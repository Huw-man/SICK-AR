package com.example.sickar.main.helpers;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sickar.R;
import com.example.sickar.Utils;

import java.util.Map;

/**
 * This class displays the data of an item associated with a particular system
 */
public class SystemPageFragment extends Fragment {
    private static final String TAG = "app_" + SystemPageFragment.class.getSimpleName();

    private Map<String, String> properties;

    /**
     * Construct an instance with the properties of an item from a particular system
     *
     * @param itemProperties properties
     */
    public SystemPageFragment(Map<String, String> itemProperties) {
        properties = itemProperties;
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null (which
     * is the default implementation).  This will be called between
     * {@link #onCreate(Bundle)} and {@link #onActivityCreated(Bundle)}.
     *
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_system_page, container, false);

        // attachToRoot = false
        // this will not work if it is set to true
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            View pView = inflater.inflate(R.layout.item_property_display,
                    view.findViewById(R.id.fragment_linear_layout),
                    false);
            TextView property = pView.findViewById(R.id.item_property);
            property.setText(Utils.unpackCamelCase(entry.getKey()));
            TextView value = pView.findViewById(R.id.item_value);
            value.setText(entry.getValue());
            // add the view
            ((LinearLayout) view.findViewById(R.id.fragment_linear_layout)).addView(pView);
        }
        return view;
    }
}
