#!/bin/sh

cd s3-tests-clone
S3TEST_CONF=s3-tests.conf ./virtualenv/bin/nosetests $@
cd -
