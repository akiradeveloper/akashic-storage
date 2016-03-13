package main

import (
	"./lib"
	"log"
	"net/http"
)

func main() {
	url := lib.AdminURL("localhost", 10946)

	req, err := http.NewRequest("POST", url, lib.EmptyReader)
	req.SetBasicAuth("admin", "passwd")

	res, err := http.DefaultClient.Do(req)
	log.Println(res, err)
}
