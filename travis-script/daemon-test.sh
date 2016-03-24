set -e

cd ../installer
make
sudo make install
ls -lR /opt/akashic-storage
cd -

whoami
export

DIR=/tmp/akashic-storage-test
mkdir -p $DIR

echo "**** start ****"
service akashic-storage start
sleep 10
service akashic-storage status

# hostname, port, passwd
akashic-admin-config <<INP


passwd
INP
cat ~/.akashic-admin

userId=`akashic-admin-add`
echo "userId: $userId"

# alias, hostname, port
akashic-admin-setup-mc $userId <<INP
aka

10946
INP
cat ~/.mc/config.json; echo

dd if=/dev/urandom of=/tmp/file-up bs=1M count=32

mc mb aka/myb
mc --debug --quiet cp /tmp/file-up aka/myb/myo
mc --debug --quiet cat aka/myb/myo - > /dev/null
mc ls aka/myb/
tree $DIR

ls -lR /var/akashic-storage

echo -- daemon.log --
cat /var/log/akashic-storage/daemon.log
echo -- daemon_error.log --
cat /var/log/akashic-storage/daemon_error.log
echo -- error.log --
cat /var/log/akashic-storage/error.log
echo -- admin.log --
cat /var/log/akashic-storage/admin.log
echo -- all.log --
cat /var/log/akashic-storage/all.log

echo "**** stop ****"
service akashic-storage stop
sleep 10
service akashic-storage status

echo "**** start ****"
service akashic-storage start
sleep 10
service akashic-storage status
userId=`akashic-admin-add`
echo "userId: $userId"
akashic-admin-list
tree $DIR
cat ~/.mc/config.json
mc ls aka
mc ls aka/myb/
mc cat aka/myb/myo - > /dev/null

echo -- daemon.log --
cat /var/log/akashic-storage/daemon.log
echo -- daemon_error.log --
cat /var/log/akashic-storage/daemon_error.log
echo -- error.log --
cat /var/log/akashic-storage/error.log
echo -- admin.log --
cat /var/log/akashic-storage/admin.log
echo -- all.log --
cat /var/log/akashic-storage/all.log
