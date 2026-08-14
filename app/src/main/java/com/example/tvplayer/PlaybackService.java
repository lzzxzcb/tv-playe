package com.example.tvplayer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import java.io.IOException;
import java.util.Map;

public class PlaybackService extends Service {
    private WebServer server;

    @Override
    public void onCreate() {
        super.onCreate();
        server = new WebServer();
        try {
            server.start(8080);
            Log.i("PlaybackService", "HTTP server started on port 8080");
        } catch (IOException e) {
            Log.e("PlaybackService", "Failed to start server", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
    }

    private class WebServer extends NanoHTTPD {
        public WebServer() { super(8080); }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            Map<String, String> params = session.getParms();

            if ("/".equals(uri)) {
                return newFixedLengthResponse(Status.OK, "text/html; charset=utf-8", getManagementHtml());
            }

            if ("/play".equals(uri)) {
                String url = params.get("url");
                if (url != null && !url.isEmpty()) {
                    Intent intent = new Intent(PlaybackService.this, MainActivity.class);
                    intent.putExtra("url", url);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return newFixedLengthResponse(Status.OK, "text/plain", "OK - Playing: " + url);
                } else {
                    return newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Missing url parameter");
                }
            }

            if ("/control".equals(uri)) {
                String action = params.get("action");
                if (action == null)
                    return newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Missing action");
                PlayerManager pm = PlayerManager.getInstance();
                switch (action) {
                    case "playpause": pm.playPause(); break;
                    case "forward": pm.seekForward(10000); break;
                    case "backward": pm.seekBackward(10000); break;
                    case "seek":
                        String posStr = params.get("position");
                        if (posStr != null) {
                            try { pm.seekTo(Long.parseLong(posStr)); }
                            catch (NumberFormatException e) { return newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Invalid position"); }
                        }
                        break;
                    default: return newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Unknown action");
                }
                return newFixedLengthResponse(Status.OK, "text/plain", "OK");
            }

            if ("/status".equals(uri)) {
                PlayerManager pm = PlayerManager.getInstance();
                long position = pm.getCurrentPosition();
                long duration = pm.getDuration();
                boolean playing = pm.isPlaying();
                String url = pm.getCurrentUrl();
                String json = String.format(
                        "{\"position\":%d,\"duration\":%d,\"playing\":%b,\"url\":\"%s\"}",
                        position, duration, playing, url != null ? url : ""
                );
                return newFixedLengthResponse(Status.OK, "application/json", json);
            }

            return newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Not found");
        }

        private String getManagementHtml() {
            return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>TV播放器控制台</title>
                <style>
                    body { font-family: Arial, sans-serif; background: #222; color: #fff; padding: 20px; max-width: 800px; margin: 0 auto; }
                    h1 { text-align: center; color: #4CAF50; }
                    .status-bar { background: #333; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
                    .progress-container { width: 100%; background: #555; height: 20px; border-radius: 10px; margin: 10px 0; cursor: pointer; }
                    .progress-bar { height: 20px; width: 0%; background: #4CAF50; border-radius: 10px; }
                    .info { display: flex; justify-content: space-between; font-size: 14px; margin: 5px 0; }
                    .controls { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin: 20px 0; }
                    .btn { background: #4CAF50; border: none; color: white; padding: 12px 24px; border-radius: 6px; cursor: pointer; font-size: 16px; min-width: 80px; }
                    .btn:hover { background: #45a049; }
                    .btn-secondary { background: #555; }
                    .btn-secondary:hover { background: #666; }
                    .url-form { margin-top: 20px; display: flex; gap: 10px; flex-wrap: wrap; }
                    .url-form input[type="text"] { flex: 1; padding: 12px; border-radius: 6px; border: none; font-size: 16px; }
                    .url-form input[type="submit"] { background: #008CBA; border: none; color: white; padding: 12px 24px; border-radius: 6px; cursor: pointer; font-size: 16px; }
                    .url-form input[type="submit"]:hover { background: #0077A3; }
                    .playing-url { word-break: break-all; background: #333; padding: 8px; border-radius: 4px; margin-top: 10px; font-size: 14px; }
                </style>
            </head>
            <body>
                <h1>📺 TV 播放器控制</h1>
                <div class="status-bar">
                    <div class="info">
                        <span id="timeDisplay">00:00 / 00:00</span>
                        <span id="playStatus">⏸ 暂停</span>
                    </div>
                    <div class="progress-container" id="progressContainer">
                        <div class="progress-bar" id="progressBar"></div>
                    </div>
                    <div class="playing-url" id="urlDisplay">未播放</div>
                </div>
                <div class="controls">
                    <button class="btn btn-secondary" onclick="control('backward')">⏪ -10s</button>
                    <button class="btn" id="playPauseBtn" onclick="control('playpause')">▶ 播放</button>
                    <button class="btn btn-secondary" onclick="control('forward')">⏩ +10s</button>
                </div>
                <div class="url-form">
                    <input type="text" id="newUrl" placeholder="输入视频URL，例如 http://example.com/video.mp4" />
                    <input type="submit" value="播放此URL" onclick="playUrl()" />
                </div>
                <script>
                    function control(action, value) {
                        let url = '/control?action=' + action;
                        if (value !== undefined) url += '&position=' + value;
                        fetch(url).then(r => r.text()).then(console.log).catch(console.error);
                    }
                    function playUrl() {
                        const url = document.getElementById('newUrl').value.trim();
                        if (!url) return alert('请输入URL');
                        fetch('/play?url=' + encodeURIComponent(url)).then(r => r.text()).then(console.log).catch(console.error);
                    }
                    function updateStatus() {
                        fetch('/status')
                            .then(r => r.json())
                            .then(data => {
                                const pos = data.position || 0;
                                const dur = data.duration || 0;
                                const playing = data.playing;
                                const url = data.url || '';
                                document.getElementById('timeDisplay').textContent = formatTime(pos) + ' / ' + formatTime(dur);
                                const pct = dur > 0 ? (pos / dur * 100) : 0;
                                document.getElementById('progressBar').style.width = pct + '%';
                                const statusText = playing ? '▶ 播放中' : '⏸ 暂停';
                                document.getElementById('playStatus').textContent = statusText;
                                document.getElementById('playPauseBtn').textContent = playing ? '⏸ 暂停' : '▶ 播放';
                                document.getElementById('urlDisplay').textContent = url ? '当前播放: ' + url : '未播放';
                            })
                            .catch(console.error);
                    }
                    function formatTime(ms) {
                        if (ms < 0) ms = 0;
                        const totalSec = Math.floor(ms / 1000);
                        const min = Math.floor(totalSec / 60);
                        const sec = totalSec % 60;
                        return String(min).padStart(2,'0') + ':' + String(sec).padStart(2,'0');
                    }
                    document.getElementById('progressContainer').addEventListener('click', function(e) {
                        const rect = this.getBoundingClientRect();
                        const x = e.clientX - rect.left;
                        const pct = x / rect.width;
                        fetch('/status').then(r => r.json()).then(data => {
                            const dur = data.duration || 0;
                            const pos = Math.floor(pct * dur);
                            control('seek', pos);
                        }).catch(console.error);
                    });
                    setInterval(updateStatus, 500);
                    updateStatus();
                </script>
            </body>
            </html>
            """;
        }
    }
}