cd ../installer
sh compile-jar.sh
sudo sh install.sh
sudo service akashic-storage status

sudo mkdir -p /mnt/akashic-storage

akashic-admin-config <<INP


passwd
INP

userId = `akashic-storage-add`
echo "userId: $userId"

akashic-admin-setup-mc $userId

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
mc cat akashic-storage/myb/myo
