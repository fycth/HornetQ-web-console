name := "hornettool"

version := "1.0"

lazy val `hornettool` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq( jdbc , anorm , cache , ws , "org.hornetq" % "hornetq-core-client" % "2.4.5.Final", "org.hornetq" % "hornetq-jms-client" % "2.4.5.Final" , "org.hornetq" % "hornetq-commons" % "2.4.5.Final" )

libraryDependencies += "org.hornetq" % "hornetq-native" % "2.4.5.Final" from "http://central.maven.org/maven2/org/hornetq/hornetq-native/2.4.5.Final/hornetq-native-2.4.5.Final.jar"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  