package local.lmp.SimpleAudioPlayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_AUDIO_FILE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void runPicker(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        startActivityForResult(intent, REQUEST_PICK_AUDIO_FILE);
    }

    @Override
    public void onActivityResult(int requestCode,
                                 int resultCode,
                                 Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_PICK_AUDIO_FILE:
                if (resultCode == RESULT_CANCELED) {
                    break;
                }
                EditText pathedit = findViewById(R.id.pathedit);
                pathedit.setText(data.getDataString());
                break;
            default:
                break;
        }
    }

    public void play(View view) {
        Intent intent = new Intent(this, PlayerActivity.class);
        EditText pathedit = findViewById(R.id.pathedit);
        intent.putExtra("uri", pathedit.getText().toString());
        startActivity(intent);
    }
}
