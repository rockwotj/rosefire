request = require 'supertest'
should = require 'should'
assert = require 'assert'
jwt = require 'jsonwebtoken'

app = require '../server'

describe 'RegistryToken', ->
  it 'should successfully register token', (done) ->
    data =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pass'
      secret: 'foobar'

    request app
      .post '/api/register'
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
      .post '/api/register'
      .send data
      .expect 'Content-Type', /json/
      .expect 401
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
      .post '/api/register'
      .send data
      .expect 'Content-Type', /json/
      .expect 401
      .end (err, res) ->
        res.body.should.have.property 'error'
        res.body.should.have.property 'status'
        done()

describe 'Authenticate', ->
  
  @registryToken = null

  before (done) =>
    data =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pass'
      secret: 'foobar'

    request app
      .post '/api/register'
      .send data
      .expect 200
      .end (err, res) =>
        @registryToken = res.body.registryToken
        done()

  it 'should generate correct jwt', (done) =>
    data =
      email: 'rockwotj@rose-hulman.edu'
      password: 'pass'
      registryToken: @registryToken

    request app
      .post '/api/auth'
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
        decoded.should.have.property 'admin'
        decoded.admin.should.equal true
        done()

  assertTokenHasGroup = (username, group) =>
    (done) =>
      data =
        email: "#{username}@rose-hulman.edu"
        password: 'pass'
        registryToken: @registryToken
        options:
          group: true

      request app
        .post '/api/auth'
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
