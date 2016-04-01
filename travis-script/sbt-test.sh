set -e
sudo mkdir -p /var/log/akashic-storage; sudo chmod o+rwx /var/log/akashic-storage

cd ..
sbt test

echo -- all.log --
cat /var/log/akashic-storage/all.log
echo -- error.log --
cat /var/log/akashic-storage/error.log
