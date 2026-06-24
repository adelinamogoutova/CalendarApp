ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"

lazy val root = (project in file("."))
  .settings(
    name := "CalendarApp",

libraryDependencies ++= Seq(
      "org.mnode.ical4j" %  "ical4j"       % "4.0.0",
      "org.scalafx"      %% "scalafx"      % "23.0.1-R34",
      "org.slf4j"        %  "slf4j-simple" % "2.0.9"
    ),
    libraryDependencies ++= Seq("base", "controls", "fxml", "graphics", "media", "swing", "web").map(m =>
      "org.openjfx" % s"javafx-$m" % "21" classifier osName
    )
  )

lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     =>
    if (System.getProperty("os.arch").startsWith("aarch64")) "mac-aarch64" else "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}
