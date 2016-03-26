
var reportError = function(msg) {
  document.getElementById('password-input').classList.add('has-error');
  document.getElementById('error').innerHTML = msg;
};

var loginResult = function() {
  let submitButton = document.getElementById('submit');
  if (this.status === 200) {
      let response = JSON.parse(this.responseText);
      submitButton.classList.remove('btn-primary');
      submitButton.classList.add('btn-success');
      submitButton.innerHTML = `${response.username} logged in`;
      window.setTimeout(loggedIntoRosefire(response.token), 1000);
  } else {
    console.log(this.status);
    try {
      let response = JSON.parse(this.responseText);
      reportError(response.error);
    } catch (e) {
      reportError("Could not reach server");
    }
    submitButton.disabled = false;
  }
}

var handleLogin = function(e) {
  if (e.preventDefault) e.preventDefault();
  document.getElementById('submit').disabled = true;
  let username = document.getElementById('username').value;
  let password = document.getElementById('password').value;
  console.log(`${username} to login`);
  let request = new XMLHttpRequest();
  request.onload = loginResult;
  request.open('POST', '/v2/api/auth', true);
  request.setRequestHeader("Content-Type", "application/json");
  let data = JSON.stringify({
    email: `${username}@rose-hulman.edu`,
    password: password,
    registryToken: registryToken
  });
  request.send(data);
  return false;
};

var form = document.getElementById('signin-form');
if (form.attachEvent) {
    form.attachEvent("submit", handleLogin);
} else {
    form.addEventListener("submit", handleLogin);
}
