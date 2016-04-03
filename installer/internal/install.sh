NAME=akashic-storage
if [ ! -e $NAME.jar ]; then
  echo "$NAME.jar not found. please run make [NG]"
  exit 1
fi
OPT=/opt/$NAME
mkdir -p $OPT

mkdir -p $OPT/jar
cp $NAME.jar $OPT/jar

mkdir -p $OPT/etc
cp application_default.conf $OPT/etc/application.conf
cp logback_default.xml $OPT/etc/logback.xml

mkdir -p $OPT/log; chmod o+rwx $OPT/log
mkdir -p $OPT/run; chmod o+rwx $OPT/run

cp $NAME /etc/init.d
chmod o+x /etc/init.d/$NAME
