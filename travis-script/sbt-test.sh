set -e
sudo mkdir -p /opt/akashic-storage/log; sudo chmod o+rwx /opt/akashic-storage/log

cd ..
sbt test

echo -- all.log --
cat /opt/akashic-storage/log/all.log
echo -- error.log --
cat /opt/akashic-storage/log/error.log
