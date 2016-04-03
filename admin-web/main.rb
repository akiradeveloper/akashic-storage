require "sinatra"
set :bind, '0.0.0.0'
set :port, 8080

require "sinatra/reloader" if development?

require "rexml/document"

get "/" do
  newUserId = `akashic-admin-add`
  xml = `akashic-admin-get #{newUserId}`
  doc = REXML::Document.new(xml)
  @access_key = doc.elements["User/AccessKey"].text
  @secret_key = doc.elements["User/SecretKey"].text
  erb :index
end
