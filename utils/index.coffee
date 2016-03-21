async = require 'async'

module.exports =
  extractEmailUsername: (email) ->
    [username, domain] = email.split '@'
    username.toLowerCase()
  # Function extention for response
  sendError: (code, msg) ->
    @status(code).json
      error: msg
      status: code
  verifyOptions: (options) ->
    return options if !options
    delete options.debug
    delete options.admin
    delete options.group
    options
    # TODO: verify only correct options
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


