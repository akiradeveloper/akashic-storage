package lib

import (
	"encoding/xml"
)

type User struct {
	Id          string
	AccessKey   string
	SecretKey   string
	Name        string
	Email       string
	DisplayName string
}

func NewUserFromXML(data []byte) User {
	var res User
	xml.Unmarshal(data, &res)
	return res
}
