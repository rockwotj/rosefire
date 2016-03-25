/* Rosefire registration */

registerUser = function() {
  var payload = {
    email: $('#email').val() + "@rose-hulman.edu",
    password: $('#password').val(),
    secret: $('#secret').val()
  };
  var endpoint = '/v2/api/register/';
  var successAlert = $('.alert.alert-success');
  var errorAlert = $('.alert.alert-danger');
  successAlert.hide();
  errorAlert.hide();
  $.ajax({
    type: "POST",
    url: endpoint,
    data: payload,
    dataType: 'json'
  })
  .done(function(data) {
    var msg = "<p>Token:</p><p><textarea class='form-control copyable' rows='3'>" + data.registryToken + "</textarea></p><p> <button class='btn btn-default copier'>Copy</button> Created for " + data.username + " at " + new Date(data.timestamp * 1000) + ".</p><p>Please save this and use this when your app makes requests to the /api/auth/ endpoint.</p>";
    successAlert.html(msg);
    successAlert.show();
    $('.copier').click(function(event) {
      var copyTextarea = $('.copyable');
      copyTextarea.select();
      $('.copier').removeClass("btn-default");
      try {
        var successful = document.execCommand('copy');
        var msg = successful ? 'successful' : 'unsuccessful';
        console.log('Copying text command was ' + msg);
        if (successful) {
          $('.copier').html("Copied!");
          $('.copier').addClass("btn-success");
        }
      } catch (err) {
        console.log('Oops, unable to copy');
        $('.copier').html("Failed!");
        $('.copier').addClass("btn-danger");
        $('.copier').prop("disabled",true);
      }
    });
  })
  .fail(function(jq) {
    var error = jq.responseJSON.error;
    errorAlert.html("<p><b>Error:</b> " + error + "</p>");
    errorAlert.show();
  })
  .always(function() {
    $('#password').val('');
    $('#secret').val('');
    $(document).scrollTop($(document).height());
  });
  return false;
};
