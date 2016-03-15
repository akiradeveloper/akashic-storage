package main

import (
	"./lib"
	"flag"
	"io/ioutil"
	"net/http"
)

func registerMc(alias, hostName string, portNumber int, accessKey, secretKey string) {
}

func main() {
	config := lib.ReadConfig()
	alias := *flag.String("alias", "akashic-storage", "name to access the server")
	hostName := *flag.String("hostname", "localhost", "hostname")
	portNumber := *flag.Int("portnumber", 10946, "port number")
	flag.Parse()

	args := flag.Args()
	userId := args[0]

	url := lib.AdminURL(config.HostName, config.PortNumber) + "/" + userId

	req, _ := http.NewRequest("GET", url, lib.EmptyReader)
	req.SetBasicAuth("admin", config.Passwd)

	res, _ := http.DefaultClient.Do(req)

	bytes, _ := ioutil.ReadAll(res.Body)
	user := lib.NewUserFromXML(bytes)

	registerMc(alias, hostName, portNumber, user.AccessKey, user.SecretKey)
}
