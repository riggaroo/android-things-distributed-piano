package za.co.riggaroo.androidthings.distributedpiano;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.things.contrib.driver.pwmspeaker.Speaker;

import java.io.IOException;

public class PianoActivity extends Activity implements PianoContract.View {

    private static final String TAG = "PianoActivity";
    private Speaker speaker;

    private PianoContract.Presenter presenter;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            speaker = new Speaker(BoardDefaults.getPwmPin());
            presenter = new PianoPresenter(this, (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE),
                    getString(R.string.service_id), getPackageName());
        } catch (IOException e) {
            throw new IllegalArgumentException("Piezo can't be opened, lets end this here.");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        presenter.attachView(this);

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        presenter.detachView();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            speaker.stop();
            speaker.close();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to stop the piezo", e);
        }
    }


    @Override
    public void playNote(final double frequency) {
        try {
            speaker.play(frequency);
        } catch (IOException e) {
            throw new IllegalArgumentException("Piezo can't play note.", e);
        }
    }

    @Override
    public void stopPlayingNote() {
        try {
            speaker.stop();
        } catch (IOException e) {
            throw new IllegalArgumentException("Piezo can't stop.", e);
        }
    }
}
