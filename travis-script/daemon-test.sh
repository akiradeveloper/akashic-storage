cd ../installer
sh compile-jar.sh
sudo sh install.sh
sudo service akashic-storage status

sudo mkdir -p /mnt/akashic-storage

# hostname, port, passwd
akashic-admin-config <<INP


passwd
INP

userId=`akashic-storage-add`
echo "userId: $userId"

# alias, hostname, port
akashic-admin-setup-mc $userId <<INP
aka

10946
INP

ls -l /usr/default/java

echo "**** start ****"
sudo service akashic-storage start
sudo service akashic-storage status

echo "akiradeveloper" > /tmp/t
mc mb aka/myb
mc cp /tmp/t aka/myb/myo
mc ls aka/myb

echo "**** stop ****"
sudo service akashic-storage stop
sudo service akashic-storage status

echo "**** start ****"
sudo service akashic-storage start
sudo service akashic-storage status
mc ls aka
mc ls aka/myb
mc cat aka/myb/myo > t-down
diff t t-down
