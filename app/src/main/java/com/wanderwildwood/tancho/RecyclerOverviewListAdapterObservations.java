package com.wanderwildwood.tancho;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecyclerOverviewListAdapterObservations extends RecyclerView.Adapter<RecyclerOverviewListAdapterObservations.ObservationViewHolder> {

    private final Context context;
    private final List<BirdObservation> birdObservations;

    public RecyclerOverviewListAdapterObservations(Context context, List<BirdObservation> birdObservations) {
        this.context = context;
        this.birdObservations = birdObservations;
    }

    @Override
    public ObservationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bird_observation, parent, false);
        return new ObservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ObservationViewHolder holder, int position) {

        holder.name.setText(birdObservations.get(position).getName());

        // Upstream graded confidence red to green across five bands. The panel renders all
        // five as much the same grey, so this says it with the border instead, and in three
        // bands rather than five, which is as many as a border weight can be told apart at.
        //
        // The border is the whole of it: the row used to print "62 %" beside the name as
        // well. BirdNET's figure is a softmax output rather than a calibrated probability,
        // so two digits of it claim a precision that is not there, and the border was
        // already saying the part that is known. The listening screen dropped its own
        // percentage for the same reason.
        double probability = birdObservations.get(position).getProbability();
        if (probability < 0.5) holder.holder.setBackgroundResource(R.drawable.oval_uncertain);
        else if (probability < 0.8) holder.holder.setBackgroundResource(R.drawable.oval_likely);
        else holder.holder.setBackgroundResource(R.drawable.oval_confident);

        SimpleDateFormat sdf;
        Date date = new Date(birdObservations.get(position).getMillis());
        if (android.text.format.DateFormat.is24HourFormat(context)){
            sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else {
            sdf = new SimpleDateFormat("hh:mm aa", Locale.getDefault());
        }
        String timeString = sdf.format(date);
        holder.time.setText(timeString);

        java.text.DateFormat df = java.text.DateFormat.getDateInstance(DateFormat.SHORT);
        String dateString = df.format(birdObservations.get(position).getMillis());
        holder.date.setText(dateString);

        if (position == 0) {
            holder.date.setVisibility(View.VISIBLE);
        } else {
            String previousDateString = df.format(birdObservations.get(position-1).getMillis());
            if (!dateString.equals(previousDateString)) {
                holder.date.setVisibility(View.VISIBLE);
            } else {
                holder.date.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return birdObservations.size();
    }

    public int getSpeciesID(int position) {
        return birdObservations.get(position).getSpeciesId();
    }

    public long getMillis(int position) {
        return birdObservations.get(position).getMillis();
    }

    public String getLocation(int position) { return birdObservations.get(position).getLatitude() + ", " + birdObservations.get(position).getLongitude();}

    /**
     * Whether this observation was actually placed anywhere.
     *
     * An entry recorded with no fix, or before a location was set, stores 0/0 -- which is
     * also the app's own default in settings. Null Island is a real point on the map and
     * no bird was ever heard there, so this reads it as "nowhere" rather than a place.
     */
    public boolean hasLocation(int position) {
        return birdObservations.get(position).getLatitude() != 0.0
                || birdObservations.get(position).getLongitude() != 0.0;
    }

    public static class ObservationViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final LinearLayout holder;
        private final TextView time;
        private final TextView date;

        public ObservationViewHolder(View itemView) {
            super(itemView);
            this.name = (TextView) itemView.findViewById(R.id.name);
            this.holder = (LinearLayout) itemView.findViewById(R.id.holder);
            this.time = (TextView) itemView.findViewById(R.id.time);
            this.date = (TextView) itemView.findViewById(R.id.date);

        }

    }
}