package main

import (
	"./lib"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
)

func main() {
	config := lib.ReadConfig()

	flag.Parse()
	args := flag.Args()
	userId := args[0]

	url := lib.AdminURL(config.HostName, config.PortNumber) + "/" + userId

	req, err := http.NewRequest("GET", url, lib.EmptyReader)
	req.SetBasicAuth("admin", config.Passwd)

	res, err := http.DefaultClient.Do(req)
	log.Println(res, err)

	bytes, _ := ioutil.ReadAll(res.Body)
	os.Stdout.Write(bytes)
}
