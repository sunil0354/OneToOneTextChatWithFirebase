package com.crazyhitty.chdev.ks.firebasechat.ui.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.crazyhitty.chdev.ks.firebasechat.R;
import com.crazyhitty.chdev.ks.firebasechat.core.chat.ChatContract;
import com.crazyhitty.chdev.ks.firebasechat.core.chat.ChatPresenter;
import com.crazyhitty.chdev.ks.firebasechat.events.PushNotificationEvent;
import com.crazyhitty.chdev.ks.firebasechat.models.Chat;
import com.crazyhitty.chdev.ks.firebasechat.ui.adapters.ChatRecyclerAdapter;
import com.crazyhitty.chdev.ks.firebasechat.utils.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

/**
 * Author: Kartik Sharma
 * Created on: 8/28/2016 , 10:36 AM
 * Project: FirebaseChat
 */

public class ChatFragment extends Fragment implements ChatContract.View, TextView.OnEditorActionListener {
    private RecyclerView mRecyclerViewChat;
    private EditText mETxtMessage;

    private ProgressDialog mProgressDialog;

    private ChatRecyclerAdapter mChatRecyclerAdapter;

    private ChatPresenter mChatPresenter;
    private Button start,stop;
    private static final int GALLERY_PICK = 1;
    private StorageReference mImageStorage;
    private MediaRecorder mRecorder = null;
    private static String mFileName = null;
    private MediaPlayer mediaPlayer;


    public static ChatFragment newInstance(String receiver,
                                           String receiverUid,
                                           String firebaseToken) {
        Bundle args = new Bundle();
        args.putString(Constants.ARG_RECEIVER, receiver);
        args.putString(Constants.ARG_RECEIVER_UID, receiverUid);
        args.putString(Constants.ARG_FIREBASE_TOKEN, firebaseToken);
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_chat, container, false);
        bindViews(fragmentView);
        return fragmentView;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindViews(View view) {
        mRecyclerViewChat = (RecyclerView) view.findViewById(R.id.recycler_view_chat);
        mETxtMessage = (EditText) view.findViewById(R.id.edit_text_message);
        start=(Button) view.findViewById(R.id.start);
        stop=(Button) view.findViewById(R.id.stop);
        // Record to the external cache directory for visibility
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent galleryIntent = new Intent();
//                galleryIntent.setType("image/*");
//                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
//                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);
//            }
//        });
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
                mETxtMessage.setText("Recording......");
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                mETxtMessage.setText(getString(R.string.type_a_message));
            }
        });
//        button.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction()==MotionEvent.ACTION_DOWN){
//                    startRecording();
//                    mETxtMessage.setText("Recording......");
//
//                }else if (event.getAction()==MotionEvent.ACTION_UP){
//                    stopRecording();
//                    mETxtMessage.setText("");
//                }
//                return false;
//            }
//        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    private void init() {
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setTitle(getString(R.string.loading));
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.setIndeterminate(true);

        mETxtMessage.setOnEditorActionListener(this);

        mChatPresenter = new ChatPresenter(this);
        mChatPresenter.getMessage(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                getArguments().getString(Constants.ARG_RECEIVER_UID));
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            sendMessage();
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mImageStorage = FirebaseStorage.getInstance().getReference();
        if(requestCode == GALLERY_PICK && resultCode == RESULT_OK){
            Uri imageUri = data.getData();
            StorageReference storageReference = mImageStorage.child("message_images").child( "hello" + ".jpg");
            storageReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUri = taskSnapshot.getDownloadUrl();
                    String message = String.valueOf(downloadUri);
                    String receiver = getArguments().getString(Constants.ARG_RECEIVER);
                    String receiverUid = getArguments().getString(Constants.ARG_RECEIVER_UID);
                    String sender = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    String senderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    String receiverFirebaseToken = getArguments().getString(Constants.ARG_FIREBASE_TOKEN);
                    Chat chat = new Chat(sender,
                            receiver,
                            senderUid,
                            receiverUid,
                            message,
                            System.currentTimeMillis(),"image");
                    mChatPresenter.sendMessage(getActivity().getApplicationContext(),
                            chat,
                            receiverFirebaseToken);
                }
            });

        }

    }

    private void sendMessage() {
        String message = mETxtMessage.getText().toString();
        String receiver = getArguments().getString(Constants.ARG_RECEIVER);
        String receiverUid = getArguments().getString(Constants.ARG_RECEIVER_UID);
        String sender = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String senderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String receiverFirebaseToken = getArguments().getString(Constants.ARG_FIREBASE_TOKEN);
        Chat chat = new Chat(sender,
                receiver,
                senderUid,
                receiverUid,
                message,
                System.currentTimeMillis(),"text");
        mChatPresenter.sendMessage(getActivity().getApplicationContext(),
                chat,
                receiverFirebaseToken);
    }

    @Override
    public void onSendMessageSuccess() {
        mETxtMessage.setText("");
        Toast.makeText(getActivity(), "Message sent", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSendMessageFailure(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGetMessagesSuccess(Chat chat) {
        if (mChatRecyclerAdapter == null) {
            mChatRecyclerAdapter = new ChatRecyclerAdapter(new ArrayList<Chat>());
            mRecyclerViewChat.setAdapter(mChatRecyclerAdapter);
        }
        mChatRecyclerAdapter.add(chat);
        mRecyclerViewChat.smoothScrollToPosition(mChatRecyclerAdapter.getItemCount() - 1);
    }

    @Override
    public void onGetMessagesFailure(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Subscribe
    public void onPushNotificationEvent(PushNotificationEvent pushNotificationEvent) {
        if (mChatRecyclerAdapter == null || mChatRecyclerAdapter.getItemCount() == 0) {
            mChatPresenter.getMessage(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                    pushNotificationEvent.getUid());
        }
    }

    private void startRecording() {
        mFileName =getActivity().getExternalCacheDir().getAbsolutePath();
        mFileName += "/audiorecordtest.mp3";
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        // Record to the external cache directory for visibility
        Log.e("TAG", "startRecording: "+mFileName );
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e("TAG", "prepare() failed");
        }
        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        mediaPlayer = new  MediaPlayer();
        try {
            mediaPlayer.setDataSource(mFileName);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        uploadAudio();
    }

    private void uploadAudio() {
        Uri imageUri = Uri.fromFile(new File(mFileName));
        mImageStorage = FirebaseStorage.getInstance().getReference();
        StorageReference storageReference = mImageStorage.child("audio_message").child( "hello" + ".mp3");
        storageReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUri = taskSnapshot.getDownloadUrl();
                String message = String.valueOf(downloadUri);
                String receiver = getArguments().getString(Constants.ARG_RECEIVER);
                String receiverUid = getArguments().getString(Constants.ARG_RECEIVER_UID);
                String sender = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                String senderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String receiverFirebaseToken = getArguments().getString(Constants.ARG_FIREBASE_TOKEN);
                Chat chat = new Chat(sender,
                        receiver,
                        senderUid,
                        receiverUid,
                        message,
                        System.currentTimeMillis(),"audio");
                mChatPresenter.sendMessage(getActivity().getApplicationContext(),
                        chat,
                        receiverFirebaseToken);
            }
        });
    }
}
