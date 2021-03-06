//libraryDependencies += "com.xjeffrose" % "xio" % "0.11.0-SNAPSHOT" % Test
// libraryDependencies += "rocksdb" % "org.rocksdb" % "1.0"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"
libraryDependencies += "org.apache.curator" % "curator-framework" % "2.9.1" exclude("log4j", "log4j")
libraryDependencies += "org.apache.curator" % "curator-recipes" % "2.9.1" exclude("log4j", "log4j")
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.7.3"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-annotations" % "2.7.3"
libraryDependencies += "io.netty" % "netty-tcnative-boringssl-static" % "1.1.33.Fork15"
libraryDependencies += "io.netty" % "netty-all" % "4.1.1.Final"
// Testing
libraryDependencies += "junit" % "junit" % "4.12" % Test
libraryDependencies += "org.apache.curator" % "curator-test" % "2.9.1" % Test
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
libraryDependencies += "org.codehaus.groovy" % "groovy-all" % "2.4.1" % Test
// http://mvnrepository.com/artifact/org.slf4j/log4j-over-slf4j
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.5"
// http://mvnrepository.com/artifact/ch.qos.logback/logback-classic
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7" % Test
// http://mvnrepository.com/artifact/org.rocksdb/rocksdbjni
libraryDependencies += "org.rocksdb" % "rocksdbjni" % "4.5.1"
libraryDependencies += "com.beust" % "jcommander" % "1.48"


lazy val Serial = config("serial") extend(Test)

parallelExecution in Serial := false

parallelExecution := true

fork := true

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

testOptions in Test += Tests.Setup( () => println("Setup") )

testOptions in Test += Tests.Cleanup( () => println("Cleanup") )
