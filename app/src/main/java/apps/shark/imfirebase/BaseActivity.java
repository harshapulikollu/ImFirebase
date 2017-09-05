package apps.shark.imfirebase;

/**
 * Created by Harsha on 9/2/2017.
 */
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;



import icepick.Icepick;
import icepick.State;

public abstract class BaseActivity extends AppCompatActivity {


    @State
    protected boolean isLoading;
    @State
    protected boolean userInteracted;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }

    public void showProgress() {
        isLoading = true;
        displayLoadingState();
    }

    public void hideProgress() {
        isLoading = false;
        displayLoadingState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Icepick.restoreInstanceState(this, savedInstanceState);
        if (savedInstanceState == null) {
            isLoading = false;
            userInteracted = false;
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        displayLoadingState();
    }

    protected abstract void displayLoadingState();

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

}
