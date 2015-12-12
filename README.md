# Rose-Hulman Firebase Authentication

![Server](https://img.shields.io/badge/server-v1.0.0-yellow.svg)
[![Android](https://img.shields.io/badge/android-v1.0.6-green.svg)](https://jitpack.io/#rockwotj/rosefire/android-v1.0.6)
![iOS](https://img.shields.io/badge/ios-v1.0.2-blue.svg)

This is a simple service that authenticates Rose-Hulman students via Kerberos Login and returns a [Firebase Custom Auth Token](https://www.firebase.com/docs/web/guide/login/custom.html).

This README and documentation is a work in progress

## TL;DR

Start [here](#client-libraries) if you're a [TL;DR](https://en.wikipedia.org/wiki/Wikipedia:Too_long;_didn%27t_read) kind of person, otherwise, start reading about the endpoints.

Get a registry token from here: [https://rosefire.csse.rose-hulman.edu](https://rosefire.csse.rose-hulman.edu) then skip down to [Client Libraries](https://github.com/rockwotj/rosefire/#client-libraries) to see how to integrate this into your Firebase app.

## Production Use

Tokens recieved from this service will have the following fields, which can accessed in your firebase rules. More information about this can be found in [Firebase's Custom Auth Documentation](https://www.firebase.com/docs/web/guide/login/custom.html).

```json
uid: (String) the Rose username of the user
email: (String) the email of the user
domain: (String) "rose-hulman.edu"
timestamp: (String) an ISO formatted timestamp of when it was created.
```

### Endpoints

The hostname for this service is `rosefire.csse.rose-hulman.edu`, it is available outside Rose-Hulman's firewall. Please only use this software for good.

#### POST `/api/register/`

A post request to `/api/register/` requires the following parameters in the JSON body of the request. The email and password is who the project will be registered under. 

```json
{
  "email": "rockwotj@rose-hulman.edu",
  "password": "Pas$w0rd", 
  "secret": "<FIREBASE SECRET HERE>"
}
```

This will then return a JSON object like the following.

```json
{
  "username": "rockwotj",
  "timestamp": "<ISO formatted timestamp>", 
  "registryToken": "<YOUR REGISTRY TOKEN HERE>"
}
```

Note: When the person who the project is registered under is authenticated via the `/api/auth/` endpoint, they have full read-write access to the entire firebase repo by default. This can be disabled by explictly setting admin to false in the `/api/auth/` endpoint. No one else is allowed to have admin access for security reasons.

#### POST `/api/auth/`

A post request to `/api/auth/` requires the following parameters in the JSON body of the request. The options field and all of that object's fields are completely optional and not required.

```json
{
  "email": "user@rose-hulman.edu",
  "password": "Pas$w0rd", 
  "registryToken": "<REGISTRY TOKEN HERE>",
  "options": {
    "expires": 12340192830,
    "notBefore": 1234234134,
    "admin": false
  }
}
```

These are the options for the endpoint, more information can be found [here](https://github.com/firebase/firebase-token-generator-node#token-options).

* <b>expires</b>: (Integer) A timestamp of when the token is invalid.
* <b>notBefore</b>: (Integer) A timestamp of when the token should start being valid.
* <b>admin</b>: (Boolean) If true, then all security rules are disabled for this user. This can only be true for the user who the token is registred with.

This endpoint returns an object that looks like this

```json
{
  "token": "<Auth token for user@rose-hulman.edu>",
  "timestamp": "<ISO formatted timestamp>",
  "username": "user"
}
```

#### Endpoint Errors

If there is an error during a request for any endpoint then the response will look like this

```json
{
  "error": "error message",
  "status": "HTTP Status code of the error"
}
```

## Client Libraries

There are client libraries available to more easily integrate this into your code or read this if you're a [TL;DR](https://en.wikipedia.org/wiki/Wikipedia:Too_long;_didn%27t_read) kind of person.

### Android

[![Android](https://img.shields.io/badge/android-v1.0.6-green.svg)](https://jitpack.io/#rockwotj/rosefire/android-v1.0.6)

**Step 1:** Add it in your build.gradle at the end of repositories:

```gradle
android {
  repositories {
    maven { url "https://jitpack.io" }
  }
}
```

**Step 2:** Add the dependency in the form:
```gradle
dependencies {
  compile 'com.github.rockwotj:rosefire:android-v1.0.6'
}
```

**Step 3:** Authenticate a Rose-Hulman User with Firebase

```java
Firebase myFirebaseRef = new Firebase("https://myproject.firebaseio.com");
RosefireAuth roseAuth = new RosefireAuth(myFirebaseRef, "<REGISTRY_TOKEN>");
roseAuth.authWithRoseHulman("rockwotj@rose-hulman.edu", "Pa$sW0rd", new Firebase.AuthResultHandler() {
    @Override
    public void onAuthenticated(AuthData authData) {
        // Show logged in UI
    }

    @Override
    public void onAuthenticationError(FirebaseError firebaseError) {
        // Show Login Error
  }
});
```

### iOS

![iOS](https://img.shields.io/badge/ios-v1.0.2-blue.svg)

**Step 1:** For either Objective-C or Swift projects add rosefire as a dependancy in your cocoapods:

```ruby
pod 'Rosefire', :git => 'https://github.com/rockwotj/rosefire.git', :tag => 'ios-v1.0.2'
```

Then run `pod install`

#### Swift Projects

**Step 2:** Import Firebase and Rosefire in your bridging header:

```objc
#import <Firebase/Firebase.h>
#import <Rosefire/Rosefire.h>
```

**Step 3:** Authenticate a Rose-Hulman User with Firebase:

```swift
let myFirebaseRef = Firebase(url: "https://myproject.firebaseio.com")
myFirebaseRef.authWithRoseHulman("<REGISTRY_TOKEN>", email: "rockwotj@rose-hulman.edu", password: "Pa$sW0rd") {
  (err, data) -> Void in
    if err == nil {
      // Show logged in UI
    } else {
      // Show login error
    }
}

```

#### Objective-C Projects

**Step 2:** Import Firebase and Rosefire in the file you're using it
```objc
#import <Firebase/Firebase.h>
#import <Rosefire/Rosefire.h>
```

**Step 3:** Authenticate a Rose-Hulman User with Firebase:

```objc
Firebase* myFirebaseRef = [[Firebase alloc] initWithUrl:@"https://passwordkeeper.firebaseio.com"];
[myFirebaseRef authWithRoseHulman:@"<REGISTRY_TOKEN>"
                            email:@"rockwotj@rose-hulman.edu"
                         password:@"Pa$sW0rd"
              withCompletionBlock:^(NSError * err, FAuthData * authData) {
    if (!err) {
        // Show logged in UI
    } else {
        // Show login error
    }
}];
```

### Javascript

TODO


## Production Setup

This is a simple nodejs app, managed by [The Guv'nor](https://github.com/tableflip/guvnor) that is reverse proxied by nginx. 

### The Guv'nor

Make sure the Guv'nor is set up. See the [latest deployment instructions](https://github.com/tableflip/guvnor#install) for help. Everything should already be set up on the rosefire server.

### Deployment :shipit:

To deploy the app, The Gov'nor can do it for you. See [this](https://github.com/tableflip/guvnor/blob/master/docs/apps.md#start-stop-restart-etc) for details. Don't forget to set the SECRETS_FILE environment variable!

Make sure nginx is set up over HTTPS to proxy to localhost:8080. The nginx configuration is included below.

### Secrets File

In order to run this server, a `secrets.json` file is required. At minimum, it must have a 'key' field that will be used as a symmetric key for the JWT that is the registry token. It may also include the following fields: 'subject', 'issuer', and 'audience'.

### Nginx Configuration 

```
server {
       listen         80 default_server;
       listen         [::]:80 default_server;
       server_name    rosefire.csse.rose-hulman.edu;
       return         301 https://$server_name$request_uri;
}

server {
       listen              443 ssl default_server;
       listen              [::]:443 ssl default_server;
       server_name         rosefire.csse.rose-hulman.edu;
       ssl_certificate     /etc/nginx/ssl/nginx.crt;
       ssl_certificate_key /etc/nginx/ssl/nginx.key;

       location / {
            proxy_pass       http://localhost:8080;
            proxy_set_header Host      $host;
            proxy_set_header X-Real-IP $remote_addr;
       }
}

```
