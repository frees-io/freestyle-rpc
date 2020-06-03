ThisBuild / organization := "io.higherkindness"
ThisBuild / githubOrganization := "47degrees"
ThisBuild / scalaVersion := "2.13.2"
ThisBuild / crossScalaVersions := Seq("2.12.11", "2.13.2")

lazy val checkScalafmt         = "+scalafmtCheckAll; +scalafmtSbtCheck;"
lazy val checkMicrosite        = "+microsite/mdoc;"
lazy val checkIntegrationTests = "+haskell-integration-tests/test;"
lazy val checkTests            = "+coverage; +test; +coverageReport; +coverageAggregate;"

addCommandAlias(
  "ci-test",
  s"$checkScalafmt $checkMicrosite $checkIntegrationTests $checkTests"
)
addCommandAlias("ci-docs", "github; mdoc; headerCreateAll; publishMicrosite")
addCommandAlias("ci-publish", "github; ci-release")

////////////////
//// COMMON ////
////////////////

lazy val `rpc-service` = project
  .in(file("modules/service"))
  .settings(moduleName := "mu-rpc-service")
  .settings(rpcServiceSettings)

lazy val monix = project
  .in(file("modules/monix"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-monix")
  .settings(monixSettings)

lazy val fs2 = project
  .in(file("modules/fs2"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-fs2")
  .settings(fs2Settings)

lazy val config = project
  .in(file("modules/config"))
  .dependsOn(`rpc-service`)
  .dependsOn(server)
  .settings(moduleName := "mu-config")
  .settings(configSettings)

lazy val testing = project
  .in(file("modules/testing"))
  .settings(moduleName := "mu-rpc-testing")
  .settings(testingSettings)

////////////////
//// CLIENT ////
////////////////

lazy val `client-netty` = project
  .in(file("modules/client/netty"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-client-netty")
  .settings(clientNettySettings)

lazy val `client-okhttp` = project
  .in(file("modules/client/okhttp"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-client-okhttp")
  .settings(clientOkHttpSettings)

lazy val `client-cache` = project
  .in(file("modules/client/cache"))
  .settings(moduleName := "mu-rpc-client-cache")
  .settings(clientCacheSettings)

///////////////////
//// NETTY SSL ////
///////////////////

lazy val `netty-ssl` = project
  .in(file("modules/netty-ssl"))
  .settings(moduleName := "mu-rpc-netty-ssl")
  .settings(nettySslSettings)

////////////////
//// SERVER ////
////////////////

lazy val server = project
  .in(file("modules/server"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-server")
  .settings(serverSettings)

/////////////////////
//// HEALTHCHECK ////
/////////////////////

lazy val `health-check` = project
  .in(file("modules/health-check"))
  .dependsOn(`rpc-service`)
  .dependsOn(fs2 % "optional->compile")
  .dependsOn(monix % "optional->compile")
  .settings(healthCheckSettings)
  .settings(moduleName := "mu-rpc-health-check")

////////////////////
//// PROMETHEUS ////
////////////////////

lazy val prometheus = project
  .in(file("modules/metrics/prometheus"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-prometheus")
  .settings(prometheusMetricsSettings)

////////////////////
//// DROPWIZARD ////
////////////////////

lazy val dropwizard = project
  .in(file("modules/metrics/dropwizard"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-dropwizard")
  .settings(dropwizardMetricsSettings)

///////////////////
//// HTTP/REST ////
///////////////////

lazy val http = project
  .in(file("modules/http"))
  .settings(moduleName := "mu-rpc-http")
  .settings(httpSettings)

///////////////
//// KAFKA ////
///////////////

lazy val kafka = project
  .in(file("modules/kafka"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-kafka")
  .settings(kafkaSettings)

////////////////////
//// BENCHMARKS ////
////////////////////

lazy val `benchmarks-vprev` = project
  .in(file("benchmarks/vprev"))
  .settings(
    libraryDependencies ++= Seq(
      "io.higherkindness" %% "mu-rpc-server" % "0.22.1"
    )
  )
  .settings(coverageEnabled := false)
  .settings(moduleName := "mu-benchmarks-vprev")
  .settings(benchmarksSettings)
  .settings(noPublishSettings)
  .enablePlugins(JmhPlugin)

lazy val `benchmarks-vnext` = project
  .in(file("benchmarks/vnext"))
  .dependsOn(server)
  .settings(coverageEnabled := false)
  .settings(moduleName := "mu-benchmarks-vnext")
  .settings(benchmarksSettings)
  .settings(noPublishSettings)
  .enablePlugins(JmhPlugin)

/////////////////////
//// MARSHALLERS ////
/////////////////////

lazy val `marshallers-jodatime` = project
  .in(file("modules/marshallers/jodatime"))
  .dependsOn(`rpc-service`)
  .settings(moduleName := "mu-rpc-marshallers-jodatime")
  .settings(marshallersJodatimeSettings)

///////////////
//// TESTS ////
///////////////

lazy val tests = project
  .in(file("modules/tests"))
  .dependsOn(coreModulesDeps: _*)
  .settings(moduleName := "mu-rpc-tests")
  .settings(testSettings)

//////////////////////////////////////
//// MU-HASKELL INTEGRATION TESTS ////
//////////////////////////////////////

lazy val `haskell-integration-tests` = project
  .in(file("modules/haskell-integration-tests"))
  .settings(noPublishSettings)
  .settings(haskellIntegrationTestSettings)
  .dependsOn(server, `client-netty`, fs2)

//////////////////////////
//// MODULES REGISTRY ////
//////////////////////////

lazy val coreModules: Seq[ProjectReference] = Seq(
  `rpc-service`,
  monix,
  fs2,
  `client-netty`,
  `client-okhttp`,
  `client-cache`,
  server,
  config,
  dropwizard,
  prometheus,
  testing,
  `netty-ssl`,
  http,
  kafka,
  `marshallers-jodatime`,
  `health-check`,
  `benchmarks-vprev`,
  `benchmarks-vnext`
)

lazy val coreModulesDeps: Seq[ClasspathDependency] =
  coreModules.map(ClasspathDependency(_, None))

lazy val testModules: Seq[ProjectReference] = Seq(tests, `haskell-integration-tests`)

lazy val root = project
  .in(file("."))
  .settings(name := "mu-scala")
  .settings(noPublishSettings)
  .aggregate(coreModules: _*)
  .aggregate(testModules: _*)

lazy val microsite = project
  .dependsOn(coreModulesDeps: _*)
  .settings(docsSettings)
  .settings(micrositeSettings)
  .settings(noPublishSettings)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(MdocPlugin)

lazy val `project-docs` = (project in file(".docs"))
  .aggregate(coreModules: _*)
  .dependsOn(coreModulesDeps: _*)
  .settings(moduleName := "mu-project-docs")
  .settings(mdocIn := file(".docs"))
  .settings(mdocOut := file("."))
  .settings(noPublishSettings)
  .enablePlugins(MdocPlugin)
