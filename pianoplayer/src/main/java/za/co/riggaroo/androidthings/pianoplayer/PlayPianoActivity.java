package za.co.riggaroo.androidthings.pianoplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import za.co.riggaroo.androidthings.pianoplayer.keyboard.KeyBoardListener;
import za.co.riggaroo.androidthings.pianoplayer.keyboard.KeyboardView;
import za.co.riggaroo.androidthings.pianoplayer.keyboard.ScrollStripView;

public class PlayPianoActivity extends AppCompatActivity implements PlayPianoContract.View {

    private static final String TAG = "PlayPianoActivity";

    PlayPianoContract.Presenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_piano);
        presenter = new PlayPianoPresenter(this, getString(R.string.service_id));

        KeyboardView keyboardView = (KeyboardView) findViewById(R.id.piano);
        ScrollStripView scrollStrip = (ScrollStripView) findViewById(R.id.scrollstrip);
        scrollStrip.bindKeyboard(keyboardView);
        keyboardView.setMidiListener(new KeyBoardListener() {
            @Override
            public void onNoteOff(final int channel, final int note, final int velocity) {
                Log.d(TAG, "onNoteOff, channel:" + channel + " note:" + note + ". velocity:" + velocity);
                presenter.noteStopped(note);
            }

            @Override
            public void onNoteOn(final int channel, final int note, final int velocity) {
                Log.d(TAG, "onNoteOn, channel:" + channel + " note:" + note + ". velocity:" + velocity);

                presenter.notePlayed(note);
            }
        });

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
    public void showConnectedToMessage(final String endpointName) {
        Toast.makeText(getApplicationContext(), getString(R.string.connected_to, endpointName), Toast.LENGTH_LONG)
                .show();

    }

    @Override
    public void showApiNotConnected() {
        Toast.makeText(getApplicationContext(), getString(R.string.google_api_not_connected), Toast.LENGTH_LONG).show();
    }

}
