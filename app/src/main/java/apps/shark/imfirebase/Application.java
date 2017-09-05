package apps.shark.imfirebase;

/**
 * Created by Harsha on 9/2/2017.
 */
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Application extends android.app.Application {


    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

 /*        //for online and offline status.
        mAuth = FirebaseAuth.getInstance();
        mUserDatabase =  FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getCurrentUser().getUid());

       mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot != null) {
                    mUserDatabase.child("online").onDisconnect().setValue(false);
                    //mUserDatabase.child("online").setValue(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        }); */
    }

}
