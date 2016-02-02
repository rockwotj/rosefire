(function(exports) {
  var createCORSRequest = function(method, url) {
    var xhr = new XMLHttpRequest();
    if ("withCredentials" in xhr) {
      xhr.open(method, url, true);
    } else if (typeof XDomainRequest != "undefined") {
      xhr = new XDomainRequest();
      xhr.open(method, url);
    } else {
      // Otherwise, CORS is not supported by the browser.
      xhr = null;
    }
    return xhr;
  };
  var getRosefireToken = function(data, callback) {
    xhr = createCORSRequest('POST', 'https://rosefire.csse.rose-hulman.edu/api/auth');
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
    xhr.onerror = function() {
      callback(new Error("Error making request to Rosefire"));
    };
    xhr.send(JSON.stringify(data));
  };
  if (typeof Firebase === "undefined") {
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
