NAME=akashic-storage
if [ ! -e $NAME.jar ]; then
  echo "$NAME.jar not found. please run make [NG]"
  exit 1
fi
mkdir -p /opt/$NAME
mkdir -p /opt/$NAME/jar
cp $NAME.jar /opt/$NAME/jar
mkdir -p /opt/$NAME/etc
cp application_default.conf /opt/$NAME/etc/application.conf
cp logback_default.xml /opt/$NAME/etc/logback.xml

mkdir -p /var/$NAME; chmod o+rwx /var/$NAME

cp $NAME /etc/init.d
chmod +x /etc/init.d/$NAME