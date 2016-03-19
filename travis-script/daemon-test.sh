cd ../installer
sh compile-jar.sh
sudo sh install.sh

sudo mkdir -p /tmp/akashic-storage-test

echo "**** start ****"
sudo service akashic-storage start
sleep 10
sudo service akashic-storage status

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
tree /mnt/akashic-storage

echo "**** stop ****"
sudo service akashic-storage stop
sleep 10
sudo service akashic-storage status

echo "**** start ****"
sudo service akashic-storage start
sleep 10
sudo service akashic-storage status
tree /mnt/akashic-storage
mc ls aka
mc ls aka/myb
mc cat aka/myb/myo > file-down
diff file-up file-down

echo -- error.log --
sudo cat /var/log/akashic-storage/error.log
echo -- all.log --
sudo cat /var/log/akashic-storage/all.log
