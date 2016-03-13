package main

import (
	"./lib"
	"flag"
	"log"
	"net/http"
)

func main() {
	var userId = flag.String("id", "", "user id")
	flag.Parse()

	url := lib.AdminURL("localhost", 10946) + "/" + *userId

	req, err := http.NewRequest("GET", url, lib.EmptyReader)
	req.SetBasicAuth("admin", "passwd") // tmp

	res, err := http.DefaultClient.Do(req)
	log.Println(res, err)
}
