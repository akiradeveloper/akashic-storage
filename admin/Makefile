INSTALL_DIR=/usr/local/bin

ADD=akashic-admin-add
GET=akashic-admin-get
UPDATE=akashic-admin-update
CONFIG=akashic-admin-config
SETUP_MC=akashic-admin-setup-mc
ALL=$(ADD) \
    $(GET) \
    $(CONFIG) \
    $(UPDATE) \
    $(SETUP_MC)

all: $(ALL)
clean: 	
	rm $(ALL)
install:
	install -m 555 $(ALL) $(INSTALL_DIR)
uninstall:
	rm $(INSTALL_DIR)/$(ALL)
akashic-admin-%: %.go
	go build -o $@ $<
