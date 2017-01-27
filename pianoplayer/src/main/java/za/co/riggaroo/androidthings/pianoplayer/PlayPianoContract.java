package za.co.riggaroo.androidthings.pianoplayer;


interface PlayPianoContract {

    interface View {

        void showConnectedToMessage(String endpointName);

        void showApiNotConnected();
    }

    interface Presenter {

        void notePlayed(int noteNumber);

        void noteStopped(int note);

        void attachView(View view);

        void detachView();
    }
}
