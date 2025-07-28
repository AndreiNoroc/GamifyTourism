package com.example.smarttourism.adapters;

import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup; import android.widget.TextView;

import androidx.annotation.NonNull; import androidx.recyclerview.widget.RecyclerView;

import com.example.smarttourism.R; import com.example.smarttourism.models.UserScore;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private List<UserScore> users;

    public LeaderboardAdapter(List<UserScore> users) {
        this.users = users;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtUsername, txtScore;

        public ViewHolder(View view) {
            super(view);
            txtUsername = view.findViewById(R.id.text_username);
            txtScore = view.findViewById(R.id.text_score);
        }
    }

    @NonNull
    @Override
    public LeaderboardAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard_user, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardAdapter.ViewHolder holder, int position) {
        UserScore user = users.get(position);
        holder.txtUsername.setText(user.getUsername());
        holder.txtScore.setText(String.valueOf(user.getScore()));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

}