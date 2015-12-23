//
//  RHViewController.m
//  Rosefire
//
//  Created by rockwotj on 10/10/2015.
//  Copyright (c) 2015 rockwotj. All rights reserved.
//

#import "RHViewController.h"
#import "Firebase.h"
#import "Rosefire.h"

@interface RHViewController ()

@end

@implementation RHViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    Firebase* myFirebaseRef = [[Firebase alloc] initWithUrl:@"https://myproject.firebaseio.com"];
    [myFirebaseRef authWithRoseHulman:@"<REGISTRY_TOKEN>"
                                email:@"rockwotj@rose-hulman.edu"
                             password:@"Pa$sW0rd"
                  withCompletionBlock:^(NSError * err, FAuthData * authData) {
                      if (!err) {
                          // Show logged in UI
                          NSLog(@"Logged in!");
                      } else {
                          // Show login error
                          NSLog(@"Not logged in :(");
                      }
                  }];
	// Do any additional setup after loading the view, typically from a nib.
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
