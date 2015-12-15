#!/bin/bash
rm -rf resources/public/js/client*
lein clean
lein with-profile production compile
