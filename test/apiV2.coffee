request = require 'supertest'
should = require 'should'
assert = require 'assert'
jwt = require 'jsonwebtoken'
_ = require 'underscore'
async = require 'async'

app = require '../server'

describe 'RegistryToken v2', ->
  it 'should successfully register token', (done) ->
    data =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pass'
      secret: 'foobar'
    request app
      .post '/v2/api/register'
      .send data
      .expect 'Content-Type', /json/
      .expect 200
      .end (err, res) ->
        res.body.should.have.property 'registryToken'
        res.body.should.have.property 'username'
        res.body.username.should.equal 'rockwotj'
        done()
  it 'should reject invalid user', (done) ->
    data =
      email: 'rockwo@rose-hulman.edu'
      password: 'pass'
      secret: 'foobar'
    request app
      .post '/v2/api/register'
      .send data
      .expect 'Content-Type', /json/
      .expect 401
      .end (err, res) ->
        res.body.should.have.property 'error'
        res.body.should.have.property 'status'
        done()
  it 'should fail if secret forgotten', (done) ->
    data =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pass'
    request app
      .post '/v2/api/register'
      .send data
      .expect 'Content-Type', /json/
      .expect 400
      .end (err, res) ->
        res.body.should.have.property 'error'
        res.body.should.have.property 'status'
        done()
  it 'should reject invalid password', (done) ->
    data =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pas'
      secret: 'foobar'
    request app
      .post '/v2/api/register'
      .send data
      .expect 'Content-Type', /json/
      .expect 401
      .end (err, res) ->
        res.body.should.have.property 'error'
        res.body.should.have.property 'status'
        done()

describe 'Authenticate v2', ->
  
  @registryToken = null
  @registryTokenWithExpires = null
  @registryTokenWithGroup = null

  before (done) =>
    data =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pass'
      secret: 'foobar'
    dataWithExpires =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pass'
      secret: 'foobar'
    dataWithGroup =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pass'
      secret: 'foobar'

    dataWithExpires.options =
      expires: 48 * 60 * 60
    dataWithGroup.options =
      group: true

    requestToken = (data) ->
      (cb) ->
        request app
          .post '/v2/api/register'
          .send data
          .expect 200
          .end cb

    tasks =
      data: requestToken data
      dataWithExpires: requestToken dataWithExpires
      dataWithGroup: requestToken dataWithGroup

    async.parallel tasks, (err, results) =>
      should.not.exist err
      should.exist results
      @registryToken = results.data.body.registryToken
      @registryTokenWithExpires = results.dataWithExpires.body.registryToken
      @registryTokenWithGroup = results.dataWithGroup.body.registryToken
      done()

   it 'should generate correct jwt', (done) =>
    data =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pass'
      registryToken: @registryToken

    request app
      .post '/v2/api/auth'
      .send data
      .expect 200
      .expect 'Content-Type', /json/
      .end (err, res) ->
        res.body.should.have.property 'token'
        res.body.should.have.property 'username'
        res.body.username.should.equal 'rockwotj'
        token = res.body.token
        decoded = jwt.verify token, 'foobar'
        decoded.should.have.property 'd'
        data = decoded.d
        data.should.have.property 'uid'
        data.uid.should.equal 'rockwotj'
        data.should.have.property 'provider'
        data.provider.should.equal 'rose-hulman'
        done()

   it 'should have correct expire time', (done) =>
    data =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pass'
      registryToken: @registryTokenWithExpires

    request app
      .post '/v2/api/auth'
      .send data
      .expect 200
      .expect 'Content-Type', /json/
      .end (err, res) ->
        res.body.should.have.property 'token'
        res.body.should.have.property 'username'
        res.body.username.should.equal 'rockwotj'
        token = res.body.token
        decoded = jwt.verify token, 'foobar'
        decoded.should.have.property 'd'
        data = decoded.d
        data.should.have.property 'uid'
        data.uid.should.equal 'rockwotj'
        data.should.have.property 'provider'
        data.provider.should.equal 'rose-hulman'
        decoded.should.have.property 'exp'
        decoded.should.have.property 'iat'
        decoded.exp.should.equal decoded.iat + 48 * 60 * 60
        done()

  it 'should ignore old options', (done) =>
    data =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pass'
      registryToken: @registryToken
      options:
        group: true
        expires: 12345

    request app
      .post '/v2/api/auth'
      .send data
      .expect 200
      .expect 'Content-Type', /json/
      .end (err, res) ->
        res.body.should.have.property 'token'
        res.body.should.have.property 'username'
        res.body.username.should.equal 'rockwotj'
        token = res.body.token
        decoded = jwt.verify token, 'foobar'
        decoded.should.have.property 'd'
        data = decoded.d
        data.should.have.property 'uid'
        data.uid.should.equal 'rockwotj'
        data.should.have.property 'provider'
        data.provider.should.equal 'rose-hulman'
        data.should.not.have.property 'group'
        decoded.should.not.have.property 'exp'
        done()

  it 'should reject invalid credentials', (done) =>
    data =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pas'
      registryToken: @registryToken

    request app
      .post '/v2/api/auth'
      .send data
      .expect 401
      .expect 'Content-Type', /json/
      .end (err, res) ->
        res.body.should.have.property 'error'
        res.body.should.have.property 'status'
        done()

  it 'should reject invalid registryToken', (done) ->
    data =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pass'
      registryToken: 'foobar'

    request app
      .post '/v2/api/auth'
      .send data
      .expect 400
      .expect 'Content-Type', /json/
      .end (err, res) ->
        res.body.should.have.property 'error'
        res.body.should.have.property 'status'
        done()



  assertTokenHasGroup = (username, group) =>
    (done) =>
      data =
        email: "#{username}@rose-hulman.edu"
        password: 'pass'
        registryToken: @registryTokenWithGroup

      request app
        .post '/v2/api/auth'
        .send data
        .expect 200
        .expect 'Content-Type', /json/
        .end (err, res) =>
          res.body.should.have.property 'token'
          decoded = jwt.verify res.body.token, 'foobar'
          decoded.should.have.property 'd'
          data = decoded.d
          data.should.have.property 'group'
          data.group.should.equal group
          done()

  it 'should find rockwotj to be student', assertTokenHasGroup('rockwotj', 'STUDENT')
  it 'should find fisherds to be instructor', assertTokenHasGroup('fisherds', 'INSTRUCTOR')
  it 'should find mouck to be sys admin', assertTokenHasGroup('mouck', 'SYSADMIN')
