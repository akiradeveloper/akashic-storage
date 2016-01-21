# akashic-storage

[![Build Status](https://travis-ci.org/akiradeveloper/akashic-storage.svg)](https://travis-ci.org/akiradeveloper/akashic-storage)
[![Join the chat at https://gitter.im/akiradeveloper/fss3](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/akiradeveloper/fss3?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

**akashic-storage** is a Amazon S3 compatible storage server running on **any** filesystem. The term **any** potentially includes distributed filesystems (like GlusterFS) because akashic-storage is designed for that purpose from scratch. It's also durable for server faults by introducing **commit** mechanism so partially written objects are ignored.

The software is built upon **finch** that is a sophisticated finagle-http wrapper in functional way. Therefore akashic-storage's all codebase is written in **Scala** with a taste of functional programming so it's readable, small and then solid.

The goal is to provide a perfect on-premise S3 storage used world-wide. 

![Concept](https://rawgit.com/akiradeveloper/akashic-storage/develop/concept.svg)
