ADD=akashic-admin-add
GET=akashic-admin-get
CONFIG=akashic-admin-config
ALL=$(ADD) $(GET) $(CONFIG)
INSTALL_DIR=/usr/local/bin

all: $(ALL)
clean: 	
	rm $(ALL)
install:
	install -m 555 $(ALL) $(INSTALL_DIR)
uninstall:
	rm $(INSTALL_DIR)/$(ALL)
akashic-admin-%: %.go
	go build -o $@ $<
