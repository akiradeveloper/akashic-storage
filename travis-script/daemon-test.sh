cd ../installer
sh compile-jar.sh
sudo sh install.sh
sudo service akashic-storage status

sudo mkdir -p /mnt/akashic-storage

# hostname, port, passwd
akashic-admin-config <<INP


passwd
INP

userId = `akashic-storage-add`
echo "userId: $userId"

# alias, hostname, port
akashic-admin-setup-mc $userId <<INP
akast

10946
INP

echo "start"
sudo service akashic-storage start
sudo service akashic-storage status

echo "akiradeveloper" > /tmp/t
mc mb akashic-storage/myb
mc cp /tmp/t akashic-storage/myb/myo
mc ls akashic-storage/myb

echo "stop"
sudo service akashic-storage stop
sudo service akashic-storage status

echo "start"
sudo service akashic-storage start
sudo service akashic-storage status
mc ls akashic-storage
mc ls akashic-storage/myb
mc cat akashic-storage/myb/myo > t-down
diff t t-down
