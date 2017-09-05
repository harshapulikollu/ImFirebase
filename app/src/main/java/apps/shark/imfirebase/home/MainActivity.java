package apps.shark.imfirebase.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import apps.shark.imfirebase.Constants;
import apps.shark.imfirebase.R;
import apps.shark.imfirebase.beans.User;
import apps.shark.imfirebase.login.LoginActivity;
import apps.shark.imfirebase.thread.ThreadActivity;
import apps.shark.imfirebase.widgets.EmptyStateRecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.activity_main_toolbar)
    Toolbar toolbar;
    @BindView(R.id.activity_main_users_recycler)
    EmptyStateRecyclerView usersRecycler;
    @BindView(R.id.activity_main_empty_view)
    TextView emptyView;

    private FirebaseAuth mAuth ;
    private boolean app_active;
    public static final int RC_SIGN_IN = 1;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase , mUserDatabase;

    @Override
    public void onStart() {
        super.onStart();
        app_active= true;
        mUserDatabase.child("online").setValue(true);
        EventBus.getDefault().register(this);
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        app_active= false;
        mUserDatabase.child("online").setValue(false);
        EventBus.getDefault().unregister(this);
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        initializeFirebaseAuthListener();
        initializeUsersRecycler();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getCurrentUser().getUid());
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot != null) {
                    mUserDatabase.child("online").onDisconnect().setValue(false);
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

    private void initializeUsersRecycler() {
        UsersAdapter adapter = new UsersAdapter(this, mDatabase.child("users"));
        usersRecycler.setAdapter(adapter);
        usersRecycler.setLayoutManager(new LinearLayoutManager(this));
        usersRecycler.setEmptyView(emptyView);
    }

    private void initializeFirebaseAuthListener() {
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    addUserToDatabase(user);
                    Log.d("@@@@", "home:signed_in:" + user.getUid());
                } else {
                    Log.d("@@@@", "home:signed_out");
                    Intent login = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(login);
                    finish();


                }
            }
        };
    }

    private void addUserToDatabase(FirebaseUser firebaseUser) {
        User user = new User(
                firebaseUser.getDisplayName(),
                firebaseUser.getEmail(),
                firebaseUser.getUid(),
                firebaseUser.getPhotoUrl() == null ? "" : firebaseUser.getPhotoUrl().toString()
        );

        mDatabase.child("users")
                .child(user.getUid()).setValue(user);

        String instanceId = FirebaseInstanceId.getInstance().getToken();
        if (instanceId != null) {
            mDatabase.child("users")
                    .child(firebaseUser.getUid())
                    .child("instanceId")
                    .setValue(instanceId);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUserSelected(DatabaseReference selectedRef) {
        Intent thread = new Intent(this, ThreadActivity.class);
        thread.putExtra(Constants.USER_ID_EXTRA, selectedRef.getKey());
        startActivity(thread);
    }

}
