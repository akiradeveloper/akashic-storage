ADD=akashic-admin-add
GET=akashic-admin-get
CONFIG=akashic-admin-config
ALL=$(ADD) $(GET) $(CONFIG)

all: $(ALL)
akashic-admin-%: %.go
	go build -o $@ $<
clean: 	
	rm $(ALL)
