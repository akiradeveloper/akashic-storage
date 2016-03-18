package main

import (
	"./lib"
	"bufio"
	"flag"
	"fmt"
	"os"
	"strconv"
	"strings"
)

func main() {
	flag.Parse()

	reader := bufio.NewReader(os.Stdin)
	fmt.Print("hostname (default: localhost): ")
	hostName, _ := reader.ReadString('\n')
	hostName = strings.Trim(hostName, "\n")
	if hostName == "" {
		hostName = "localhost"
	}

	fmt.Print("port# (default: 10946): ")
	portS, _ := reader.ReadString('\n')
	portS = strings.Trim(portS, "\n")
	if portS == "" {
		portS = "10946"
	}
	port, _ := strconv.Atoi(portS)

	fmt.Print("admin passwd: ")
	passwd, _ := reader.ReadString('\n')
	passwd = strings.Trim(passwd, "\n")

	config := lib.Config{hostName, port, passwd}
	config.Debug()

	config.Encode(lib.ConfigPath)
}
