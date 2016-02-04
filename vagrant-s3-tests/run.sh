#!/bin/sh

TESTNAME=$1 # e.g. s3tests.functional.test_s3:test_bucket_list_empty

cd s3-tests-clone
sudo S3TEST_CONF=s3-tests.conf ./virtualenv/bin/nosetests $TESTNAME
cd -
