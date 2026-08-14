package com.example.tvplayer;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackState;

public class PlayerManager {
    private static PlayerManager instance;
    private ExoPlayer player;
    private String currentUrl;

    private PlayerManager() {}

    public static synchronized PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    public void setPlayer(ExoPlayer player, String url) {
        this.player = player;
        this.currentUrl = url;
    }

    public ExoPlayer getPlayer() { return player; }
    public String getCurrentUrl() { return currentUrl; }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public void playPause() {
        if (player != null) {
            if (player.isPlaying()) player.pause();
            else player.play();
        }
    }

    public void seekForward(long millis) {
        if (player != null) {
            long newPos = player.getCurrentPosition() + millis;
            if (newPos > player.getDuration()) newPos = player.getDuration();
            player.seekTo(newPos);
        }
    }

    public void seekBackward(long millis) {
        if (player != null) {
            long newPos = player.getCurrentPosition() - millis;
            if (newPos < 0) newPos = 0;
            player.seekTo(newPos);
        }
    }

    public void seekTo(long position) {
        if (player != null) player.seekTo(position);
    }

    public long getCurrentPosition() {
        return player == null ? 0 : player.getCurrentPosition();
    }

    public long getDuration() {
        return player == null ? 0 : player.getDuration();
    }

    public int getPlaybackState() {
        return player == null ? PlaybackState.STATE_IDLE : player.getPlaybackState();
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}