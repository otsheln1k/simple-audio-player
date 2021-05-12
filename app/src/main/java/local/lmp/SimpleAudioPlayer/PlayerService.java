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
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;

public class PlayerService extends Service implements MediaPlayer.OnPreparedListener {

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

    private MediaPlayer m_player;
    private Uri m_currentUri;

    public PlayerService() {
        m_player = new MediaPlayer();
        m_player.setOnPreparedListener(this);
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
        stopForeground();
    }

    public void pause() {
        m_player.pause();
    }

    public void resume() {
        m_player.start();
    }

    public void seekTo(long msec) {
        m_player.seekTo((int)msec);  // TODO: do better on Android O +
    }

    public void togglePause() {
        if (m_player.isPlaying()) {
            m_player.pause();
        } else {
            m_player.start();
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
    }
}
