package za.co.riggaroo.androidthings.pianoplayer;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;

import java.nio.ByteBuffer;

import static android.content.Context.CONNECTIVITY_SERVICE;

class PlayPianoPresenter implements PlayPianoContract.Presenter, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, Connections.EndpointDiscoveryListener, Connections.MessageListener {

    private static final String TAG = "PlayPianoPresenter";
    private static final long TIMEOUT_DISCOVER = 4000;
    private final GoogleApiClient googleApiClient;
    private final String serviceId;
    private Context context;
    private String otherEndpointId;
    private PlayPianoContract.View view;

    PlayPianoPresenter(Context context, String serviceId) {
        googleApiClient = new GoogleApiClient.Builder(context).addApi(Nearby.CONNECTIONS_API)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        this.context = context;
        this.serviceId = serviceId;
    }

    public void attachView(PlayPianoContract.View view) {
        this.view = view;
        googleApiClient.connect();
    }

    public void detachView() {
        this.view = null;
        googleApiClient.disconnect();
    }

    private boolean isViewAttached() {
        return view != null;
    }

    @Override
    public void notePlayed(final int noteNumber) {
        double frequency = getFrequencyForNote(noteNumber + 28);
        Log.d(TAG, "Frequency:" + frequency);
        sendNote(frequency);
    }

    @Override
    public void noteStopped(final int note) {
        sendStop();
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        Log.d(TAG, "onConnected!");
        startDiscovery();
    }

    @Override
    public void onConnectionSuspended(final int i) {
        Log.d(TAG, "onConnectionSuspended!");
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult.getErrorCode());
    }

    private void connectTo(String endpointId, final String endpointName) {
        Log.d(TAG, "connectTo:" + endpointId + ":" + endpointName);
        Nearby.Connections.sendConnectionRequest(googleApiClient, null, endpointId, null,
                new Connections.ConnectionResponseCallback() {
                    @Override
                    public void onConnectionResponse(String endpointId, Status status, byte[] bytes) {
                        Log.d(TAG, "onConnectionResponse:" + endpointId + ":" + status);
                        if (!isViewAttached()) {
                            return;
                        }
                        if (status.isSuccess()) {
                            Log.d(TAG, "onConnectionResponse: " + endpointName + " SUCCESS");
                            view.showConnectedToMessage(endpointName);

                            otherEndpointId = endpointId;
                        } else {
                            Log.d(TAG, "onConnectionResponse: " + endpointName + " FAILURE");
                        }
                    }
                }, this);
    }

    private boolean isConnectedToNetwork() {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo info1 = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        return (info != null && info.isConnectedOrConnecting()) || (info1 != null && info1.isConnectedOrConnecting());
    }


    private void startDiscovery() {
        Log.d(TAG, "startDiscovery");
        if (!isConnectedToNetwork()) {
            Log.d(TAG, "startDiscovery: not connected to WiFi network.");
            return;
        }

        Nearby.Connections.startDiscovery(googleApiClient, serviceId, TIMEOUT_DISCOVER, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (!isViewAttached()) {
                            return;
                        }
                        if (status.isSuccess()) {
                            Log.d(TAG, "startDiscovery:onResult: SUCCESS");
                        } else {
                            Log.d(TAG, "startDiscovery:onResult: FAILURE");

                            int statusCode = status.getStatusCode();
                            if (statusCode == ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING) {
                                Log.d(TAG, "STATUS_ALREADY_DISCOVERING");
                            }
                        }
                    }
                });
    }

    @Override
    public void onEndpointFound(final String endpointId, String deviceId, String serviceId, final String endpointName) {
        Log.d(TAG, "onEndpointFound:" + endpointId + ":" + endpointName);

        connectTo(endpointId, endpointName);

    }

    @Override
    public void onEndpointLost(String endpointId) {
        Log.d(TAG, "onEndpointLost:" + endpointId);
    }

    /**
     * The function for calculating the frequency that should be played for a certain note.
     * More information about the formula can be found here: https://en.wikipedia.org/wiki/Piano_key_frequencies
     *
     * @param note The number of the note.
     * @return frequency to play on Piezo.
     */
    private double getFrequencyForNote(int note) {
        return Math.pow(2, ((note - 49.0f) / 12.0f)) * 440;
    }

    private void sendNote(final double frequency) {
        if (!googleApiClient.isConnected()) {
            view.showApiNotConnected();

            return;
        }
        Nearby.Connections.sendReliableMessage(googleApiClient, otherEndpointId, toByteArray(frequency));
    }

    private static byte[] toByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }

    private void sendStop() {
        if (!googleApiClient.isConnected()) {
            view.showApiNotConnected();
            return;
        }
        Nearby.Connections.sendReliableMessage(googleApiClient, otherEndpointId, toByteArray(-1));

    }

    @Override
    public void onMessageReceived(final String s, final byte[] bytes, final boolean b) {

    }

    @Override
    public void onDisconnected(final String s) {

    }
}
