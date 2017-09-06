package apps.shark.imfirebase.thread;

/**
 * Created by Harsha on 9/2/2017.
 */
import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import apps.shark.imfirebase.BaseActivity;
import apps.shark.imfirebase.Constants;
import apps.shark.imfirebase.R;
import apps.shark.imfirebase.beans.Message;
import apps.shark.imfirebase.beans.User;
import apps.shark.imfirebase.login.LoginActivity;
import apps.shark.imfirebase.widgets.EmptyStateRecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import icepick.Icepick;
import icepick.State;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class ThreadActivity extends BaseActivity implements TextWatcher {

    @BindView(R.id.activity_thread_toolbar)
    Toolbar toolbar;
    @BindView(R.id.activity_thread_messages_recycler)
    EmptyStateRecyclerView messagesRecycler;
    @BindView(R.id.activity_thread_send_fab)
    FloatingActionButton sendFab;
    @BindView(R.id.activity_thread_input_edit_text)
    TextInputEditText inputEditText;
    @BindView(R.id.activity_thread_empty_view)
    TextView emptyView;
    @BindView(R.id.activity_thread_editor_parent)
    RelativeLayout editorParent;
    @BindView(R.id.activity_thread_progress)
    ProgressBar progress;
    @BindView(R.id.activity_thread_ic_camera)
    ImageView camera;

    private DatabaseReference mDatabase, mUserDatabase;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private StorageReference mStorage;

    @State
    String userUid;
    @State
    boolean emptyInput;

    boolean online_status, app_active;
    private User user;
    private FirebaseUser owner;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);
        Icepick.restoreInstanceState(this, savedInstanceState);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
      //to make chat data available offline too
       // FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.keepSynced(true);
        if (savedInstanceState == null) {
            userUid = getIntent().getStringExtra(Constants.USER_ID_EXTRA);
        }
        sendFab.requestFocus();

        loadUserDetails();
        initializeAuthListener();
        initializeInteractionListeners();

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getCurrentUser().getUid());
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot != null) {
                    if(app_active == true){
                        mUserDatabase.child("online").setValue(true);
                    }
                    else{
                        mUserDatabase.child("online").setValue(false);

                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void initializeInteractionListeners() {
        inputEditText.addTextChangedListener(this);
    }

    private void loadUserDetails() {
        DatabaseReference userReference = mDatabase
                .child("users")
                .child(userUid);

        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user = dataSnapshot.getValue(User.class);
                initializeMessagesRecycler();
                if(dataSnapshot.hasChild("online")){
                    online_status = (boolean) dataSnapshot.child("online").getValue();
                }
                displayUserDetails();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ThreadActivity.this, R.string.error_loading_user, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void initializeAuthListener() {
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                owner = firebaseAuth.getCurrentUser();
                if (owner != null) {
                    initializeMessagesRecycler();

                    Log.d("@@@@", "thread:signed_in:" + owner.getUid());
                } else {
                    Log.d("@@@@", "thread:signed_out");
                    Intent login = new Intent(ThreadActivity.this, LoginActivity.class);
                    startActivity(login);
                    finish();
                }
            }
        };
        mAuth.addAuthStateListener(mAuthListener);
    }

    private void initializeMessagesRecycler() {
        if (user == null || owner == null) {
            Log.d("@@@@", "initializeMessagesRecycler: User:" + user + " Owner:" + owner);
            return;
        }
        Query messagesQuery = mDatabase
                .child("messages")
                .child(owner.getUid())
                .child(user.getUid())
                .orderByChild("negatedTimestamp");
        MessagesAdapter adapter = new MessagesAdapter(this, owner.getUid(), messagesQuery);
        messagesRecycler.setAdapter(null);
        messagesRecycler.setAdapter(adapter);
        messagesRecycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));
        messagesRecycler.setEmptyView(emptyView);
        messagesRecycler.getAdapter().registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                messagesRecycler.smoothScrollToPosition(0);
            }
        });
    }
    //code for getting photo from gallery with permission when user clicks on camera icon
    @OnClick(R.id.activity_thread_ic_camera)
    public void onClickCamera(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //getting permissions for first time

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

            } else {

                getPhoto();

            }

        } else {

            getPhoto();

        }
    }

    //permission for gallery
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                getPhoto();

            }


        }

    }

    //In order get images from gallery
    public void getPhoto() {
        mProgress = new ProgressDialog(this);
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1);


    }
    //Result of getting the image into app
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null){

            mProgress.setMessage("Sending the image...");
            mProgress.show();
            Uri selectedImage = data.getData();
            Log.d("@@@","imge uri: "+selectedImage);

            final String imageLocation = "Photos" + "/" ;
            final String imageLocationId = imageLocation + "/" + selectedImage.getLastPathSegment();
            final String uniqueId = UUID.randomUUID().toString();
            final StorageReference filepath = mStorage.child(imageLocation).child(uniqueId + "/image_message");
            final String downloadURl = filepath.getPath();
            filepath.putFile(selectedImage).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //create a new message containing this image
                   // addImageToMessages(downloadURl);

                    Toast.makeText(ThreadActivity.this, downloadURl, Toast.LENGTH_SHORT).show();
                    Log.d("@@@","downloadurl"+downloadURl);
                   mProgress.dismiss();
                    long timestamp = new Date().getTime();
                    long dayTimestamp = getDayTimestamp(timestamp);
                    String body = downloadURl; //inputEditText.getText().toString().trim();
                    String ownerUid = owner.getUid();
                    String userUid = user.getUid();
                    if(!body.isEmpty()) {
                        Message message =
                                new Message(timestamp, -timestamp, dayTimestamp, body, ownerUid, userUid);
                        mDatabase
                                .child("notifications")
                                .child("messages")
                                .push()
                                .setValue(message);
                        mDatabase
                                .child("messages")
                                .child(userUid)
                                .child(ownerUid)
                                .push()
                                .setValue(message);
                        if (!userUid.equals(ownerUid)) {
                            mDatabase
                                    .child("messages")
                                    .child(ownerUid)
                                    .child(userUid)
                                    .push()
                                    .setValue(message);
                        }
                        inputEditText.setText("");
                    }
                }
            });
        }
    }

        //code for sending msg when send button clicked
    @OnClick(R.id.activity_thread_send_fab)
    public void onClick() {
        if (user == null || owner == null) {
            Log.d("@@@@", "onSendClick: User:" + user + " Owner:" + owner);
            return;
        }
        long timestamp = new Date().getTime();
        long dayTimestamp = getDayTimestamp(timestamp);
        String body = inputEditText.getText().toString().trim();
        String ownerUid = owner.getUid();
        String userUid = user.getUid();
        if(!body.isEmpty()) {
            Message message =
                    new Message(timestamp, -timestamp, dayTimestamp, body, ownerUid, userUid);
            mDatabase
                    .child("notifications")
                    .child("messages")
                    .push()
                    .setValue(message);
            mDatabase
                    .child("messages")
                    .child(userUid)
                    .child(ownerUid)
                    .push()
                    .setValue(message);
            if (!userUid.equals(ownerUid)) {
                mDatabase
                        .child("messages")
                        .child(ownerUid)
                        .child(userUid)
                        .push()
                        .setValue(message);
            }
            inputEditText.setText("");
        }
    }

    @Override
    protected void displayLoadingState() {
        //was considering a progress bar but firebase offline database makes it unnecessary

        //TransitionManager.beginDelayedTransition(editorParent);
        progress.setVisibility(isLoading ? VISIBLE : INVISIBLE);
        //displayInputState();
    }

    private void displayInputState() {
        //inputEditText.setEnabled(!isLoading);
        sendFab.setEnabled(!emptyInput && !isLoading);
        //sendFab.setImageResource(isLoading ? R.color.colorTransparent : R.drawable.ic_send);
    }

    private long getDayTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        return calendar.getTimeInMillis();
    }

    private void displayUserDetails() {
        //todo[improvement]: maybe display the picture in the toolbar.. WhatsApp style
        toolbar.setTitle(user.getDisplayName());
        if(online_status == (true)){
            toolbar.setSubtitle("Online");
        }
        else{
            toolbar.setSubtitle("Offline");
        }


    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        emptyInput = s.toString().trim().isEmpty();
        displayInputState();
    }

    @Override
    public void onStart(){
        super.onStart();
        app_active = true;
        mUserDatabase.child("online").setValue(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        app_active = false;
        mUserDatabase.child("online").setValue(false);
    }

}
