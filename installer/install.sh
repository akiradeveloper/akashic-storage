NAME=akashic-storage
if [ ! -e $NAME.jar ]; then
    echo "$NAME.jar not found. please run compile-jar.sh [NG]"
    exit 1
fi 
mkdir -p /opt/$NAME
mkdir -p /opt/$NAME/jar
mkdir -p /opt/$NAME/etc
mkdir -p /var/log/$NAME
cp $NAME.jar /opt/$NAME/jar
if [ ! -e /opt/$NAME/etc/conf ]; then
  cp default.conf /opt/$NAME/etc/conf
else
  echo "/opt/$NAME/etc/conf exists. skipped [OK]" 
fi
cp $NAME /etc/init.d
chmod +x /etc/init.d/$NAME
