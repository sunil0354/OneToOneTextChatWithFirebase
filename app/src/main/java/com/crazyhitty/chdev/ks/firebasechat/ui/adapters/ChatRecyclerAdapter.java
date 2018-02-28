package com.crazyhitty.chdev.ks.firebasechat.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.crazyhitty.chdev.ks.firebasechat.R;
import com.crazyhitty.chdev.ks.firebasechat.models.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Author: Kartik Sharma
 * Created on: 10/16/2016 , 10:36 AM
 * Project: FirebaseChat
 */

public class ChatRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;

    private List<Chat> mChats;
    private Context context;
    public ChatRecyclerAdapter(List<Chat> chats) {
        mChats = chats;
    }

    public void add(Chat chat) {
        mChats.add(chat);
        notifyItemInserted(mChats.size() - 1);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        RecyclerView.ViewHolder viewHolder = null;
        context=parent.getContext();
        switch (viewType) {
            case VIEW_TYPE_ME:
                View viewChatMine = layoutInflater.inflate(R.layout.item_chat_mine, parent, false);
                viewHolder = new MyChatViewHolder(viewChatMine);
                break;
            case VIEW_TYPE_OTHER:
                View viewChatOther = layoutInflater.inflate(R.layout.item_chat_other, parent, false);
                viewHolder = new OtherChatViewHolder(viewChatOther);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (TextUtils.equals(mChats.get(position).senderUid,
                FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            configureMyChatViewHolder((MyChatViewHolder) holder, position);
        } else {
            configureOtherChatViewHolder((OtherChatViewHolder) holder, position);
        }
    }

    private void configureMyChatViewHolder(MyChatViewHolder myChatViewHolder, int position) {
        Chat chat = mChats.get(position);

        String alphabet = chat.sender.substring(0, 1);
        myChatViewHolder.txtUserAlphabet.setText(alphabet);
        if(chat.type!=null&&chat.type.equals("text")){
            myChatViewHolder.txtChatMessage.setVisibility(View.VISIBLE);
            myChatViewHolder.txtChatMessage.setText(chat.message);
            myChatViewHolder.iv_image.setVisibility(View.GONE);
            myChatViewHolder.play.setVisibility(View.GONE);
        }else if (chat.type!=null&&chat.type.equals("image")){
            myChatViewHolder.txtChatMessage.setVisibility(View.GONE);
            myChatViewHolder.iv_image.setVisibility(View.VISIBLE);
            Picasso.with(context).load(chat.message).into(myChatViewHolder.iv_image);
            myChatViewHolder.play.setVisibility(View.GONE);
        }else if (chat.type!=null&&chat.type.equals("audio")){
            myChatViewHolder.txtChatMessage.setVisibility(View.GONE);
            myChatViewHolder.iv_image.setVisibility(View.GONE);
            myChatViewHolder.play.setVisibility(View.VISIBLE);
        }
        myChatViewHolder.play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void configureOtherChatViewHolder(OtherChatViewHolder otherChatViewHolder, int position) {
        Chat chat = mChats.get(position);

        String alphabet = chat.sender.substring(0, 1);
        otherChatViewHolder.txtUserAlphabet.setText(alphabet);
        if(chat.type!=null&&chat.type.equals("text")){
            otherChatViewHolder.txtChatMessage.setVisibility(View.VISIBLE);
            otherChatViewHolder.txtChatMessage.setText(chat.message);
            otherChatViewHolder.iv_image.setVisibility(View.GONE);
            otherChatViewHolder.play.setVisibility(View.GONE);
        }else if (chat.type!=null&&chat.type.equals("image")){
            otherChatViewHolder.txtChatMessage.setVisibility(View.GONE);
            otherChatViewHolder.iv_image.setVisibility(View.VISIBLE);
            Picasso.with(context).load(chat.message).into(otherChatViewHolder.iv_image);
            otherChatViewHolder.play.setVisibility(View.GONE);
        }else if (chat.type!=null&&chat.type.equals("audio")){
            otherChatViewHolder.txtChatMessage.setVisibility(View.GONE);
            otherChatViewHolder.iv_image.setVisibility(View.GONE);
            otherChatViewHolder.play.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        if (mChats != null) {
            return mChats.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (TextUtils.equals(mChats.get(position).senderUid,
                FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            return VIEW_TYPE_ME;
        } else {
            return VIEW_TYPE_OTHER;
        }
    }

    private static class MyChatViewHolder extends RecyclerView.ViewHolder {
        private TextView txtChatMessage, txtUserAlphabet;
        private ImageView iv_image;
        private Button play;

        public MyChatViewHolder(View itemView) {
            super(itemView);
            txtChatMessage = (TextView) itemView.findViewById(R.id.text_view_chat_message);
            txtUserAlphabet = (TextView) itemView.findViewById(R.id.text_view_user_alphabet);
            iv_image = (ImageView) itemView.findViewById(R.id.iv_image);
            play = (Button) itemView.findViewById(R.id.play);
        }
    }

    private static class OtherChatViewHolder extends RecyclerView.ViewHolder {
        private TextView txtChatMessage, txtUserAlphabet;
        private ImageView iv_image;
        private Button play;


        public OtherChatViewHolder(View itemView) {
            super(itemView);
            txtChatMessage = (TextView) itemView.findViewById(R.id.text_view_chat_message);
            txtUserAlphabet = (TextView) itemView.findViewById(R.id.text_view_user_alphabet);
            iv_image = (ImageView) itemView.findViewById(R.id.iv_image);
            play = (Button) itemView.findViewById(R.id.play);
        }
    }
}
