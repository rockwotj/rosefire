var ActiveDirectory = require('activedirectory');
var FirebaseTokenGenerator = require("firebase-token-generator");
var Express = require('express');
var BodyParser = require('body-parser');
var moment = require('moment');
var jwt = require('jsonwebtoken');
var fs = require('fs');

var ldapConfig = {url: 'ldap://rose-hulman.edu:389'}
var rose = new ActiveDirectory(ldapConfig);

var secrets = JSON.parse(fs.readFileSync('secrets.json'));

var extractEmailUsername = function(email) {
  var emailSplit = email.split("@");
  return emailSplit[0];
};

var app = Express();

app.use(BodyParser.json());
app.use(BodyParser.urlencoded({extended: false}));

app.use(function (req, res, next) {
  var email = req.body.email;
  var password = req.body.password;
  if(!email || !password ) {
    res.status(400).json({error: {message: 'Missing email or password in request'}, status: 400});
  } else {
    rose.authenticate(email, password, function(err, auth) {
      if (err || !auth) {
        console.log(email + " failed authentication!");
        res.status(400).json({error: "Invalid Rose-Hulman credentials", status: 400});
        return; 
      }
      next();
    });
  }
});

app.use('/api/auth', function (req, res, next) {
  var token = req.body.registryToken;
  if (!token) {
    res.status(400).json({error: {message: 'Missing registryToken in request'}, status: 400});
  } else {
    jwt.verify(token, secrets.key, secrets, function(err, decoded) {
      if(err) {
        res.status(400).json({error: {message: 'Bad registryToken in request'}, status: 400});
        return;
      }
      console.log("Registry token successfully decoded");
      console.log(decoded);
      req.body.secret = decoded.admin;
      req.body.admin = decoded.admin;
      next();
    });
  }
});

app.post('/api/auth', function(req, res) {
  var email = req.body.email;
  var username = extractEmailUsername(email);
  var password = req.body.password;
  var secret = req.body.secret;
  var tokenOptions = req.body.options || {};
  tokenOptions.debug = false;
  tokenOptions.admin = admin === username;

  var tokenGenerator = new FirebaseTokenGenerator(secret); 
  var tokenData = {
    uid: username, 
    email: email, 
    domain: "rose-hulman.edu", 
    timestamp: moment().format()
  };
  var token = tokenGenerator.createToken(tokenData, tokenOptions);
  console.log("Generated token authenticating " + username);
  res.json({token: token, timestamp: tokenData.timestamp, username: tokenData.uid});
});

app.use('/api/register', function (req, res, next) {
  var secret = req.body.secret;
  if (!secret) {
    res.status(400).json({error: {message: 'Missing firebase secret in request'}, status: 400});
  } else {
    next();
  }
});

app.post('/api/register', function(req, res, next) {
  var email = req.body.email;
  var username = extractEmailUsername(email);
  var password = req.body.password;
  var secret = req.body.secret;
  var tokenData = {
    admin: username,
    secret: secret,
    timestamp: moment().format()
  };

  var token = jwt.sign(tokenData, secrets.key, secrets);
  console.log("Generated registryToken for " + username);
  res.json({registryToken: token, timestamp: tokenData.timestamp, username: username}); 
});

app.use(function(err, req, res, next) {
  console.error(err.toString());
  res.status(err.status).json({error: err.toString(), status: err.status});
});

var port = 8080;
var ip_address = '127.0.0.1';

var server = app.listen(port, ip_address, function () {
    console.log('Rose Firebase Auth service listening at http://localhost:%s', port);
});

