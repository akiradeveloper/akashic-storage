NAME=akashic-storage

compile:
	sh internal/compile-jar.sh
clean:
	cd ..; sbt clean
install:
	sh internal/install.sh
uninstall:
	rm -rf /opt/$(NAME)
update:
	cp $(NAME).jar /opt/$(NAME)/jar
