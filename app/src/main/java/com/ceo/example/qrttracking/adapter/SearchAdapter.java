package com.ceo.example.qrttracking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ceo.example.qrttracking.R;
import com.ceo.example.qrttracking.data.PartInfo;

import java.util.ArrayList;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private ArrayList<PartInfo> itemList;
    private ArrayList<PartInfo> filteredList;
    private OnItemClickListener listener;

    public SearchAdapter(ArrayList<PartInfo> itemList, OnItemClickListener listener) {
        this.itemList = itemList;
        this.filteredList = new ArrayList<>(itemList);
        this.listener = listener;
    }


    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PartInfo partInfo = filteredList.get(position);
        holder.bind(partInfo);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(itemList);
        } else {
            query = query.toLowerCase();
            for (PartInfo item : itemList) {
                if (item.getPsName().toLowerCase().contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView nameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.txt_psname);
            itemView.setOnClickListener(this);
        }

        public void bind(PartInfo item) {
            nameTextView.setText(item.getPsName());
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(filteredList.get(position));
                }
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(PartInfo item);
    }
}
