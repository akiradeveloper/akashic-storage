require "sinatra"
set :bind, '0.0.0.0'
set :port, 8080

require "sinatra/reloader" if development?

get "/" do
  @access_key = "hoge"
  @secret_key = "hige"
  erb :index
end
