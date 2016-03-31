resolvers += Resolver.mavenLocal

lazy val commonSettings = Seq(
  organization := "net.magik6k.lxcadmin",
  version := "0.1.1",
  // set the Scala version used for the project
  scalaVersion := "2.11.7"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
  	libraryDependencies += "net.magik6k" % "jliblxc" % "0.1.0",
  	libraryDependencies += "net.magik6k.jwwf" % "jwwf" % "0.3.0"
  )
