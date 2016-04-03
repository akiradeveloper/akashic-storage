require "sinatra"
require "sinatra/reloader" if development?

get "/" do
  @access_key = "hoge"
  @secret_key = "hige"
  erb :index
end
