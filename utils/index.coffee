
module.exports =
  extractEmailUsername: (email) ->
    [username, domain] = email.split '@'
    username.toLowerCase()


