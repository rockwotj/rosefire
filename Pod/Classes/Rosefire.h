//
//  Rosefire.h
//  Pods
//
//  Created by Tyler Rockwood on 10/10/15.
//
//

#import <Foundation/Foundation.h>
#import <Firebase/Firebase.h>


@interface RosefireTokenOptions : NSObject

@property(strong, nonatomic) NSNumber* admin;
@property(strong, nonatomic) NSNumber* notBefore;
@property(strong, nonatomic) NSNumber* expires;

@end

@interface Firebase (FirebaseWithRoseHulmanAuth)
- (void)authWithRoseHulman:(NSString *)registryToken
                     email:(NSString *)email
                  password:(NSString *)password
       withCompletionBlock:(void (^)(NSError *, FAuthData *))block;
- (void)authWithRoseHulman:(NSString *)registryToken
                     email:(NSString *)email
                  password:(NSString *)password
       withCompletionBlock:(void (^)(NSError *, FAuthData *))block
               withOptions:(RosefireTokenOptions *)tokenOptions;
@end
