package com.example.defindin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder>{

    List<MessageModel> messageModelList;
    public MessageAdapter(List<MessageModel> messageModelList) {
        this.messageModelList = messageModelList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View ChatView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_layout,null);
        MyViewHolder myViewHolder = new MyViewHolder(ChatView);
        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        MessageModel messageModel = messageModelList.get(position);
        if(messageModel.getSendBy().equals(MessageModel.SEND_BY_ME))
        {
            holder.meLayout.setVisibility(View.VISIBLE);
            holder.botLayout.setVisibility(View.GONE);
            holder.meTxt.setText(messageModel.getMessage().toString());

        }
        else
        {
            holder.meLayout.setVisibility(View.GONE);
            holder.botLayout.setVisibility(View.VISIBLE);
            holder.botTxt.setText(messageModel.getMessage());
        }

    }

    @Override
    public int getItemCount() {
        return messageModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder
    {
        TextView meTxt,botTxt;
        LinearLayout meLayout,botLayout;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            meTxt = itemView.findViewById(R.id.meTxt);
            botTxt = itemView.findViewById(R.id.botTxt);
            meLayout = itemView.findViewById(R.id.meLayout);
            botLayout = itemView.findViewById(R.id.botLayout);

        }
    }
}
