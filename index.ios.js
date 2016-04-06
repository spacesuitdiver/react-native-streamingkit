'use strict';

import { NativeModules, DeviceEventEmitter } from 'react-native';
const RNStreamingKitManager = NativeModules.RNStreamingKitManager


var listeners = {};

module.exports = {
  play: function (url) {
    RNStreamingKitManager.play(url);
  },

  queue: function (url) {
    RNStreamingKitManager.queue(url);
  },  

  resume: function () {
    RNStreamingKitManager.resume();
  },  

  pause: function () {
    RNStreamingKitManager.pause();
  },  

  stop: function () {
    RNStreamingKitManager.stop();
  },  

  clearQueue: function () {
    RNStreamingKitManager.clearQueue();
  },  

  addListener: function(cb) {
    listeners[cb] = DeviceEventEmitter.addListener('StreamingKitEvent', cb);
  },

  removeListener(cb) {
    if (!listeners[cb]) {
      return;
    }
    listeners[cb].remove();
    listeners[cb] = null;
  }
};