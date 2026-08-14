package com.example.tvplayer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    private PlayerView playerView;
    private TextView infoTextView;
    private ExoPlayer player;
    private PlayerManager playerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.playerView);
        infoTextView = findViewById(R.id.infoTextView);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        playerManager = PlayerManager.getInstance();
        playerManager.setPlayer(player, null);

        String ip = getLocalIpAddress();
        infoTextView.setText("等待视频URL...\n请访问 http://" + ip + ":8080/ 进行控制");

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("url")) {
            String url = intent.getStringExtra("url");
            if (!TextUtils.isEmpty(url)) {
                playUrl(url);
            }
        }
    }

    private void playUrl(String url) {
        infoTextView.setVisibility(View.GONE);
        playerManager.setPlayer(player, url);
        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    playerManager.playPause();
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    playerManager.seekForward(10000);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    playerManager.seekBackward(10000);
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, PlaybackService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerManager.release();
    }
}