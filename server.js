'use strict';

var ActiveDirectory = require('activedirectory');
var FirebaseTokenGenerator = require("firebase-token-generator");
var express = require('express');
var BodyParser = require('body-parser');
var corser = require("corser");
var fs = require('fs');
var async = require('async');
var encrypter = require('simple-encryptor');

var ldapConfig = {
  url: 'ldaps://rose-hulman.edu:636',
  baseDN: 'dc=rose-hulman,dc=edu',
  tlsOptions: {
    agent: false,
    rejectUnauthorized: false
  },
  logging: {
    name: 'ActiveDirectory',
    streams: [
      { level: 'error', stream: process.stdout }
    ]
  }
};

var rose = new ActiveDirectory(ldapConfig);

var secretsFile = process.env.SECRETS_FILE || 'secrets.json';

var secrets = JSON.parse(fs.readFileSync(secretsFile));

var engine = encrypter(secrets.key);

var extractEmailUsername = (email) => {
  var [username, domain] = email.split("@");
  return username.toLowerCase();
};

var app = express();

app.use(express.static(__dirname + '/public'));

app.use(BodyParser.json());
app.use(BodyParser.urlencoded({extended: false}));
app.use(corser.create())

app.use((req, res, next) => {
  var email = req.body.email;
  var password = req.body.password;
  if (!email || !password ) {
    res.status(400).json({error: 'Missing email or password', status: 400});
  } else {
    rose.authenticate(email, password, (err, auth) => {
      if (err || !auth) {
        console.log(email + " failed authentication!");
        res.status(401).json({error: "Invalid Rose-Hulman credentials", status: 401});
        return; 
      }
      req.body.username = extractEmailUsername(email);
      next();
    });
  }
});

app.use('/api/auth', (req, res, next) => {
  var token = req.body.registryToken;
  if (!token) {
    res.status(400).json({error: 'Missing registryToken in request', status: 400});
  } else {
    var decoded = engine.decrypt(token);
    if (decoded == null) {
      res.status(400).json({error: 'Invalid registryToken in request', status: 400});
      return;
    }
    console.log("Registry token successfully decoded for " + decoded.admin + "'s app");
    req.body.secret = decoded.secret;
    req.body.admin = decoded.admin;
    next();
  }
});

app.use('/api/auth', (req, res, next) => {
  if (req.body.options && req.body.options.group) {
    var email = req.body.email;
    var username = req.body.username;
    var password = req.body.password;
    var creds = {bindDN: email, bindCredentials: password};
    rose.getGroupMembershipForUser(creds, username, (err, groups) => {
      if (err || !groups) {
        console.log(email + " failed authentication!");
        res.status(400).json({error: "Invalid Rose-Hulman credentials", status: 400});
      } else {
        console.log(groups);
        async.parallel({
          isStudent: (callback) => {
            async.any(groups, (item, cb) => {
              // Is a regular OR international student.
              cb(item.cn.startsWith('Stu') || item.cn === "all-sg");
            }, callback.bind(undefined, null));
          },
          isInstructor: (callback) => {
            async.any(groups, (item, cb) => {
              cb(item.cn === 'Instructor');
            }, callback.bind(undefined, null));
          },
          isSysAdmin: (callback) => {
            async.any(groups, (item, cb) => {
              cb(item.cn === 'CSSE-MA Departmental Admins');
            }, callback.bind(undefined, null));
          }
        }, (err, results) => {
          if (err) {
            res.status(500).json({error: err.toString(), status: 500});
          } else {
            if (results.isSysAdmin) {
              req.body.group = 'SYSADMIN'
            } else if (results.isInstructor) {
              req.body.group = 'INSTRUCTOR';
            } else if (results.isStudent) {
              req.body.group = 'STUDENT';
            } else {
              req.body.group = 'OTHER';
            }
            console.log("Found group to be " + req.body.group + " for user " + username);
            next();
          }
        });
      }
    });
  } else {
    req.body.group = false;
    next();
  }
});

app.post('/api/auth', (req, res) => {
  var email = req.body.email;
  var username = req.body.username;
  var admin = req.body.admin;
  var isAdmin = username === admin;
  var password = req.body.password;
  var secret = req.body.secret;
  var tokenOptions = req.body.options || {};
  delete tokenOptions.debug;
  if (isAdmin && !tokenOptions.hasOwnProperty("admin")) {
    tokenOptions.admin = true;
  } else if (!isAdmin) {
    tokenOptions.admin = false;
  }
  var tokenGenerator = new FirebaseTokenGenerator(secret); 
  var tokenData = {
    uid: username, 
    provider: "rose-hulman"
  };
  if (req.body.group) {
    tokenData.group = req.body.group;
  }
  delete tokenOptions.group;
  var token = tokenGenerator.createToken(tokenData, tokenOptions);
  console.log("Generated token authenticating " + username);
  res.json({token: token, timestamp: tokenData.timestamp, username: tokenData.uid});
});

app.use('/api/register', (req, res, next) => {
  var secret = req.body.secret;
  if (!secret) {
    res.status(400).json({error: 'Missing secret in request', status: 400});
  } else {
    next();
  }
});

app.post('/api/register', (req, res, next) => {
  var email = req.body.email;
  var username = req.body.username;
  var password = req.body.password;
  var secret = req.body.secret;
  var tokenData = {
    admin: username,
    secret: secret,
    timestamp: Math.floor(Date.now() / 1000)
  };
  var token = engine.encrypt(tokenData);
  console.log("Generated registryToken for " + username);
  res.json({registryToken: token, timestamp: tokenData.timestamp, username: username}); 
});

app.use((err, req, res, next) => {
  var msg = err.toString()
  console.error(msg);
  res.status(err.status).json({error: msg, status: err.status});
});

var port = 8080;
var ip_address = '127.0.0.1';

var server = app.listen(port, ip_address, () => {
    console.log('Rose-Hulman Authentication service started on port 8080');
});

