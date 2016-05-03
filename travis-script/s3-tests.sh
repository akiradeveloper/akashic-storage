sudo mkdir -p /mnt/akashic-storage; sudo chmod o+rwx /mnt/akashic-storage
sudo mkdir -p /opt/akashic-storage/log; sudo chmod o+rwx /opt/akashic-storage/log

cd ../s3-tests
sh run.sh
