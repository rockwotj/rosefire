FirebaseTokenGenerator = require 'firebase-token-generator'
{extractGroup} = require '../utils'

module.exports = ({app, rose, secrets, engine}) ->

  app.use '/api/auth', (req, res, next) ->
    token = req.body.registryToken
    if !token
      res.jsonError 400, 'Missing registryToken in request'
    else
      decoded = engine.decrypt(token)
      if decoded == null
        res.jsonError 400, 'Invalid registryToken in request'
        return
      console.log 'Registry token successfully decoded for ' + decoded.admin + '\'s app'
      req.body.secret = decoded.secret
      req.body.admin = decoded.admin
      next()
      
  app.use '/api/auth', (req, res, next) ->
    if req.body.options?.group
      {email, username, password} = req.body
      creds =
        bindDN: email
        bindCredentials: password
      rose.getGroupMembershipForUser creds, username, (err, groups) ->
        if err or !groups
          console.log "#{email} failed authentication!"
          res.jsonError 401, 'Invalid Rose-Hulman credentials'
        else
          extractGroup groups, (err, group) ->
            if err
              req.jsonError 500, 'Could not lookup group!'
            req.body.group = group
            console.log "Found group to be #{group} for user #{username}"
            next()
    else
      req.body.group = false
      next()

  app.post '/api/auth', (req, res) ->
    {username, secret, group, options} = req.body
    delete options.debug
    delete options.admin
    delete options.group
    tokenGenerator = new FirebaseTokenGenerator secret
    tokenData =
      uid: username
      provider: 'rose-hulman'
    tokenData.group = group if group
    token = tokenGenerator.createToken tokenData, options
    console.log "Generated token authenticating #{username}"
    res.json
      token: token
      timestamp: Math.floor(Date.now() / 1e3)
      username: username

  app.use '/api/register', (req, res, next) ->
    secret = req.body.secret
    if !secret
      res.jsonError 400, 'Missing secret in request'
    else
      next()

  app.post '/api/register', (req, res, next) ->
    {username, secret} = req.body
    tokenData =
      admin: username
      secret: secret
      timestamp: Math.floor(Date.now() / 1000)
    token = engine.encrypt(tokenData)
    console.log "Generated registryToken for #{username}"
    res.json
      registryToken: token
      timestamp: tokenData.timestamp
      username: username

