# Rose-Hulman Firebase Authentication

This is a simple service that authenticates Rose-Hulman students via Kerberos Login and returns a [Firebase Custom Auth Token](https://www.firebase.com/docs/web/guide/login/custom.html).

This README and documentation is a work in progress

## Production Use

Tokens recieved from this service will have the following fields, which can accessed in your firebase rules. More information about this can be found in [Firebase's Custom Auth Documentation](https://www.firebase.com/docs/web/guide/login/custom.html).

```json
uid: String: the Rose username of the user
email: String: the email of the user
domain: "rose-hulman.edu"
timestamp: String: an ISO formatted timestamp of when it was created.
```

### Endpoints

The hostname for this endpoint has yet to be determined, but will be behind Rose-Hulman's Firewall.

#### POST `api/auth` endpoint

A post request to `api/auth` requires the following parameters in the JSON body of the request. The options field and all of that object's fields are completely optional and not required.

```json
{
  "email": "rockwotj@rose-hulman.edu",
  "password": "Pas$w0rd", 
  "secret": "<FIREBASE SECRET HERE>",
  "options": {
    "expires": 12340192830,
    "notBefore": 1234234134,
    "admin": false
  }
}
```

These are the options for the endpoint, more information can be found [here](https://github.com/firebase/firebase-token-generator-node#token-options).

*expires*: A timestamp of when the token is invalid.

*notBefore*: A timestamp of when the token should start being valid.

*admin*: If true, then all security rules are disabled for this user.

And it returns an object that looks like this

```json
{
  "token": "Auth token for rockwotj@rose-hulman.edu",
  "timestamp": "ISO formatted timestamp",
  "username": "rockwotj"
}
```

If there is an error then the response will look like this

```json
{
  "error": "error message",
  "status": "HTTP Status code of the error"
}
```

### Client Libraries

In the near future there will be client libraries available to more easily integrate this into your code.

#### Java

TODO

#### Swift

TODO

#### Javascript

TODO


## Production Setup

This is a simple nodejs app that is reverse proxied by nginx. Here are the deployment instructions:

```
git clone https://github.com/rockwotj/rose-firebase-auth.git/
npm install
./scripts/start
```


### Nginx Configuration 

TODO

