require "sinatra"
set :bind, '0.0.0.0'
set :port, 10947

require "sinatra/reloader" if development?

require "rexml/document"

get "/" do
  newUserId = `akashic-admin-add`
  xml = (`akashic-admin-get #{newUserId}` rescue "<Error/>")
  doc = REXML::Document.new(xml)
  def gettext(doc, key)
    (doc.elements["User/#{key}"].text rescue "INVALID")
  end
  @id = gettext(doc, "Id")
  @access_key = gettext(doc, "AccessKey")
  @secret_key = gettext(doc, "SecretKey")
  erb :index
end
