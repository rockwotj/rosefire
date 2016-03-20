express = require 'express'
mu = require 'mu2'
util = require 'util'

module.exports = ({app}) ->
  app.use '/webview', express.static(__dirname + '/public')

  app.get '/webview/login', (req, res) ->
    mu.clearCache() if process.env.NODE_ENV == 'DEVELOPMENT'

    registryToken = req.query.registryToken
    platformFunction = getPlatformFunction req.query
    if not registryToken or not platformFunction
      res.sendFile __dirname + '/src/error.html'
    else
      stream = mu.compileAndRender __dirname + '/src/login.html.mustache', {registryToken}
      stream.pipe res

getPlatformFunction = ({platform}) ->
  switch platform
    when 'android' then 'Android.finish'
    else null
