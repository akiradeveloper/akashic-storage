package main

import (
	"./lib"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"os/exec"
)

func configMc(alias, hostName string, portNumber int, accessKey, secretKey string) {
	command := exec.Command("mc", "config", "host", "add", alias,
		fmt.Sprintf("http://%s:%d", hostName, portNumber),
		accessKey, secretKey,
		"S3v2")
	if err := command.Run(); err != nil {
		os.Exit(1)
	}
}

func main() {
	config := lib.ReadConfig()
	alias := flag.String("alias", "akashic-storage", "name to access the server")
	hostName := flag.String("hostname", "localhost", "hostname")
	portNumber := flag.Int("port", 10946, "port number")
	flag.Parse()

	log.Printf("alias:%s, hostname:%s, port: %d\n", *alias, *hostName, *portNumber)

	args := flag.Args()
	userId := args[0]

	url := lib.AdminURL(config.HostName, config.Port) + "/" + userId

	req, _ := http.NewRequest("GET", url, lib.EmptyReader)
	req.SetBasicAuth("admin", config.Passwd)

	res, _ := http.DefaultClient.Do(req)

	bytes, _ := ioutil.ReadAll(res.Body)
	user := lib.NewUserFromXML(bytes)

	log.Printf("accessKey:%s, secretKey:%s\n", user.AccessKey, user.SecretKey)

	configMc(*alias, *hostName, *portNumber, user.AccessKey, user.SecretKey)
}
