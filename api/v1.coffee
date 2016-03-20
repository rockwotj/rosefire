async = require 'async'
FirebaseTokenGenerator = require 'firebase-token-generator'

module.exports = ({app, rose, secrets, engine}) ->

  app.use '/api/auth', (req, res, next) ->
    token = req.body.registryToken
    if !token
      res.status(400).json
        error: 'Missing registryToken in request'
        status: 400
    else
      decoded = engine.decrypt(token)
      if decoded == null
        res.status(400).json
          error: 'Invalid registryToken in request'
          status: 400
        return
      console.log 'Registry token successfully decoded for ' + decoded.admin + '\'s app'
      req.body.secret = decoded.secret
      req.body.admin = decoded.admin
      next()
      
  app.use '/api/auth', (req, res, next) ->
    if req.body.options?.group
      email = req.body.email
      username = req.body.username
      password = req.body.password
      creds =
        bindDN: email
        bindCredentials: password
      rose.getGroupMembershipForUser creds, username, (err, groups) ->
        if err or !groups
          console.log email + ' failed authentication!'
          res.status(400).json
            error: 'Invalid Rose-Hulman credentials'
            status: 401
        else
          studentChecker = (item) ->
            item.cn.startsWith('Stu') or item.cn == 'all-sg'
          instructorChecker = (item) ->
            item.cn == 'Instructor'
          sysAdminChecker = (item) ->
            item.cn == 'CSSE-MA Departmental Admins'

          checkForGroup = (pred) ->
            (item, cb) ->
              cb pred(item)
          isRole = (roleChecker) ->
            (callback) ->
              async.any groups, checkForGroup(roleChecker), callback.bind(undefined, null)

          groupTasks =
            isStudent: isRole studentChecker
            isInstructor: isRole instructorChecker
            isSysAdmin: isRole sysAdminChecker

          async.parallel groupTasks, (err, results) ->
            if err
              res.status(500).json
                error: err.toString()
                status: 500
            else
              if results.isSysAdmin
                req.body.group = 'SYSADMIN'
              else if results.isInstructor
                req.body.group = 'INSTRUCTOR'
              else if results.isStudent
                req.body.group = 'STUDENT'
              else
                req.body.group = 'OTHER'
              console.log 'Found group to be ' + req.body.group + ' for user ' + username
              next()
    else
      req.body.group = false
      next()

  app.post '/api/auth', (req, res) ->
    email = req.body.email
    username = req.body.username
    admin = req.body.admin
    isAdmin = username == admin
    password = req.body.password
    secret = req.body.secret
    tokenOptions = req.body.options or {}
    delete tokenOptions.debug
    if isAdmin and !tokenOptions.hasOwnProperty('admin')
      tokenOptions.admin = true
    else if !isAdmin
      tokenOptions.admin = false
    tokenGenerator = new FirebaseTokenGenerator(secret)
    tokenData =
      uid: username
      provider: 'rose-hulman'
    if req.body.group
      tokenData.group = req.body.group
    delete tokenOptions.group
    token = tokenGenerator.createToken(tokenData, tokenOptions)
    console.log 'Generated token authenticating ' + username
    res.json
      token: token
      timestamp: tokenData.timestamp
      username: tokenData.uid

  app.use '/api/register', (req, res, next) ->
    secret = req.body.secret
    if !secret
      res.status(400).json
        error: 'Missing secret in request'
        status: 400
    else
      next()

  app.post '/api/register', (req, res, next) ->
    email = req.body.email
    username = req.body.username
    password = req.body.password
    secret = req.body.secret
    tokenData =
      admin: username
      secret: secret
      timestamp: Math.floor(Date.now() / 1000)
    token = engine.encrypt(tokenData)
    console.log 'Generated registryToken for ' + username
    res.json
      registryToken: token
      timestamp: tokenData.timestamp
      username: username

