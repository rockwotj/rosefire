FirebaseTokenGenerator = require 'firebase-token-generator'
{createOptions, extractOptions, unixTime} = require '../utils'

module.exports = ({app, rose, secrets, engine}) ->

  app.use '/v2/api/auth', (req, res, next) ->
    token = req.body.registryToken
    if !token
      res.jsonError 400, 'Missing registryToken in request'
    else
      decoded = engine.decrypt token
      if decoded == null
        res.jsonError 400, 'Invalid registryToken in request'
        return
      req.body.secret = decoded.s
      req.body.admin = decoded.a
      req.body.options = extractOptions decoded.o
      next()
      
  app.use '/v2/api/auth', (req, res, next) ->
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

  app.use '/v2/api/auth', (req, res, next) ->
    if req.body.options?.expires
      req.body.expires = unixTime() + expires
    else
      req.body.expires = false
    next()

  app.post '/v2/api/auth', (req, res) ->
    {username, secret, group, expires} = req.body
    tokenGenerator = new FirebaseTokenGenerator secret
    tokenData =
      uid: username
      provider: 'rose-hulman'
    tokenData.group = group if group
    tokenOptions = {}
    tokenOptions.expires = expires if expires
    token = tokenGenerator.createToken tokenData, expires
    res.json {username, token}

  app.use '/v2/api/register', (req, res, next) ->
    {secret, options} = req.body
    req.body.options = createOptions options
    if !secret
      res.jsonError 400, 'Missing secret in request'
    else
      next()

  app.post '/v2/api/register', (req, res) ->
    {options, username, secret} = req.body
    tokenData =
      a: username
      s: secret
    tokenData.o = options if options
    registryToken = engine.encrypt tokenData
    console.log "Generated v2 registryToken for #{username}"
    res.json
      registryToken: registryToken
      username: username
      timestamp: unixTime()
    
