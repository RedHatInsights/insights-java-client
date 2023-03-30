package main

import (
	"errors"
	"fmt"
	"log"
	"os"
	"os/user"
	"syscall"
)

const (
	certPath      = "/etc/pki/consumer/cert.pem"
	keyPath       = "/etc/pki/consumer/key.pem"
	validUsername = "jboss"
)

const usageError = "incorrect usage"

const (
	errCurrentUser   = 1
	errIncorrectUser = 2
	errWrongArgs     = 3
	errNotSetuid     = 4
	errCertOrKey     = 5
	errFileRead      = 6
)

func main() {
	ensureCorrectUser()
	ensureOneArgument()
	setUidForRead()
	readAndPrint(os.Args[1])
}

func fail(code int, message string) {
	log.Println(message)
	os.Exit(code)
}

func ensureCorrectUser() {
	current, err := user.Current()
	if err != nil {
		fail(errCurrentUser, err.Error())
	}
	if current.Username != validUsername {
		fail(errIncorrectUser, fmt.Sprint("Incorrect current ", current.Username))
	}
}

func ensureOneArgument() {
	if len(os.Args) != 2 {
		fail(errWrongArgs, usageError)
	}
}

func setUidForRead() {
	ruid := syscall.Getuid()
	// The binary expects to be setuid, be we have to actually change EUID to root
	err := syscall.Setreuid(ruid, 0)
	if err != nil {
		fail(errNotSetuid, err.Error())
	}
}

func readAndPrint(s string) {
	path, err := selectFile(s)
	if err != nil {
		fail(errCertOrKey, err.Error())
	}
	data, err := readFile(path)
	if err != nil {
		fail(errFileRead, err.Error())
	}
	fmt.Println(data)
}

func selectFile(argument string) (string, error) {
	switch argument {
	case "--cert":
		return certPath, nil
	case "--key":
		return keyPath, nil
	default:
		return "", errors.New(usageError)
	}
}

func readFile(path string) (string, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return "", err
	}
	return string(data), nil
}
