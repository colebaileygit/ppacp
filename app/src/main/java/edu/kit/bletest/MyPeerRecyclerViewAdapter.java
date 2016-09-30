package edu.kit.bletest;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import edu.kit.privateadhocpeering.Peer;
import edu.kit.privateadhocpeering.PeerDatabase;
import edu.kit.privateadhocpeering.PeerStatus;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Peer}
 */
public class MyPeerRecyclerViewAdapter extends RecyclerView.Adapter<MyPeerRecyclerViewAdapter.ViewHolder> {

    private List<Peer> mValues;
    private Context context;

    public MyPeerRecyclerViewAdapter(Context context, List<Peer> items) {
        mValues = items;
        this.context = context;
    }

    public void refreshItems(List<Peer> items) {
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
        holder.mIdView.setText(mValues.get(position).getIdentifier());
        holder.mContentView.setText(mValues.get(position).getStatus().toString());
        holder.mContentView.setTextColor(getStatusColor(mValues.get(position).getStatus()));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, PeerInfoActivity.class);
                i.putExtra("id", holder.mItem.getIdentifier());
                context.startActivity(i);
            }
        });
    }

    private int getStatusColor(PeerStatus status) {
        switch (status) {
            case AUTHENTICATED:
                return Color.BLUE;
            case CONNECTING:
            case CONNECTED:
                return Color.GREEN;
            case DISCOVERED:
                return Color.DKGRAY;
            case OUT_OF_RANGE:
                return Color.RED;
        }

        return Color.RED;
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public Peer mItem;

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
