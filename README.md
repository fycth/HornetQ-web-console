HornetQ-web-console
===
This is a very raw project that I made while playing with Scala (and Scala Play!). It is a web-tool that allows connecting to a HornetQ installation and do some kind of basic actions (like gathering some statistics, sending and receving messages).

Prerequisites
---
You should have these tools installed:
* JDK - http://www.oracle.com/technetwork/java/index.html
* SBT - http://www.scala-sbt.org
* Scala - http://www.scala-lang.org
* Typesafe Activator - http://www.typesafe.com/community/core-tools/activator-and-sbt

How to build
---
```$ cd <project_dir>
    $ activator clean compile
```

How to run
---
```$ activator run```

Now navigate your web broser to http://localhost:9000
