set -e

MOUNTPOINT=/mnt/akashic-storage
sudo mkdir -p $MOUNTPOINT
sudo chmod o+rwx $MOUNTPOINT

cd ../installer
make
sudo make install
ls -lR /opt/akashic-storage
cd -

whoami
export

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

echo "aaaa" > /tmp/up-small
dd if=/dev/urandom of=/tmp/up-large bs=1M count=128

mc mb aka/myb
mc --debug --quiet cp /tmp/up-small aka/myb/myo-small
mc --debug --quiet cp /tmp/up-large aka/myb/myo-large
mc --debug --quiet cp aka/myb/myo-small down-small-1
mc --debug --quiet cp aka/myb/myo-large down-large-1
mc ls aka/myb/
tree $MOUNTPOINT

echo -- daemon.log --
cat /opt/akashic-storage/log/daemon.log
echo -- daemon_error.log --
cat /opt/akashic-storage/log/daemon_error.log
echo -- error.log --
cat /opt/akashic-storage/log/error.log
echo -- admin.log --
cat /opt/akashic-storage/log/admin.log
echo -- all.log --
cat /opt/akashic-storage/log/all.log

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
tree $MOUNTPOINT
cat ~/.mc/config.json
mc ls aka
mc ls aka/myb/
mc --debug --quiet cp aka/myb/myo-small down-small-2
mc --debug --quiet cp aka/myb/myo-large down-large-2

echo -- daemon.log --
cat /opt/akashic-storage/log/daemon.log
echo -- daemon_error.log --
cat /opt/akashic-storage/log/daemon_error.log
echo -- error.log --
cat /opt/akashic-storage/log/error.log
echo -- admin.log --
cat /opt/akashic-storage/log/admin.log
echo -- all.log --
cat /opt/akashic-storage/log/all.log
