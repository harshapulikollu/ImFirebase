package apps.shark.imfirebase.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;

import apps.shark.imfirebase.R;
import apps.shark.imfirebase.beans.User;
import butterknife.BindView;
import jp.wasabeef.glide.transformations.CropCircleTransformation;


/**
 * Created by Harsha on 9/7/2017.
 */

public class UserProfile extends AppCompatActivity{

    @BindView(R.id.activity_profile_toolbar)
    Toolbar toolbar;
    ImageView profileImage;
    TextView abouttextview;
    String  activeUsername,imageurl,abouttext;
    private CollapsingToolbarLayout collapsingToolbarLayout = null;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        setSupportActionBar(toolbar);
       // ActionBar actionBar = getSupportActionBar(); TODO: check abt action bar
       // actionBar.setDisplayHomeAsUpEnabled(true);

         profileImage = (ImageView) findViewById(R.id.activity_profile_image_view);
        abouttextview = (TextView) findViewById(R.id.activity_profile_about);
        Intent intent = getIntent();
        activeUsername = intent.getStringExtra("username");
        imageurl = intent.getStringExtra("picurl");
        abouttext = intent.getStringExtra("email");
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(activeUsername);
        Glide.with(this)
                .load(imageurl)
                .asBitmap()
                .placeholder(R.drawable.placeholder_user)
                .into(profileImage);

        toolbarTextAppernce();
        abouttextview.setText(abouttext);
    }
    //private void dynamicToolbarColor() {


        /*Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher); */
        /*Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {

            @Override
            public void onGenerated(Palette palette) {
                collapsingToolbarLayout.setContentScrimColor(palette.getMutedColor(getResources().getColor(R.color.colorPrimary)));
                collapsingToolbarLayout.setStatusBarScrimColor(palette.getMutedColor(getResources().getColor(R.color.colorPrimaryDark)));
            }
        });*/
   // }

    private void toolbarTextAppernce() {
        collapsingToolbarLayout.setCollapsedTitleTextAppearance(R.style.collapsedappbar);
        collapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.expandedappbar);
    }
}
