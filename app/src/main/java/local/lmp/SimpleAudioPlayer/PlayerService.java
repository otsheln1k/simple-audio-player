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
    private static final int NOTIF_ID = 1;   // TODO: make app-global resource
    private static final String NOTIF_CHANNEL = "player-service";

    private MediaPlayer m_player;

    public PlayerService() {
        m_player = new MediaPlayer();
        m_player.setOnPreparedListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        goForeground();
        return super.onStartCommand(intent, flags, startId);
    }

    private void goForeground() {
        Intent notifIntent =
                new Intent(this, PlayerActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this, REQ_START_SERVICE, notifIntent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationManager nman =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel chan =
                    new NotificationChannel(
                            NOTIF_CHANNEL,
                            "Player Service",
                            NotificationManager.IMPORTANCE_DEFAULT);
            nman.createNotificationChannel(chan);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, NOTIF_CHANNEL);
        builder.setSmallIcon(R.mipmap.ic_launcher_round);
        builder.setContentTitle("Now playing");
        builder.setContentText("(nothing)");
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentIntent(pendingIntent);

        Notification notif = builder.build();
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
        m_player.prepareAsync();
    }

    public void stop() {
        m_player.stop();
        stopForeground();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        m_player.start();
    }
}
