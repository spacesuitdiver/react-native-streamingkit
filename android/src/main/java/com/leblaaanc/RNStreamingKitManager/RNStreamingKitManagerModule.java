package com.leblaaanc.RNStreamingKitManager;

import com.facebook.react.bridge.*;

import android.util.Log;
import java.util.Map;

public class RNStreamingKitManagerModule extends ReactContextBaseJavaModule {
  ReactApplicationContext reactContext;

  public MPRemoteCommandCenterModule(ReactApplicationContext reactContext) {
      super(reactContext);
      this.reactContext = reactContext;
  }

  @Override
  public String getName() {
      return "RNStreamingKitManager";
  }

  @ReactMethod
  public void resume() {
      System.out.println("RNStreamingKitManager : resume ");
  }

  @ReactMethod
  public void play(String url)
  {
      System.out.println("RNStreamingKitManager : play " + url );
  }

  @ReactMethod
  public void stop()
  {
      System.out.println("RNStreamingKitManager : stop " );
  }

  @ReactMethod
  public void clearQueue()
  {
      System.out.println("RNStreamingKitManager : clearQueue " );
  }

  @ReactMethod
  public void queue(String url)
  {
      System.out.println("RNStreamingKitManager : queue " + url );
  }

  @ReactMethod
  public void pause()
  {
      System.out.println("RNStreamingKitManager : pause ");
  }

  @ReactMethod
  public void seekToTime(Double time)
  {
      System.out.println("RNStreamingKitManager : seekToTime " + time );
  }

  @ReactMethod
  public void getDuration(Callback cb)
  {
      System.out.println("RNStreamingKitManager : getDuration ");
  }

  @ReactMethod
  public void getProgress(Callback cb)
  {
      System.out.println("RNStreamingKitManager : getProgress ");
  }

  @ReactMethod
  public void getState(Callback cb)
  {
      System.out.println("RNStreamingKitManager : getState ");
  }


}
