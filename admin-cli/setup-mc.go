package main

import (
	"./lib"
	"bufio"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"os/exec"
	"strconv"
	"strings"
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

	flag.Parse()

	args := flag.Args()
	userId := args[0]

	url := lib.AdminURL(config.HostName, config.Port) + "/" + userId

	req, _ := http.NewRequest("GET", url, lib.EmptyReader)
	req.SetBasicAuth("admin", config.Passwd)

	res, _ := http.DefaultClient.Do(req)

	bytes, _ := ioutil.ReadAll(res.Body)
	user := lib.NewUserFromXML(bytes)

	log.Printf("accessKey:%s, secretKey:%s\n", user.AccessKey, user.SecretKey)

	reader := bufio.NewReader(os.Stdin)

	fmt.Print("alias (default: akashic-storage): ")
	alias, _ := reader.ReadString('\n')
	alias = strings.Trim(alias, "\n")
	if alias == "" {
		alias = "akashic-storage"
	}

	fmt.Print("hostname (default: localhost): ")
	hostName, _ := reader.ReadString('\n')
	hostName = strings.Trim(hostName, "\n")
	if hostName == "" {
		hostName = "localhost"
	}

	fmt.Print("port# (default: 10946): ")
	portS, _ := reader.ReadString('\n')
	portS = strings.Trim(portS, "\n")
	if portS == "" {
		portS = "10946"
	}
	port, _ := strconv.Atoi(portS)

	log.Printf("alias:%s, hostname:%s, port: %d\n", alias, hostName, port)

	configMc(alias, hostName, port, user.AccessKey, user.SecretKey)
}
