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
    try {
      Uri uri = Uri.parse(url);
      _mediaPlayer.reset();
      _mediaPlayer.setDataSource(_reactContext, uri);
      _mediaPlayer.prepareAsync();
      _isBuffering = true;

      Log.d(NAME, "==> play: " + url);

    } catch (Exception ex) {
      ex.printStackTrace();
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
  }

  @ReactMethod
  public void resume() {
    if (_isPaused) {
        _mediaPlayer.start();
    }

    Log.d(NAME, "==> resume");
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
  public void pause()
  {
    if (isMusicPlaying()) {
        _mediaPlayer.pause();
        _isPaused = true;
    }
    Log.d(NAME, "==> pause");
  }

  @ReactMethod
  public void seekToTime(Integer time)
  {
    if (isMusicPlaying() || _isPaused) {
        _mediaPlayer.seekTo(time);
    }

    Log.d(NAME, "==> seekToTime " + time);
  }

  @ReactMethod
  public void getDuration(Callback cb)
  {
    Integer duration = 0;
    if (isMusicPlaying() || _isPaused) {
        duration  = _mediaPlayer.getCurrentPosition();
    }
    cb.invoke(null, duration);
    Log.d(NAME, "==> getDuration = " + duration);
  }

  @ReactMethod
  public void getProgress(Callback cb)
  {
    Integer duration = 0;
    if (isMusicPlaying() || _isPaused) {
        duration  = _mediaPlayer.getCurrentPosition();
    }
    cb.invoke(null, duration);
    Log.d(NAME, "==> getProgress = " + duration);
  }

  @ReactMethod
  public void getState(Callback cb)
  {
    if (_mediaPlayer == null) {
      return;
    }
    Log.d(NAME, "==> getState");
  }


  private void startPlaying() {
      _mediaPlayer.start();
      _isPaused = false;
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
        Log.d(NAME, "==> onError");
        return false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
     _buffering = i;
        if (i == 100)
            _isBuffering = false;
        else
            _isBuffering = true;

      Log.d(NAME, "==> onBufferingUpdate");
    }


  private void sendEvent(@Nullable WritableMap params) {
    _reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit("StreamingKitEvent", params);
  }

}
