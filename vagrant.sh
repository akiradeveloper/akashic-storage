echo export GOPATH="$HOME/go" >> .bash_profile
echo export PATH="/usr/local/go/bin:$PATH" >> .bash_profile

source .bash_profile

sudo yum -y update
sudo yum -y install jsvc rubygems tree curl git tree

if [ ! -e jdk-8u65-linux-x64.rpm ]; then
  wget --no-check-certificate --no-cookies - --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u65-b17/jdk-8u65-linux-x64.rpm
  sudo yum localinstall -y jdk-8u65-linux-x64.rpm
fi

wget https://storage.googleapis.com/golang/go1.6.linux-amd64.tar.gz
sudo tar -C /usr/local -xzf go1.6.linux-amd64.tar.gz
go version

# run `sh compile-jar.sh` in installer/ first
# so you can avoid compiling jar file in this Vagrant script
cd akashic-storage/installer
if [ ! -e akashic-storage.jar ]; then
  curl https://bintray.com/sbt/rpm/rpm | sudo tee /etc/yum.repos.d/bintray-sbt-rpm.repo
  sudo yum -y install sbt
  make
fi
sudo make install
cd -

sudo mkdir -p /mnt/akashic-storage
sudo chmod o+rwx /mnt/akashic-storage
service akashic-storage start
sleep 10

cd akashic-storage/admin-cli
make
sudo make install
cd -
ls /usr/local/bin

akashic-admin-config <<INP


passwd
INP
cat ~/.akashic-admin

gem install sinatra
gem install sinatra-contrib
cd akashic-storage/admin-web
sh run-daemon.sh
cd -
