#!/bin/sh

cd ..
sbt compile
sbt -Dconfig.file=src/test/resources/test.conf "runMain akashic.storage.app.RunServer" &
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
echo wait 30 seconds until server is up
sleep 30 # tmp
netstat -tanp
cat ~/.boto
mv boto.config ~/.boto
S3TEST_CONF=$CONFNAME ./virtualenv/bin/nosetests
cd -

echo successfully run and shut down the server
kill $processId
