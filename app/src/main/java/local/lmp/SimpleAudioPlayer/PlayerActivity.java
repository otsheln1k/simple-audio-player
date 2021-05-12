package local.lmp.SimpleAudioPlayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

public class PlayerActivity extends AppCompatActivity {

    private PlayerService m_service = null;
    private ServiceConnection m_conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.Binder binder = (PlayerService.Binder)service;
            m_service = binder.service();

            if (m_uriToSend != null) {
                m_service.setUri(m_uriToSend);
                m_uriToSend = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            m_service = null;
        }
    };

    private Uri m_uriToSend = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Intent intent = getIntent();
        String suri = intent.getStringExtra("uri");
        if (suri != null) {
            m_uriToSend = Uri.parse(suri);
        }

        Intent srvIntent = new Intent(this, PlayerService.class);
        startService(srvIntent);
        bindService(srvIntent, m_conn, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(m_conn);
    }

    public void stopPlayback(View view) {
        m_service.stop();
        finish();
    }

    public void pausePlayback(View view) {
        m_service.pause();
    }

    public void resumePlayback(View view) {
        m_service.resume();
    }
}
