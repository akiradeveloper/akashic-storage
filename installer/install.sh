NAME=akashic-storage
if [ ! -e $NAME.jar ]; then
  echo "$NAME.jar not found. please run compile-jar.sh [NG]"
  exit 1
fi
mkdir -p /opt/$NAME
mkdir -p /opt/$NAME/jar
mkdir -p /var/$NAME; chmod 0666 /var/$NAME
cp $NAME.jar /opt/$NAME/jar
cp $NAME /etc/init.d
chmod +x /etc/init.d/$NAME
