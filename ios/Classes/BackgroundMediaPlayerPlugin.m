#import "BackgroundMediaPlayerPlugin.h"
#if __has_include(<background_media_player/background_media_player-Swift.h>)
#import <background_media_player/background_media_player-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "background_media_player-Swift.h"
#endif

@implementation BackgroundMediaPlayerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftBackgroundMediaPlayerPlugin registerWithRegistrar:registrar];
}
@end
