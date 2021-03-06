#!/bin/sh

cat ~/.boto
mv boto.config ~/.boto

cd ..
sbt compile
sbt "runMain akashic.storage.app.RunServer" &
processId=$!
echo server process id: $processId
cd -

CLONEDIR=s3-tests-clone
CONFNAME=s3-tests.conf
rm -rf $CLONEDIR
git clone https://github.com/ceph/s3-tests $CLONEDIR
cp $CONFNAME $CLONEDIR

cd $CLONEDIR
./bootstrap
# FIXME should wait for the server to get ready
echo wait 10 seconds until server is up
sleep 10 # tmp
netstat -tanp
S3TEST_CONF=$CONFNAME ./virtualenv/bin/nosetests $@
cd -

echo successfully run and shut down the server
kill $processId
