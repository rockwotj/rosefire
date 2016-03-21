FirebaseTokenGenerator = require 'firebase-token-generator'
{verifyOptions} = require '../utils'

module.exports = ({app, rose, secrets, engine}) ->

  app.use '/v2/api/auth', (req, res) ->
    # TODO


  app.use '/v2/api/register', (req, res, next) ->
    {secret, options} = req.body
    req.body.options = verifyOptions options
    if !secret
      res.jsonError 400, 'Missing secret in request'
    else
      next()

  app.post '/v2/api/register', (req, res) ->
    {options, username, secret} = req.body
    tokenData =
      admin: username
      secret: secret
    tokenData.options = options if options
    registryToken = engine.encrypt tokenData
    console.log "Generated v2 registryToken for #{username}"
    res.json
      registryToken: registryToken
      username: username
      timestamp: Math.floor(Date.now() / 1e3)
    
