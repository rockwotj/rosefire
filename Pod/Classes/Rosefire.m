//
//  Rosefire.m
//  Pods
//
//  Created by Tyler Rockwood on 10/10/15.
//
//

#import "Rosefire.h"


@implementation RosefireTokenOptions

@end

typedef void(^CompletionBlock)(NSData *data,
                               NSError *connectionError);

@interface RosefireConnectionDelegate : NSObject<NSURLConnectionDelegate>

@property(strong, nonatomic) NSMutableData *responseData;
@property(strong, nonatomic) CompletionBlock resultBlock;

@end

@implementation RosefireConnectionDelegate

- (id) initWithCompletionHandler:(void (^)(NSData *data,
                                           NSError *connectionError))handler {
    self = [super init];
    if (self != nil) {
        self.resultBlock = handler;
    }
    return self;
}

- (void)connection:(NSURLConnection *)connection didReceiveAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge {
    if([challenge.protectionSpace.host isEqualToString:@"rosefire.csse.rose-hulman.edu"]) {
        [challenge.sender useCredential: [NSURLCredential credentialForTrust:challenge.protectionSpace.serverTrust] forAuthenticationChallenge:challenge];
    }
}

- (BOOL)connection:(NSURLConnection *)connection canAuthenticateAgainstProtectionSpace:(NSURLProtectionSpace *)protectionSpace {
    return YES;
}

- (void)connection:(NSURLConnection*)connection didReceiveResponse:(NSURLResponse *)response
{
    NSLog(@"Did Receive Response %@", response);
    self.responseData = [[NSMutableData alloc]init];
}
- (void)connection:(NSURLConnection*)connection didReceiveData:(NSData*)data
{
    //NSLog(@"Did Receive Data %@", data);
    [self.responseData appendData:data];
}
- (void)connection:(NSURLConnection*)connection didFailWithError:(NSError*)error
{
    NSLog(@"Did Fail");
    self.resultBlock(nil, error);
}
- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    NSLog(@"Did Finish");
    self.resultBlock(self.responseData, nil);
}

@end

@implementation Firebase (FirebaseWithRoseHulmanAuth)

- (void)reportInvalidCredentials:(void (^)(NSError *, FAuthData*))block {
    NSDictionary *userInfo = @{
                               NSLocalizedDescriptionKey: NSLocalizedString(@"Invalid Rose-Hulman Credentials.", nil),
                               NSLocalizedFailureReasonErrorKey: NSLocalizedString(@"Invalid username/password.", nil),
                               NSLocalizedRecoverySuggestionErrorKey: NSLocalizedString(@"Please check to make sure your credentials are corrent.", nil)
                               };
    NSError *err = [[NSError alloc] initWithDomain:@"rose-hulman.edu" code:400 userInfo:userInfo];
    block(err, nil);
}

- (void)authWithRoseHulman:(NSString *)registryToken
                     email:(NSString *)email
                  password:(NSString *)password
       withCompletionBlock:(void (^)(NSError *, FAuthData *))block {
    [self authWithRoseHulman:registryToken email:email password:password withCompletionBlock:block withOptions:nil];
}

- (void)authWithRoseHulman:(NSString *)registryToken
                     email:(NSString *)email
                  password:(NSString *)password
       withCompletionBlock:(void (^)(NSError *, FAuthData *))block
               withOptions:(RosefireTokenOptions *)tokenOptions {
    
    NSMutableDictionary *params = [NSMutableDictionary
        dictionaryWithDictionary: @{ @"email"    : email,
                                     @"password" : password,
                                     @"registryToken" : registryToken
                                     }];
    
    if (tokenOptions) {
        NSMutableDictionary *options = [[NSMutableDictionary alloc] init];
        if (tokenOptions.admin) {
            [options setObject:@"admin" forKey:tokenOptions.admin];
        }
        if (tokenOptions.expires) {
            [options setObject:@"expires" forKey:tokenOptions.expires];
        }
        if (tokenOptions.notBefore) {
            [options setObject:@"notBefore" forKey:tokenOptions.notBefore];
        }
        [params setObject:@"options" forKey:options];
    }
    
     NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    [request setURL:[NSURL URLWithString: @"https://rosefire.csse.rose-hulman.edu/api/auth"]];

    NSData *jsonData = [NSJSONSerialization  dataWithJSONObject:params options:0 error:nil];
    NSString *jsonString = [[NSString alloc] initWithData:jsonData   encoding:NSUTF8StringEncoding];
    NSString *loginDataLength = [NSString stringWithFormat:@"%lu",(unsigned long)[jsonString length]];
    
    [request setHTTPMethod:@"POST"];
    
    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    [request setValue:loginDataLength forHTTPHeaderField:@"Content-Length"];
    [request setHTTPBody:jsonData];


    
    @try {
        RosefireConnectionDelegate *delegate =[[RosefireConnectionDelegate alloc] initWithCompletionHandler: ^(NSData *_Nullable data, NSError *_Nullable connectionError) {
            if (connectionError) {
                block(connectionError, nil);
            } else {
                NSDictionary *json = [NSJSONSerialization JSONObjectWithData:data options:0 error:&connectionError];
                NSString *token = [json objectForKey:@"token"];
                if (token) {
                    [self authWithCustomToken:token withCompletionBlock:block];
                } else {
                    [self reportInvalidCredentials:block];
                }
            }
        }];
        NSURLConnection *conn = [[NSURLConnection alloc] initWithRequest:request delegate:delegate startImmediately:YES];
        [conn start];
    } @catch (NSException *exception) {
        [self reportInvalidCredentials:block];
    }
    
}
@end

