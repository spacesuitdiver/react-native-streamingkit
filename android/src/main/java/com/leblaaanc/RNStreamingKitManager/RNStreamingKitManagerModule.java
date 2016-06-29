/*
Missing:
    - PlayerState events : stopped and paused
*/

package com.leblaaanc.RNStreamingKitManager;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.*;

import java.util.Map;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.net.Uri;

import android.os.PowerManager;

import android.media.AudioManager;
import android.media.MediaPlayer;

import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;

public class RNStreamingKitManagerModule extends ReactContextBaseJavaModule implements
MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener {

  final String NAME = "RNStreamingKitManager";
  ReactApplicationContext _reactContext;
  volatile private MediaPlayer _mediaPlayer;

  int _buffering;
  volatile boolean _isPaused;
  volatile boolean _isBuffering;
  AudioManager _mediaAudioManager;
  MediaSessionCompat _mediaSessionCompat;

  public RNStreamingKitManagerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    _reactContext  = reactContext;

    init();
  }

  @Override
  public String getName() {
    return NAME;
  }

  public void init() {
     if (_mediaPlayer != null) {
         return;
       }

       _mediaPlayer = new MediaPlayer();
       _mediaPlayer.setWakeMode(_reactContext, PowerManager.PARTIAL_WAKE_LOCK);
       _mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

       //set listeners
       _mediaPlayer.setOnPreparedListener(this);
       _mediaPlayer.setOnCompletionListener(this);
       _mediaPlayer.setScreenOnWhilePlaying(true);
       _mediaPlayer.setOnErrorListener(this);
       _mediaPlayer.setOnBufferingUpdateListener(this);
   }

  @ReactMethod
  public void play(String url)
  {
    if (_isPaused) {
      startPlaying();
    } else {
     try {

        notifyPlayerStateChange("buffering");
       Uri uri = Uri.parse(url);
       _mediaPlayer.reset();
       _mediaPlayer.setDataSource(_reactContext, uri);
       _mediaPlayer.prepareAsync();
       _isBuffering = true;


     } catch (Exception ex) {
       ex.printStackTrace();
     }
    }
  }

  @ReactMethod
  public void stop()
  {
    if (_mediaPlayer == null) {
        return;
    }
    _mediaPlayer.pause();
    _mediaPlayer.reset();
    _buffering = 0;
    _isBuffering = false;

    Log.d(NAME, "==> stop");
    notifyPlayerStateChange("stopped");
  }

    @ReactMethod
    public void pause()
    {
      if (isMusicPlaying()) {
          _mediaPlayer.pause();
          _isPaused = true;
          Log.d(NAME, "==> paused");
          notifyPlayerStateChange("paused");
      } else {
          Log.d(NAME, "==> pause, media player is not playing");
      }

    }

  @ReactMethod
  public void resume() {
    if (_isPaused) {
        _mediaPlayer.start();
        Log.d(NAME, "==> resume");
    } else {
        Log.d(NAME, "==> resume not called as media is no paused");
    }
  }

  @ReactMethod
  public void clearQueue()
  {
    if (_mediaPlayer == null) {
        return;
    }
    Log.d(NAME, "==> clearQueue() : NOT IMPLEMENTED ");
  }

  @ReactMethod
  public void queue(String url)
  {
    if (_mediaPlayer == null) {
        return;
    }
    Log.d(NAME, "==> queue() : NOT IMPLEMENTED" );
  }

  @ReactMethod
  public void seekToTime(Integer time)
  {
    if (isMusicPlaying() || _isPaused) {
        // time will be in milliseconds
        _mediaPlayer.seekTo(time);
        Log.d(NAME, "==> seekToTime " + time * 1000);
    } else {
        Log.d(NAME, "==> seekToTime no called, media is not playing");
    }
  }

  @ReactMethod
  public void getDuration(Callback cb)
  {
    Integer duration = 0;
    if (isMusicPlaying() || _isPaused) {
        duration  = (int)(_mediaPlayer.getDuration()/1000);
    }
    cb.invoke(null, duration);
    Log.d(NAME, "==> getDuration = " + duration);
  }

  @ReactMethod
  public void getProgress(Callback cb)
  {
    Integer duration = 0;
    if (isMusicPlaying()) {
        duration  = (int)(_mediaPlayer.getCurrentPosition()/1000);
    }
    cb.invoke(null, duration);
    Log.d(NAME, "==> getProgress = " + duration);
  }

  @ReactMethod
  public void getState(Callback cb)
  {
    if (_mediaPlayer == null) {
        cb.invoke(null, "error");
    } else if (_isPaused) {
        cb.invoke(null,  "paused");
    } else if (_isBuffering) {
        cb.invoke(null, "buffering");
    } else {
        cb.invoke(null, "playing");
    }
  }


  private void startPlaying() {
      _mediaPlayer.start();
      _isPaused = false;

      Log.d(NAME, "AudioPlayer is playing");

      notifyPlayerStateChange("playing");
  }

  synchronized public boolean isMusicPlaying() {
      return _mediaPlayer != null && _mediaPlayer.isPlaying();
  }

  synchronized public boolean isMusicPaused() {
      return _mediaPlayer != null && _isPaused;
  }

  synchronized public boolean isMusicBuffering() {
      return _mediaPlayer != null && _isBuffering;
  }


  // MEDIA PLAYER EVENTS
  @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
      Log.d(NAME, "==> onPrepared");
      startPlaying();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
      _buffering = 0;
      _isBuffering = false;
      Log.d(NAME, "==> onCompletion");
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        _buffering = 0;
        _isBuffering = false;
        Log.d(NAME, String.format("AudioPlayer unexpected Error with code %d", i));

        notifyPlayerStateChange("error");

        return false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
     _buffering = i;
        if (i == 100) {
            _isBuffering = false;
            Log.d(NAME, "AudioPlayer finished buffering");
        }
        else {
            _isBuffering = true;
        }

      Log.d(NAME, "==> onBufferingUpdate");
    }


  // Call JS events
  private void notifyPlayerStateChange(String state) {
    WritableMap params = Arguments.createMap();
        params.putString("playerState", state);
        params.putString("type", "playerStateChange");

    sendEvent(params);
  }

  private void notifyFinishedPlaying(String eventType) {
      WritableMap params = Arguments.createMap();
          params.putString("playerState", "playing");
          params.putString("type", eventType);

      sendEvent(params);
  }

  private void sendEvent(@Nullable WritableMap params) {
    Log.d(NAME, String.format("==== Emit ==== %s, %s", params.getString("playerState"), params.getString("type")));
    _reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit("StreamingKitEvent", params);
  }

}
