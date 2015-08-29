// Copyright (c) 2015 Ben Zimmer. All rights reserved.

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "3.0.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.12.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.1")

addSbtPlugin("org.ensime" % "ensime-sbt" % "0.1.7")