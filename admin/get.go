package main

import (
	"./lib"
	"flag"
	"log"
	"net/http"
)

func main() {
	config := lib.ReadConfig()

	var userId = flag.String("id", "", "user id")
	flag.Parse()

	url := lib.AdminURL(config.HostName, config.PortNumber) + "/" + *userId

	req, err := http.NewRequest("GET", url, lib.EmptyReader)
	req.SetBasicAuth("admin", config.Passwd)

	res, err := http.DefaultClient.Do(req)
	log.Println(res, err)
}
