package com.ceo.example.qrttracking.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ceo.example.qrttracking.Interface.OnItemClickListener;
import com.ceo.example.qrttracking.R;
import com.ceo.example.qrttracking.data.PartInfo;

import java.util.ArrayList;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.MyViewHolder> {

    private Context context;
    private ArrayList<PartInfo> dataList;
    private ArrayList<PartInfo> filteredList;
    private OnItemClickListener listener;


    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public SearchAdapter(Context context, ArrayList<PartInfo> dataList) {
        this.context = context;
        this.dataList = dataList;
        this.filteredList = new ArrayList<>(dataList);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.bind(filteredList.get(position));
        holder.serach_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (listener != null) {
                    listener.onItemClick(position);
                }

            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(dataList);

            Log.d("item search","no data found");

        } else {
            query = query.toLowerCase();
            for (PartInfo item : dataList) {
                if (item.getPsName().toLowerCase().contains(query)) {
                    filteredList.add(item);

                    Log.d("filteredlist",filteredList.toString());
                }
            }
        }
        notifyDataSetChanged();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView txt_psname;
         RelativeLayout serach_layout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            txt_psname = itemView.findViewById(R.id.txt_psname);
            serach_layout = itemView.findViewById(R.id.serach_layout);

        }

        public void bind(PartInfo item) {
            txt_psname.setText(item.getPsName());

        }
    }
}
