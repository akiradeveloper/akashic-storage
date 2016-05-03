set -e

cd ../
vagrant up

curl http://localhost:10947/
