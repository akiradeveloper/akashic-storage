package main

import (
	"./lib"
	"bytes"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"strings"
)

func main() {
	config := lib.ReadConfig()

	name := flag.String("name", "", "new name")
	email := flag.String("email", "", "new email")
	displayName := flag.String("display-name", "", "new display name")
	flag.Parse()

	log.Printf("name:%s, email:%s, display name:%s\n", *name, *email, *displayName)

	args := flag.Args()
	userId := args[0]

	url := lib.AdminURL(config.HostName, config.Port) + "/" + userId

	var result bytes.Buffer
	result.WriteString("<User>")
	if *name != "" {
		result.WriteString(fmt.Sprintf("<Name>%s</Name>", *name))
	}
	if *email != "" {
		result.WriteString(fmt.Sprintf("<Email>%s</Email>", *email))
	}
	if *displayName != "" {
		result.WriteString(fmt.Sprintf("<DisplayName>%s</DisplayName>", *displayName))
	}
	result.WriteString("</User>")

	xml := result.String()
	log.Println(xml)

	req, _ := http.NewRequest("PUT", url, strings.NewReader(xml))
	req.Header.Add("Content-Type", "application/xml")

	res, _ := http.DefaultClient.Do(req)

	bytes, _ := ioutil.ReadAll(res.Body)
	os.Stdout.Write(bytes)
}
