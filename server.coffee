'use strict'

ActiveDirectory = require 'activedirectory'
express = require 'express'
BodyParser = require 'body-parser'
corser = require 'corser'
fs = require 'fs'
encrypter = require 'simple-encryptor'

{ extractEmailUsername } = require './utils'
apiV1 = require './api/v1'
apiV2 = require './api/v2'
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

# You must provide valid Rose-Hulman Credentials
# to use this service.
app.use (req, res, next) ->
  email = req.body.email
  password = req.body.password
  if !email or !password
    res.status(400).json
      error: 'Missing email or password'
      status: 400
  else
    rose.authenticate email, password, (err, auth) ->
      if err or !auth
        console.log email + ' failed authentication!'
        res.status(401).json
          error: 'Invalid Rose-Hulman credentials'
          status: 401
      else
        req.body.username = extractEmailUsername email
        next()

# Original API
apiV1 {app, rose, secrets, engine}
# New WebView APIs
apiV2 {app, rose, secrets, engine}

# Handle errors
app.use (err, req, res, next) ->
  msg = err?.toString() or 'Internal Server Error'
  status = err?.status or 500
  console.error msg
  res.status(status).json
    error: msg
    status: status

port = 8080
ip_address = '127.0.0.1'
module.exports = app.listen port, ip_address, ->
  console.log 'Rose-Hulman Authentication service started on port 8080'

