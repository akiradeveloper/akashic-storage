INSTALL_DIR=/usr/local/bin

ADD=akashic-admin-add
GET=akashic-admin-get
LIST=akashic-admin-list
UPDATE=akashic-admin-update
CONFIG=akashic-admin-config
SETUP_MC=akashic-admin-setup-mc
ALL=$(ADD) \
    $(GET) \
    $(LIST) \
    $(UPDATE) \
    $(CONFIG) \
    $(SETUP_MC)

all: $(ALL)
clean: 	
	rm $(ALL)
install: all
	install -m 555 $(ALL) $(INSTALL_DIR)
uninstall:
	rm $(INSTALL_DIR)/$(ALL)
akashic-admin-%: %.go
	go get github.com/BurntSushi/toml
	go build -o $@ $<
