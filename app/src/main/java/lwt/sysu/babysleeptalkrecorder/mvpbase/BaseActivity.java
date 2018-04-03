package lwt.sysu.babysleeptalkrecorder.mvpbase;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import dagger.android.AndroidInjection;


public abstract class BaseActivity extends AppCompatActivity implements IMVPView {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidInjection.inject(this);
    }
}
