var ActiveDirectory = require('activedirectory');
var FirebaseTokenGenerator = require("firebase-token-generator");
var Express = require('express');
var BodyParser = require('body-parser');
var moment = require('moment');
var jwt = require('jsonwebtoken');
var fs = require('fs');

var ldapConfig = {
  url: 'ldap://rose-hulman.edu:389',
  baseDN: 'dc=rose-hulman,dc=edu'
};
var rose = new ActiveDirectory(ldapConfig);

var secretsFile = process.env.SECRETS_FILE || 'secrets.json';

var secrets = JSON.parse(fs.readFileSync(secretsFile));

var extractEmailUsername = function(email) {
  var emailSplit = email.split("@");
  return emailSplit[0];
};

var app = Express();

app.get('/', function(req, res) {
  res.sendFile(__dirname + "/public/index.html");    
});

app.use(BodyParser.json());
app.use(BodyParser.urlencoded({extended: false}));

app.use(function (req, res, next) {
  var email = req.body.email;
  var password = req.body.password;
  if (!email || !password ) {
    res.status(400).json({error: 'Missing email or password in request', status: 400});
  } else {
    rose.authenticate(email, password, function (err, auth) {
      if (err || !auth) {
        console.log(email + " failed authentication!");
        res.status(400).json({error: "Invalid Rose-Hulman credentials", status: 400});
        return; 
      }
      next();
    });
  }
});

app.use(function(req, res, next) {
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Methods", "POST, OPTIONS");
    res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
    next();
});

app.use('/api/auth', function (req, res, next) {
  var token = req.body.registryToken;
  if (!token) {
    res.status(400).json({error: 'Missing registryToken in request', status: 400});
  } else {
    jwt.verify(token, secrets.key, secrets, function (err, decoded) {
      if (err) {
        res.status(400).json({error: 'Invalid registryToken in request', status: 400});
        return;
      }
      console.log("Registry token successfully decoded for " + decoded.admin + "'s app");
      req.body.secret = decoded.secret;
      req.body.admin = decoded.admin;
      next();
    });
  }
});

app.post('/api/auth', function (req, res) {
  var email = req.body.email;
  var username = extractEmailUsername(email);
  var admin = req.body.admin;
  var isAdmin = username === admin;
  var password = req.body.password;
  var secret = req.body.secret;
  var tokenOptions = req.body.options || {};
  tokenOptions.debug = false;
  if (isAdmin && !tokenOptions.hasOwnProperty("admin")) {
    tokenOptions.admin = true;
  } else if (!isAdmin) {
    tokenOptions.admin = false;
  }
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
    res.status(400).json({error: 'Missing firebase secret in request', status: 400});
  } else {
    next();
  }
});

app.post('/api/register', function (req, res, next) {
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

