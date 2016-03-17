package main

import (
	"./lib"
	"flag"
	"io/ioutil"
	"net/http"
	"os"
)

func main() {
	config := lib.ReadConfig()
	flag.Parse()

	url := lib.AdminURL(config.HostName, config.Port)

	req, _ := http.NewRequest("GET", url, lib.EmptyReader)
	req.SetBasicAuth("admin", config.Passwd)

	res, _ := http.DefaultClient.Do(req)

	bytes, _ := ioutil.ReadAll(res.Body)
	os.Stdout.Write(bytes)
}
