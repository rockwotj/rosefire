'use strict'

ActiveDirectory = require 'activedirectory'
BodyParser = require 'body-parser'
express = require 'express'
corser = require 'corser'
fs = require 'fs'
encrypter = require 'simple-encryptor'

{extractEmailUsername, sendError} = require './utils'
api = require './api'
webviewPages = require './webview/pages'

ldapConfig =
  url: 'ldaps://rose-hulman.edu:636'
  baseDN: 'dc=rose-hulman,dc=edu'
  tlsOptions:
    agent: false
    rejectUnauthorized: false
  logging:
    name: 'ActiveDirectory'
    streams: [
      level: 'error'
      stream: process.stdout
    ]

secretsFile = process.env.SECRETS_FILE or 'secrets.json'
secrets = JSON.parse fs.readFileSync(secretsFile)
engine = encrypter secrets.key

if process.env.NODE_ENV == 'DEVELOPMENT'
  MockDirectory = require './test/mocks/mockdirectory'
  rose = new MockDirectory ldapConfig
else
  rose = new ActiveDirectory ldapConfig

app = express()

# Landing page
app.use express.static(__dirname + '/public')

# Webview HTML
webviewPages {app}

app.use BodyParser.json()
app.use BodyParser.urlencoded(extended: false)
app.use corser.create()

app.use (req, res, next) ->
  # Register helper functions
  res.jsonError = sendError
  next()

# You must provide valid Rose-Hulman Credentials
# to use this service.
app.use (req, res, next) ->
  email = req.body.email
  password = req.body.password
  if !email or !password
    res.jsonError 400, 'Missing email or password'
  else
    rose.authenticate email, password, (err, auth) ->
      if err or !auth
        console.log email + ' failed authentication!'
        res.jsonError 401, 'Invalid Rose-Hulman credentials'
      else
        req.body.username = extractEmailUsername email
        next()

# Original API
api.v1 {app, rose, secrets, engine}
# New WebView APIs
api.v2 {app, rose, secrets, engine}

# Handle errors
app.use (err, req, res, next) ->
  msg = err?.toString() or 'Internal Server Error'
  status = err?.status or 500
  console.error err.stack
  res.jsonError status, msg

port = 8080
ip_address = '0.0.0.0'
module.exports = app.listen port, ip_address, ->
  console.log 'Rose-Hulman Authentication service started on port 8080'

