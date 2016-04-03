package main

import (
	"./lib"
	"flag"
	"io/ioutil"
	"net/http"
	"os"
)

func main() {
	flag.Parse()

	config := lib.ReadConfig()
	url := lib.AdminURL(config.HostName, config.Port)

	req, _ := http.NewRequest("POST", url, lib.EmptyReader)
	req.SetBasicAuth("admin", config.Passwd)

	res, _ := http.DefaultClient.Do(req)

	bytes, _ := ioutil.ReadAll(res.Body)
	newUser := lib.NewUserFromXML(bytes)
	os.Stdout.WriteString(newUser.Id)
}
