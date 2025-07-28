package com.example.smarttourism;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VisitedLocationAdapter extends RecyclerView.Adapter<VisitedLocationAdapter.ViewHolder> {

    private final List<String> visitedLocations;

    public VisitedLocationAdapter(List<String> visitedLocations) {
        this.visitedLocations = visitedLocations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_visited_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String locationName = visitedLocations.get(position);
        holder.locationNameTextView.setText(locationName);
    }

    @Override
    public int getItemCount() {
        return visitedLocations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView locationNameTextView;
        ImageView checkIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            locationNameTextView = itemView.findViewById(R.id.location_name_textview);
            checkIcon = itemView.findViewById(R.id.check_icon);
        }
    }
}
