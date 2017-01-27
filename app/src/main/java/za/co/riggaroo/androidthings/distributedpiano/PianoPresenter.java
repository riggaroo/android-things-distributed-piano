package za.co.riggaroo.androidthings.distributedpiano;


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
import com.google.android.gms.nearby.connection.AppIdentifier;
import com.google.android.gms.nearby.connection.AppMetadata;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class PianoPresenter implements PianoContract.Presenter, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, Connections.ConnectionRequestListener, Connections.MessageListener {

    private static final String TAG = "PianoPresenter";
    private final GoogleApiClient googleApiClient;
    private final String serviceId;
    private final String packageName;
    private PianoContract.View view;
    private ConnectivityManager connectivityManager;

    PianoPresenter(Context context, ConnectivityManager connectivityManager, String serviceId,
                   String packageName) throws IOException {
        googleApiClient = new GoogleApiClient.Builder(context).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(Nearby.CONNECTIONS_API).build();
        this.serviceId = serviceId;
        this.connectivityManager = connectivityManager;
        this.packageName = packageName;
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        Log.d(TAG, "onConnected!");
        startAdvertising();

    }

    @Override
    public void onConnectionSuspended(final int i) {
        Log.d(TAG, "onConnectionSuspended!");
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
    }

    private boolean isConnectedToNetwork() {
        NetworkInfo info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo info1 = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

        return (info != null && info.isConnectedOrConnecting()) || (info1 != null && info1.isConnectedOrConnecting());
    }


    private void startAdvertising() {
        Log.d(TAG, "startAdvertising");
        if (!isConnectedToNetwork()) {
            Log.d(TAG, "startAdvertising: not connected to WiFi network.");
            return;
        }

        List<AppIdentifier> appIdentifierList = new ArrayList<>();
        appIdentifierList.add(new AppIdentifier(packageName));
        AppMetadata appMetadata = new AppMetadata(appIdentifierList);
        Nearby.Connections.startAdvertising(googleApiClient, serviceId, appMetadata, 0L, this)
                .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                        Log.d(TAG, "startAdvertising:onResult:" + result);
                        if (result.getStatus().isSuccess()) {
                            Log.d(TAG, "startAdvertising:onResult: SUCCESS");

                        } else {
                            Log.d(TAG, "startAdvertising:onResult: FAILURE ");
                            int statusCode = result.getStatus().getStatusCode();
                            if (statusCode == ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING) {
                                Log.d(TAG, "STATUS_ALREADY_ADVERTISING");
                            } else {
                                Log.d(TAG, "STATE_READY");
                            }
                        }
                    }
                });
    }

    @Override
    public void onConnectionRequest(final String endpointId, String deviceId, String endpointName, byte[] payload) {
        Log.d(TAG, "onConnectionRequest");

        Nearby.Connections.acceptConnectionRequest(googleApiClient, endpointId, payload, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "acceptConnectionRequest: SUCCESS");

                        } else {
                            Log.d(TAG, "acceptConnectionRequest: FAILURE");
                        }
                    }
                });
    }

    @Override
    public void onMessageReceived(final String s, final byte[] bytes, final boolean b) {
        Log.d(TAG, "onMessageReceived");
        double frequency = toDouble(bytes);
        if (frequency == -1) {
            view.stopPlayingNote();
            return;
        }
        view.playNote(frequency);
    }

    private static double toDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    @Override
    public void onDisconnected(final String s) {

    }

    @Override
    public void detachView() {
        this.view = null;
        googleApiClient.disconnect();
    }

    @Override
    public void attachView(final PianoContract.View view) {
        this.view = view;
        googleApiClient.connect();
    }
}
