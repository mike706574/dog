#!/bin/bash
rm -rf resources/public/js/client*
lein clean
lein with-profile $1 figwheel
