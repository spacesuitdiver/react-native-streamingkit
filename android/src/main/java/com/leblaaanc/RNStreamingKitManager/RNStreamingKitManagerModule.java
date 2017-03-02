/*
JS Events:
    - Audio interrupts
      1. {playerState: 'interruptStart',  type: 'audioSessionInterruption'}
      2. {playerState: 'interruptEnd',    type: 'audioSessionInterruption'}

    - Player State Changes
      1. {playerState: 'playing',     type: 'playerStateChange'}
      2. {playerState: 'paused',      type: 'playerStateChange'}
      3. {playerState: 'stopped',     type: 'playerStateChange'}
      4. {playerState: 'buffering',   type: 'playerStateChange'}
      5. {playerState: 'error',       type: 'playerStateChange'}

    - Player finished events
      1. {playerState: 'playing',     type: 'didFinishPlayingEof'}
Ref:
    - AudioManager Interrupts: https://developer.android.com/training/managing-audio/audio-focus.html
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

public class RNStreamingKitManagerModule extends ReactContextBaseJavaModule implements
MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener {

  final String NAME = "RNStreamingKitManager";
  ReactApplicationContext _reactContext;
  volatile private MediaPlayer _mediaPlayer;

  int _seekToTime = 0;

  volatile boolean _isPaused;
  volatile boolean _wasInterrupted;
  volatile boolean _isBuffering;
  AudioManager _audioManager;

  public RNStreamingKitManagerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    _reactContext  = reactContext;
    init();
    initAudioInterrupts();
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
     /*
     _mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
         @Override
         public void onSeekComplete(MediaPlayer mp) {
             Log.d(NAME," ========> Seek Complete. Current Position: " + mp.getCurrentPosition());
             //mp.start();
         }
     });
     */
 }

  @ReactMethod
  public void play(String url)
  {
    if (url == null && _isPaused) {
      startPlaying();
    } else {
     try {
        notifyPlayerStateChange("buffering");
       _mediaPlayer.reset();
       _mediaPlayer.setDataSource(_reactContext, Uri.parse(url));
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
    Log.d(NAME, "==> stop");
    if (isMusicPlaying()) {
      _mediaPlayer.pause();
    }

    reset();
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
    Log.d(NAME, "==> resume");
    if (!isMusicPlaying()) {
        startPlaying();
    }
  }

  @ReactMethod
  public void clearQueue()
  {
    Log.d(NAME, "==> clearQueue() : NOT IMPLEMENTED ");
  }

  @ReactMethod
  public void queue(String url)
  {
    Log.d(NAME, "==> queue() : NOT IMPLEMENTED" );
  }

  @ReactMethod
  public void seekToTime(Integer time)
  {
    _seekToTime = time * 1000;
    Log.d(NAME, "==> seekToTime " + _seekToTime);
    if (isMusicPlaying() || _isPaused) {
        // time will be in milliseconds
        _mediaPlayer.seekTo(_seekToTime);
        _seekToTime = 0;
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

  void startPlaying() {
      _mediaPlayer.start();
      _isPaused = false;

      if (_seekToTime > 1000) {
        _mediaPlayer.seekTo(_seekToTime);
        _seekToTime = 0;
      }

      notifyPlayerStateChange("playing");
      Log.d(NAME, "AudioPlayer is playing");
  }

  private void reset() {
      _mediaPlayer.reset();
      _isPaused = false;
      _isBuffering = false;
  }

  synchronized public boolean isMusicPlaying() {
    return _mediaPlayer != null && _mediaPlayer.isPlaying();
  }

  // === MEDIA PLAYER LISTENERS ===

  @Override
  public void onPrepared(MediaPlayer mediaPlayer) {
    Log.d(NAME, "==> onPrepared");
    startPlaying();
  }

  @Override
  public void onCompletion(MediaPlayer mediaPlayer) {
    Log.d(NAME, "==> onCompletion");
    _isBuffering = false;
    notifyFinishedPlaying("didFinishPlayingEof");
  }

  @Override
  public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
    Log.d(NAME, String.format("AudioPlayer unexpected Error with code %d", i));
    _isBuffering = false;
    notifyPlayerStateChange("error");
    return false;
  }

  @Override
  public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
    if (i == 100) {
        _isBuffering = false;
        Log.d(NAME, "AudioPlayer finished buffering");
    }
    else {
        _isBuffering = true;
        Log.d(NAME, String.format("==> AudioPlayer buffering %d", i));
    }
  }

  // === AUDIO INTERRUPTS ===

  void initAudioInterrupts() {
    _audioManager = (AudioManager)_reactContext.getSystemService(Context.AUDIO_SERVICE);

    AudioManager.OnAudioFocusChangeListener afChangeListener =
        new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                  case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    Log.d(NAME, "==> Audio Session Interruption case AUDIOFOCUS_LOSS_TRANSIENT.");

                    if (isMusicPlaying())  {
                        _wasInterrupted = true;
                        pause();
                        notifyAudioInterruption("interruptStart");
                    }

                    break;
                  case AudioManager.AUDIOFOCUS_GAIN:
                    Log.d(NAME, "==> Audio Session Interruption case AUDIOFOCUS_GAIN.");
                    if (_wasInterrupted) {
                        _wasInterrupted = false;
                        resume();
                        notifyAudioInterruption("interruptEnd");
                    }
                    break;
                  case AudioManager.AUDIOFOCUS_LOSS:
                    //_audioManager.abandonAudioFocus(afChangeListener);
                    Log.d(NAME, "==> Audio Session Interruption case AUDIOFOCUS_LOSS.");
                    stop();
                    break;
                  default:
                    Log.d(NAME, "==> Audio Session Interruption case default.");
                }
            }
        };

    int result = _audioManager.requestAudioFocus(afChangeListener,
                                     // Use the music stream.
                                     AudioManager.STREAM_MUSIC,
                                     // Request permanent focus.
                                     AudioManager.AUDIOFOCUS_GAIN);
  }

  // ~~~

  // === Call JS events ===

  private void notifyPlayerStateChange(String state) {
    WritableMap params = Arguments.createMap();
        params.putString("playerState", state);
        params.putString("type", "playerStateChange");

    sendEvent(params);
  }

  private void notifyAudioInterruption(String state) {
    WritableMap params = Arguments.createMap();
        params.putString("playerState", state);
        params.putString("type", "audioSessionInterruption");

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
