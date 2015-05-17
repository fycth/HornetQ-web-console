# HornetQ-web-console
A simple HornetQ web console

## Compile
play compile

## Copy default configuration file into production one
cp conf/application.conf.default conf/application.conf

## Edit configuration file and put port, hostname, username, password.
vi conf/application.conf

## Start the web console
bin/hornettool . -Dconfig.file=../conf/application.conf

## Navigate to http://localhost:9000

