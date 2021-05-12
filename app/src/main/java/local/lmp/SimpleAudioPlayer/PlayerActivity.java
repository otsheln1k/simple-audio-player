package local.lmp.SimpleAudioPlayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class PlayerActivity extends AppCompatActivity implements PlayerService.OnPlaybackStateChanged, PlayerService.OnPlaybackPositionChanged {

    private PlayerService m_service = null;
    private final ServiceConnection m_conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.Binder binder = (PlayerService.Binder)service;
            m_service = binder.service();
            bindHandlers();

            if (m_uriToSend != null) {
                m_service.setUri(m_uriToSend);
                m_uriToSend = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            unbindHandlers();
            m_service = null;
        }
    };

    private Uri m_uriToSend = null;

    private void bindHandlers() {
        m_service.addPlaybackStateListener(this);
        m_service.addPlaybackPositionListener(this);
    }

    private void unbindHandlers() {
        m_service.removePlaybackStateListener(this);
        m_service.removePlaybackPositionListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Intent intent = getIntent();
        m_uriToSend = intent.getData();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent srvIntent = new Intent(this, PlayerService.class);
        startService(srvIntent);
        bindService(srvIntent, m_conn, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (m_service != null) {
            unbindHandlers();
            unbindService(m_conn);
        }
    }

    public void stopPlayback(View view) {
        m_service.stop();
    }

    public void pausePlayback(View view) {
        m_service.pause();
    }

    public void resumePlayback(View view) {
        m_service.resume();
    }

    public void restartTrack(View view) {
        m_service.seekTo(0);
    }

    private void updatePosition(int msec) {
        TextView progressview = findViewById(R.id.progress);
        int sec = (msec + 500) / 1000;
        int total = (m_service.getDuration() + 500) / 1000;
        progressview.setText(String.format(
                Locale.getDefault(),
                "%d:%02d / %d:%02d",
                sec / 60, sec % 60, total / 60, total % 60));
    }

    @Override
    public void onPlaybackStateChanged(PlayerService ps,
                                       PlayerService.PlaybackState st) {
        TextView titleview = findViewById(R.id.songtitle);
        titleview.setText(ps.title());
        switch (st) {
            case STOPPED:
                finish();
                break;
            case PLAYING:
                findViewById(R.id.pausebutton).setEnabled(true);
                findViewById(R.id.resumebutton).setEnabled(false);
                break;
            case PAUSED:
                findViewById(R.id.pausebutton).setEnabled(false);
                findViewById(R.id.resumebutton).setEnabled(true);
                break;
        }
    }

    @Override
    public void onPlaybackPositionChanged(PlayerService ps, int msec) {
        updatePosition(msec);
    }
}
