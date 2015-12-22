(function(exports) {
  var getRosefireToken = function(data, callback) {
    var xhr = new XMLHttpRequest();
    xhr.open('POST', 'https://rosefire.csse.rose-hulman.edu/api/auth');
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.onload = function() {
      if (xhr.status === 200) {
        var response = JSON.parse(xhr.responseText);
        callback(null, response.token);
      } else if (xhr.responseText) {
        callback(new Error(xhr.responseText));
      } else {
        callback(new Error("Error authenticating"));
      }
    };
    xhr.send(JSON.stringify(data));
  };
  if (Firebase) {
    Firebase.prototype.authWithRoseHulman = function(registryToken, email, password, options, callback) {
      var data = {
        registryToken: registryToken,
        email: email,
        password: password
      };
      if (callback) {
        data.options = options;
      } else {
        callback = options;
      }
      getRosefireToken(data, function(err, token) {
        if (err) {
          callback(err);
        } else {
          this.authWithCustomToken(token, callback);
        }          
      }.bind(this));
    };
  }
  exports.Rosefire = {};
  exports.Rosefire.getToken = getRosefireToken;
})(this);
