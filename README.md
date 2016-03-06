# akashic-storage

[![Build Status](https://travis-ci.org/akiradeveloper/akashic-storage.svg)](https://travis-ci.org/akiradeveloper/akashic-storage)
[![Join the chat at https://gitter.im/akiradeveloper/fss3](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/akiradeveloper/fss3?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

**akashic-storage** is a Amazon S3 compatible storage server running on any filesystem backend. The ultimate goal is to offer the best on-premise S3 storage used world-wide. It's build on top of akka-http and written in Scala.

![Concept](https://rawgit.com/akiradeveloper/akashic-storage/develop/concept.svg)

## Getting Started

The easiest way to try out akashic-storage is to use [Vagrant](https://www.vagrantup.com) virtual machine. It will install everything you need and set up working akashic-storage service. You can find `Vagranfile` at `/vagrant-quick-start` folder.

### First step

Before launching your Vagrant environment, you must install [VirtualBox 5.x](https://www.virtualbox.org/wiki/Downloads) as [Vagrant](https://www.vagrantup.com/downloads.html). All of these software packages provide easy-to-use visual installers for all popular operating systems.

### Boot up Vagrant

Once you finish installing Vagrant, you can now boot up your Vagrant VM.

```
$ cd vagrant-quick-start
$ vagrant up
```

Now you can access akashic-storage at `http://localhost:10946`.
