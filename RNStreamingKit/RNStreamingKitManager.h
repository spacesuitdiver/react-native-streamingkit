//
//  RNStreamingKitManager.h
//  RNStreamingKitManager
//
//  Created by Chris LeBlanc on 4/4/16.
//  Copyright © 2016 Clever Lever. All rights reserved.
//

#import <React/RCTBridge.h>
#import <React/RCTBridgeModule.h>

#import "STKAudioPlayer.h"

@interface RNStreamingKitManager : NSObject <RCTBridgeModule, STKAudioPlayerDelegate>

@property (nonatomic, strong) STKAudioPlayer *audioPlayer;
@property (nonatomic, readwrite) BOOL isPlayingWithOthers;


- (void)queue: (NSString*)url;
- (void)play: (NSString*)url;
- (void)resume;
- (void)pause;
- (void)stop;
- (void)clearQueue;
- (void)seekToTime: (double)time;
- (void)getProgress: (RCTResponseSenderBlock) cb;
- (void)getDuration: (RCTResponseSenderBlock) cb;

@end
