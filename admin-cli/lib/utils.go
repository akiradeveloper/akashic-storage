package lib

import (
	"fmt"
	"strings"
)

func AdminURL(hostName string, port int) string {
	url := fmt.Sprintf("http://%s:%d/admin/user", hostName, port)
	return url
}

var (
	EmptyReader = strings.NewReader("")
)
