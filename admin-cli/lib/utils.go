package lib

import (
	"fmt"
	"log"
	"strings"
)

func AdminURL(hostName string, portNumber int) string {
	url := fmt.Sprintf("http://%s:%d/admin/user", hostName, portNumber)
	log.Println(url)
	return url
}

var (
	EmptyReader = strings.NewReader("")
)
