package edu.kit.bletest;

import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import edu.kit.bletest.dummy.Beacon;

import java.util.List;

/**
 */
public class MyBeaconRecyclerViewAdapter extends RecyclerView.Adapter<MyBeaconRecyclerViewAdapter.ViewHolder> {

    private List<Beacon> mValues;
    private BeaconFragment.BeaconListener listener;

    public MyBeaconRecyclerViewAdapter(List<Beacon> items, BeaconFragment.BeaconListener listener) {
        mValues = items;
        this.listener = listener;
    }

    public void refreshItems(List<Beacon> items) {
        mValues = items;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_peer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).identifier);
        holder.mContentView.setText(mValues.get(position).status.toString());
        holder.mContentView.setTextColor(mValues.get(position).status.getColor());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.beaconSelected(holder.mItem.identifier);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public Beacon mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.id);
            mContentView = (TextView) view.findViewById(R.id.status);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
