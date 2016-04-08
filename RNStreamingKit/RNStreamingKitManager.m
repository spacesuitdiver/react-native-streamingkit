//
//  RNStreamingKitManager.m
//  RNStreamingKitManager
//
//  Created by Chris LeBlanc on 4/4/16.
//  Copyright © 2016 Clever Lever. All rights reserved.
//

#import "RNStreamingKitManager.h"

#import "RCTBridge.h"
#import "RCTEventDispatcher.h"
#import "RCTConvert.h"
@import AVFoundation;
@import MediaPlayer;

@implementation RNStreamingKitManager

@synthesize bridge = _bridge;

- (RNStreamingKitManager *)init
{
    self = [super init];
    if (self) {
        self.audioPlayer = [[STKAudioPlayer alloc] initWithOptions:(STKAudioPlayerOptions){}];
        [self.audioPlayer setDelegate:self];
        [self setSharedAudioSessionCategory];
        [self registerAudioInterruptionNotifications];
    }
    
    return self;
}

- (void)dealloc
{
    [self unregisterAudioInterruptionNotifications];
    [self.audioPlayer setDelegate:nil];
}


#pragma mark - Pubic API


RCT_EXPORT_MODULE();


RCT_EXPORT_METHOD(resume)
{
    if (!self.audioPlayer) {
        return;
    }
    
    [self.audioPlayer resume];
    
}

RCT_EXPORT_METHOD(play:(NSString *)url)
{
    if (!self.audioPlayer) {
        return;
    }
    
    [self.audioPlayer play:url];
    
}


RCT_EXPORT_METHOD(stop)
{
    if (!self.audioPlayer) {
        return;
    }
    
    [self.audioPlayer stop];
    
}

RCT_EXPORT_METHOD(clearQueue)
{
    if (!self.audioPlayer) {
        return;
    }
    
    [self.audioPlayer clearQueue];
    
}


RCT_EXPORT_METHOD(queue:(NSString *)url)
{
    if (!self.audioPlayer) {
        return;
    }
    
    [self.audioPlayer queue:url];
    
}

RCT_EXPORT_METHOD(pause)
{
    if (!self.audioPlayer) {
        return;
    } else {
        [self.audioPlayer pause];
    }
}

RCT_EXPORT_METHOD(seekToTime:(double)time)
{
    if (!self.audioPlayer){
        return;
    }
    
    [self.audioPlayer seekToTime:time];
}

RCT_EXPORT_METHOD(getDuration: (RCTResponseSenderBlock) callback)
{
    if (!self.audioPlayer){
        callback(@[[NSNull null]]);
    }

    callback(@[[NSNull null], [NSNumber numberWithDouble:self.audioPlayer.duration]]);
}

RCT_EXPORT_METHOD(getProgress: (RCTResponseSenderBlock) callback)
{
    if (!self.audioPlayer){
        callback(@[[NSNull null]]);
    }

    callback(@[[NSNull null], [NSNumber numberWithDouble:self.audioPlayer.progress]]);
}

RCT_EXPORT_METHOD(getState: (RCTResponseSenderBlock) callback)
{
    if (!self.audioPlayer) {
        callback(@[[NSNull null], @"error"]);
    } else if ([self.audioPlayer state] == STKAudioPlayerStatePlaying) {
        callback(@[[NSNull null], @"playing"]);
    } else if ([self.audioPlayer state] == STKAudioPlayerStateBuffering) {
        callback(@[[NSNull null], @"buffering"]);
    } else {
        callback(@[[NSNull null], @"paused"]);
    }
}

#pragma mark - StreamingKit Audio Player


- (void)audioPlayer:(STKAudioPlayer *)player didStartPlayingQueueItemId:(NSObject *)queueItemId
{
    NSLog(@"AudioPlayer is playing");
}

- (void)audioPlayer:(STKAudioPlayer *)player didFinishPlayingQueueItemId:(NSObject *)queueItemId withReason:(STKAudioPlayerStopReason)stopReason andProgress:(double)progress andDuration:(double)duration
{
    
    switch (stopReason) {
        case STKAudioPlayerStopReasonEof:
            [self.bridge.eventDispatcher sendDeviceEventWithName:@"StreamingKitEvent"
                                                            body:@{@"playerState": @"playing",
                                                                   @"type": @"didFinishPlayingEof"}];
            break;
        case STKAudioPlayerStopReasonUserAction:
            [self.bridge.eventDispatcher sendDeviceEventWithName:@"StreamingKitEvent"
                                                            body:@{@"playerState": @"playing",
                                                                   @"type": @"didFinishPlayingUserAction"}];
            break;
        case STKAudioPlayerStopReasonPendingNext:
            [self.bridge.eventDispatcher sendDeviceEventWithName:@"StreamingKitEvent"
                                                            body:@{@"playerState": @"playing",
                                                                   @"type": @"didFinishPlayingPendingNext"}];
            break;
        case STKAudioPlayerStopReasonDisposed:
            [self.bridge.eventDispatcher sendDeviceEventWithName:@"StreamingKitEvent"
                                                            body:@{@"playerState": @"playing",
                                                                   @"type": @"didFinishPlayingDisposed"}];
        case STKAudioPlayerStopReasonError:
            [self.bridge.eventDispatcher sendDeviceEventWithName:@"StreamingKitEvent"
                                                            body:@{@"playerState": @"playing",
                                                                   @"type": @"didFinishPlayingError"}];
        case STKAudioPlayerStopReasonNone:
            [self.bridge.eventDispatcher sendDeviceEventWithName:@"StreamingKitEvent"
                                                            body:@{@"playerState": @"playing",
                                                                   @"type": @"didFinishPlayingNone"}];
            
    }
}

- (void)audioPlayer:(STKAudioPlayer *)player didFinishBufferingSourceWithQueueItemId:(NSObject *)queueItemId
{
    NSLog(@"AudioPlayer finished buffering");
}

- (void)audioPlayer:(STKAudioPlayer *)player unexpectedError:(STKAudioPlayerErrorCode)errorCode {
    NSLog(@"AudioPlayer unexpected Error with code %d", errorCode);
}

- (void)audioPlayer:(STKAudioPlayer *)player stateChanged:(STKAudioPlayerState)state previousState:(STKAudioPlayerState)previousState
{
    NSLog(@"AudioPlayer state has changed");
    switch (state) {
        case STKAudioPlayerStatePlaying:
            [self.bridge.eventDispatcher sendDeviceEventWithName:@"StreamingKitEvent"
                                                            body:@{@"playerState": @"playing",
                                                                   @"type": @"playerStateChange"}];
            break;
            
        case STKAudioPlayerStatePaused:
            [self.bridge.eventDispatcher sendDeviceEventWithName:@"StreamingKitEvent"
                                                            body:@{@"playerState": @"paused",
                                                                   @"type": @"playerStateChange"}];
            break;
            
        case STKAudioPlayerStateStopped:
            [self.bridge.eventDispatcher sendDeviceEventWithName:@"StreamingKitEvent"
                                                            body:@{@"playerState": @"stopped",
                                                                   @"type": @"playerStateChange"}];
            break;
            
        case STKAudioPlayerStateBuffering:
            [self.bridge.eventDispatcher sendDeviceEventWithName:@"StreamingKitEvent"
                                                            body:@{@"playerState": @"buffering",
                                                                   @"type": @"playerStateChange"}];
            break;
            
        case STKAudioPlayerStateError:
            [self.bridge.eventDispatcher sendDeviceEventWithName:@"StreamingKitEvent"
                                                            body:@{@"playerState": @"error",
                                                                   @"type": @"playerStateChange"}];
            break;
            
        default:
            break;
    }
}


#pragma mark - Audio Session


- (void)setSharedAudioSessionCategory
{
    NSError *categoryError = nil;
    
    // Create shared session and set audio session category allowing background playback
    [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryPlayback error:&categoryError];
    
    if (categoryError) {
        NSLog(@"Error setting category! %@", [categoryError description]);
    }
}

- (void)registerAudioInterruptionNotifications
{
    // Register for audio interrupt notifications
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onAudioInterruption:)
                                                 name:AVAudioSessionInterruptionNotification
                                               object:nil];
    // Register for route change notifications
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onRouteChangeInterruption:)
                                                 name:AVAudioSessionRouteChangeNotification
                                               object:nil];
}

- (void)unregisterAudioInterruptionNotifications
{
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                    name:AVAudioSessionRouteChangeNotification
                                                  object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                    name:AVAudioSessionInterruptionNotification
                                                  object:nil];
}

- (void)onAudioInterruption:(NSNotification *)notification
{
    // Get the user info dictionary
    NSDictionary *interruptionDict = notification.userInfo;
    
    // Get the AVAudioSessionInterruptionTypeKey enum from the dictionary
    NSInteger interuptionType = [[interruptionDict valueForKey:AVAudioSessionInterruptionTypeKey] integerValue];
    
    // Decide what to do based on interruption type
    switch (interuptionType)
    {
        case AVAudioSessionInterruptionTypeBegan:
            NSLog(@"Audio Session Interruption case started.");
            [self.audioPlayer pause];
            break;
            
        case AVAudioSessionInterruptionTypeEnded:
            NSLog(@"Audio Session Interruption case ended.");
            self.isPlayingWithOthers = [[AVAudioSession sharedInstance] isOtherAudioPlaying];
            (self.isPlayingWithOthers) ? [self.audioPlayer stop] : [self.audioPlayer resume];
            break;
            
        default:
            NSLog(@"Audio Session Interruption Notification case default.");
            break;
    }
}

- (void)onRouteChangeInterruption:(NSNotification *)notification
{
    
    NSDictionary *interruptionDict = notification.userInfo;
    NSInteger routeChangeReason = [[interruptionDict valueForKey:AVAudioSessionRouteChangeReasonKey] integerValue];
    
    switch (routeChangeReason)
    {
        case AVAudioSessionRouteChangeReasonUnknown:
            NSLog(@"routeChangeReason : AVAudioSessionRouteChangeReasonUnknown");
            break;
            
        case AVAudioSessionRouteChangeReasonNewDeviceAvailable:
            // A user action (such as plugging in a headset) has made a preferred audio route available.
            NSLog(@"routeChangeReason : AVAudioSessionRouteChangeReasonNewDeviceAvailable");
            break;
            
        case AVAudioSessionRouteChangeReasonOldDeviceUnavailable:
            // The previous audio output path is no longer available.
            [self.audioPlayer stop];
            break;
            
        case AVAudioSessionRouteChangeReasonCategoryChange:
            // The category of the session object changed. Also used when the session is first activated.
            NSLog(@"routeChangeReason : AVAudioSessionRouteChangeReasonCategoryChange"); //AVAudioSessionRouteChangeReasonCategoryChange
            break;
            
        case AVAudioSessionRouteChangeReasonOverride:
            // The output route was overridden by the app.
            NSLog(@"routeChangeReason : AVAudioSessionRouteChangeReasonOverride");
            break;
            
        case AVAudioSessionRouteChangeReasonWakeFromSleep:
            // The route changed when the device woke up from sleep.
            NSLog(@"routeChangeReason : AVAudioSessionRouteChangeReasonWakeFromSleep");
            break;
            
        case AVAudioSessionRouteChangeReasonNoSuitableRouteForCategory:
            // The route changed because no suitable route is now available for the specified category.
            NSLog(@"routeChangeReason : AVAudioSessionRouteChangeReasonNoSuitableRouteForCategory");
            break;
    }
}

@end
