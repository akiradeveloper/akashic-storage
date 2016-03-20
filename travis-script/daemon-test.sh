cd ../installer
sh compile-jar.sh
sudo sh install.sh

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
cat ~/.mc/config.json

echo "akiradeveloper" > file-up
mc mb aka/myb
mc cp file-up aka/myb/myo
mc ls aka/myb
tree $DIR

echo "**** stop ****"
service akashic-storage stop
sleep 10
service akashic-storage status

echo "**** start ****"
service akashic-storage start
sleep 10
service akashic-storage status
tree $DIR
mc ls aka
mc ls aka/myb
mc cat aka/myb/myo > file-down
diff file-up file-down

echo -- error.log --
cat /var/akashic-storage/log/error.log
echo -- all.log --
cat /var/akashic-storage/log/all.log
