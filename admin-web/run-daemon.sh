#!/bin/sh
nohup ruby app.rb >> /opt/akashic-storage/log/admin-web.log 2>&1 &
