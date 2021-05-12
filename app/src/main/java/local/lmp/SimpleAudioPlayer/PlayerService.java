package local.lmp.SimpleAudioPlayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;
import java.util.ArrayList;

public class PlayerService
        extends Service
        implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnCompletionListener {

    // TODO: read attached URI:
    // 1. Load file
    // 2. Extract title (or at least a basename)
    // 3. Extract length
    // 4. Start playing
    // 5. Attach callback or something to update time

    public class Binder extends android.os.Binder {
        public PlayerService service() {
            return PlayerService.this;
        }
    }

    private static final int REQ_START_SERVICE = 1;
    private static final int REQ_STOP_PLAYBACK = 1;
    private static final int NOTIF_ID = 1;   // TODO: make app-global resource
    private static final String NOTIF_CHANNEL = "player-service";

    private final MediaPlayer m_player;
    private Uri m_currentUri;

    private boolean m_doReschedule = false;
    private Handler m_handler = new Handler (Looper.getMainLooper());

    public enum PlaybackState {
        STOPPED,
        PLAYING,
        PAUSED,
    }

    public interface OnPlaybackStateChanged {
        void onPlaybackStateChanged(PlayerService ps, PlaybackState st);
    }
    private final ArrayList<OnPlaybackStateChanged> _onPsc = new ArrayList<>();
    PlaybackState _lastState = null;

    public void addPlaybackStateListener(OnPlaybackStateChanged l) {
        _onPsc.add(l);
        if (_lastState != null) {
            l.onPlaybackStateChanged(this, _lastState);
        }
    }

    public void removePlaybackStateListener(OnPlaybackStateChanged l) {
        _onPsc.remove(l);
    }

    private void announceState(PlaybackState s) {
        _lastState = s;
        for (OnPlaybackStateChanged l : _onPsc) {
            l.onPlaybackStateChanged(this, s);
        }
    }

    public interface OnPlaybackPositionChanged {
        void onPlaybackPositionChanged(PlayerService ps, int msec);
    }
    private final ArrayList<OnPlaybackPositionChanged> _onPpc =
            new ArrayList<>();

    public void addPlaybackPositionListener(OnPlaybackPositionChanged l) {
        _onPpc.add(l);
        if (_lastState != null && _lastState != PlaybackState.STOPPED) {
            l.onPlaybackPositionChanged(this, getPosition());
        }
    }

    public void removePlaybackPositionListener(OnPlaybackPositionChanged l) {
        _onPpc.remove(l);
    }

    private void announcePosition(int pos) {
        if (_lastState != null && _lastState != PlaybackState.STOPPED) {
            for (OnPlaybackPositionChanged l : _onPpc) {
                l.onPlaybackPositionChanged(this, pos);
            }
        }
    }

    public PlayerService() {
        m_player = new MediaPlayer();
        m_player.setOnPreparedListener(this);
        m_player.setOnSeekCompleteListener(this);
        m_player.setOnCompletionListener(this);
    }

    private void startReportingPosition() {
        if (m_doReschedule) {
            return;
        }

        announcePosition(getPosition());

        m_doReschedule = true;
        int interval = 300;
        m_handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                announcePosition(getPosition());
                if (m_doReschedule) {
                    m_handler.postDelayed(this, interval);
                }
            }
        }, interval);
    }

    private void stopReportingPosition() {
        m_doReschedule = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        goForeground();
        return super.onStartCommand(intent, flags, startId);
    }

    private Notification makeNotification(String title) {
        PendingIntent srvPintent =
                PendingIntent.getActivity(
                        this, REQ_START_SERVICE,
                        new Intent(this, PlayerActivity.class), 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationManagerCompat nman =
                    NotificationManagerCompat.from(this);

            NotificationChannel chan =
                    new NotificationChannel(
                            NOTIF_CHANNEL,
                            "Player Service",
                            NotificationManager.IMPORTANCE_DEFAULT);
            chan.setImportance(NotificationManager.IMPORTANCE_LOW);
            nman.createNotificationChannel(chan);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, NOTIF_CHANNEL);
        builder.setSmallIcon(R.drawable.ic_baseline_queue_music_24);
        builder.setContentTitle("Now playing");
        builder.setContentText(title);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentIntent(srvPintent);

        androidx.media.app.NotificationCompat.MediaStyle style =
                new androidx.media.app.NotificationCompat.MediaStyle();
        style.setShowActionsInCompactView(0);  // Stop button
        builder.setStyle(style);

        Intent stopIntent = new Intent(this, PlayerBroadcastReceiver.class);
        stopIntent.setAction(PlayerBroadcastReceiver.ACTION_STOP);
        PendingIntent stopPintent =
                PendingIntent.getBroadcast(
                        this, REQ_STOP_PLAYBACK, stopIntent, 0);
        builder.addAction(R.drawable.ic_baseline_stop_24, "Stop", stopPintent);

        return builder.build();
    }

    private void goForeground() {
        Notification notif = makeNotification("(nothing)");
        startForeground(NOTIF_ID, notif);
    }

    private void stopForeground() {
        stopReportingPosition();
        stopForeground(true);
        stopSelf();
    }

    public void setUri(Uri uri) {
        try {
            m_player.setDataSource(getApplicationContext(), uri);
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
            return;
        }
        m_currentUri = uri;
        m_player.prepareAsync();
    }

    public void stop() {
        m_player.stop();
        announceState(PlaybackState.STOPPED);
        stopForeground();
    }

    public void pause() {
        m_player.pause();
        announceState(PlaybackState.PAUSED);
        stopReportingPosition();
    }

    public void resume() {
        m_player.start();
        announceState(PlaybackState.PLAYING);
        startReportingPosition();
    }

    public void seekTo(int msec) {
        m_player.seekTo(msec);
    }

    public int getPosition() {
        return m_player.getCurrentPosition();
    }

    public int getDuration() {
        return m_player.getDuration();
    }

    public void togglePause() {
        if (m_player.isPlaying()) {
            pause();
        } else {
            resume();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        m_player.start();

        String title = m_currentUri.getLastPathSegment();
        Notification notif = makeNotification(title);
        NotificationManagerCompat nman = NotificationManagerCompat.from(this);
        nman.notify(NOTIF_ID, notif);

        announceState(PlaybackState.PLAYING);
        announcePosition(0);

        startReportingPosition();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        announcePosition(getPosition());
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        announceState(PlaybackState.STOPPED);
        stopForeground();
    }
}
