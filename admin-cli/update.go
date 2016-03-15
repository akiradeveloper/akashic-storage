package main

import (
	"./lib"
	"bytes"
	"flag"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
)

func main() {
	config := lib.ReadConfig()

	name := *flag.String("name", "", "new name")
	email := *flag.String("email", "", "new email")
	displayName := *flag.String("displayname", "", "new display name")
	flag.Parse()

	var result bytes.Buffer
	result.WriteString("<User>")
	if name != "" {
		result.WriteString(fmt.Sprintf("<Name>%s</Name>", name))
	}
	if email != "" {
		result.WriteString(fmt.Sprintf("<Email>%s</Email>", email))
	}
	if displayName != "" {
		result.WriteString(fmt.Sprintf("<DisplayName>%s</DisplayName>", displayName))
	}
	result.WriteString("</User>")

	url := lib.AdminURL(config.HostName, config.PortNumber)
	req, _ := http.NewRequest("PUT", url, bytes.NewReader(result.Bytes()))

	res, _ := http.DefaultClient.Do(req)

	bytes, _ := ioutil.ReadAll(res.Body)
	os.Stdout.Write(bytes)
}
