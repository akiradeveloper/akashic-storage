package lib

import (
	"bytes"
	"fmt"
	"github.com/BurntSushi/toml"
	"io/ioutil"
)

type Config struct {
	HostName    string
	PortNumber  int
	AdminPasswd string
}

func Decode(fileName string) Config {
	var config Config
	toml.DecodeFile(fileName, &config)
	return config
}

func (self *Config) Encode(fileName string) {
	var buf bytes.Buffer
	encoder := toml.NewEncoder(&buf)
	encoder.Encode(self)
	ioutil.WriteFile(fileName, buf.Bytes(), 0777)
}

func (self *Config) Debug() {
	fmt.Printf("hostName: %s, portNumber: %d, passwd: %s\n",
		self.HostName,
		self.PortNumber,
		self.AdminPasswd)
}
