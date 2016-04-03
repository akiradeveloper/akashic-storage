require "sinatra"
set :bind, '0.0.0.0'
set :port, 8080

require "sinatra/reloader" if development?

require "rexml/document"

get "/" do
  newUserId = `akashic-admin-add`
  xml = (`akashic-admin-get #{newUserId}` rescue "<Error/>")
  doc = REXML::Document.new(xml)
  def gettext(key)
    (doc.elements["User/#{key}"].text rescue "INVALID")
  end
  @id = gettext("Id")
  @access_key = gettext("AccessKey")
  @secret_key = gettext("SecretKey")
  erb :index
end
