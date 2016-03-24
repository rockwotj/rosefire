async = require 'async'
{isBoolean, isFinite} = require 'underscore'

module.exports =
  extractEmailUsername: (email) ->
    [username, domain] = email.split '@'
    username.toLowerCase()

  # Function extention for response
  sendError: (code, msg) ->
    console.error msg
    @status(code).json
      error: msg
      status: code

  unixTime: () ->
    Math.floor(Date.now() / 1e3)

  extractOptions: (options) ->
    return null unless options
    {g, e} = options
    {group: g, expires: e}

  createOptions: (options) ->
    return null unless options
    {group, expires} = options
    options = {}
    if isBoolean group
      options.g = group
    if isFinite expires
      options.e = expires
    options

  extractGroup: (ldapGroup, callback) ->
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
      (cb) ->
        async.any ldapGroup, checkForGroup(roleChecker), cb.bind(undefined, null)

    groupTasks =
      isStudent: isRole studentChecker
      isInstructor: isRole instructorChecker
      isSysAdmin: isRole sysAdminChecker

    async.parallel groupTasks, (err, results) ->
      callback err if err
      group = switch
        when results.isSysAdmin then 'SYSADMIN'
        when results.isInstructor then 'INSTRUCTOR'
        when results.isStudent then 'STUDENT'
        else 'OTHER'
      callback null, group


