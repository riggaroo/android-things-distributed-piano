package za.co.riggaroo.androidthings.distributedpiano;


interface PianoContract {

    interface Presenter {

        void detachView();

        void attachView(View view);
    }

    interface View {

        void playNote(double frequency);

        void stopPlayingNote();
    }
}
