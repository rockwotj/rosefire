var ActiveDirectory = require('activedirectory');
var FirebaseTokenGenerator = require("firebase-token-generator");
var Express = require('express');
var BodyParser = require('body-parser');
var moment = require('moment');

var ldapConfig = {url: 'ldap://rose-hulman.edu:389'}
var ad = new ActiveDirectory(ldapConfig);

var extractEmailUsername = function(email) {
  var emailSplit = email.split("@");
  return emailSplit[0];
}

var app = Express();

app.use(BodyParser.json());
app.use(BodyParser.urlencoded({ extended: false }));

app.use(function (req, res, next) {
  var email = req.body.email;
  var password = req.body.password;
  var secret = req.body.secret;
  if(!email || !password ) {
    res.status(400).json({error: {message: 'Missing email or password in request'}, status: 400});
  } else if (!secret) {
    res.status(400).json({error: {message: 'Missing firebase secret in request'}, status: 400});
  } else {
    next();
  }
});

app.post('/api/auth', function(req, res) {
  var email = req.body.email;
  var password = req.body.password;
  var secret = req.body.secret;
  var tokenOptions = req.body.options;
  ad.authenticate(email, password, function(err, auth) {
    if (err || !auth) {
      console.log(email + " failed authentication!");
      res.status(400).json({error: err.toString(), status: 400});
      return; 
    }
    var tokenGenerator = new FirebaseTokenGenerator(secret); 
    var username = extractEmailUsername(email);
    console.log(username + " has authenticated!");
    var tokenData = {
      uid: username, 
      email: email, 
      domain: "rose-hulman.edu", 
      timestamp: moment().format()
    };
    var token = tokenGenerator.createToken(tokenData, tokenOptions);
    console.log("Generated token: ");
    console.log(token);
    res.json({token: token, timestamp: tokenData.timestamp, username: tokenData.uid});
  });

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

