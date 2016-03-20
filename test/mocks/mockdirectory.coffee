{extractEmailUsername} = require '../../utils'

module.exports = ({url}) ->
  console.log "Mocking ActiveDirectory for #{url}"

  mockUsers =
    rockwotj:
      password: 'pass'
      groups: [
        cn: 'Stu2012'
      ]

    mouck:
      password: 'pass'
      groups: [
        cn: 'CSSE-MA Departmental Admins'
      ]

    fisherds:
      password: 'pass'
      groups: [
        cn: 'Instructor'
      ]

  # Exported functions
  authenticate: (email, password, callback) ->
    username = extractEmailUsername email
    mockUser = mockUsers[username]
    callback null, mockUser?.password == password
  getGroupMembershipForUser: ({bindDN, bindCredentials}, searchName, callback) ->
    console.log "LDAP query for #{searchName}'s groups"
    username = extractEmailUsername bindDN
    mockUser = mockUsers[username]
    if mockUser?.password != bindCredentials
      return callback true
    callback null, mockUsers[searchName]?.groups

