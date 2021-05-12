package local.lmp.SimpleAudioPlayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PlayerBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_RESTART
            = "local.lmp.SimpleAudioPlayer.RESTART";
    public static final String ACTION_STOP
            = "local.lmp.SimpleAudioPlayer.STOP";

    @Override
    public void onReceive(Context context, Intent intent) {
        PlayerService.Binder binder = (PlayerService.Binder)peekService(
                context, new Intent(context, PlayerService.class));
        if (binder == null) {
            return;
        }

        PlayerService srv = binder.service();

        switch (intent.getAction()) {
            case ACTION_STOP:
                srv.stop();
                break;
            case ACTION_RESTART:
                srv.seekTo(0);
                break;
        }
    }
}
