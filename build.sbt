import sbtorgpolicies.model.scalac

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val common = project
  .in(file("modules/common"))
  .settings(moduleName := "frees-rpc-common")
  .settings(commonSettings)
  .disablePlugins(ScriptedPlugin)

lazy val internal = project
  .in(file("modules/internal"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "frees-rpc-internal")
  .settings(internalSettings)
  .disablePlugins(ScriptedPlugin)

lazy val client = project
  .in(file("modules/client"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(internal)
  .dependsOn(testing % "test->test")
  .settings(moduleName := "frees-rpc-client-core")
  .settings(clientCoreSettings)
  .disablePlugins(ScriptedPlugin)

lazy val `client-netty` = project
  .in(file("modules/client-netty"))
  .dependsOn(client % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-client-netty")
  .settings(clientNettySettings)
  .disablePlugins(ScriptedPlugin)

lazy val `client-okhttp` = project
  .in(file("modules/client-okhttp"))
  .dependsOn(client % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-client-okhttp")
  .settings(clientOkHttpSettings)
  .disablePlugins(ScriptedPlugin)

lazy val server = project
  .in(file("modules/server"))
  .dependsOn(common % "compile->compile;test->test")
  .dependsOn(client % "test->test")
  .dependsOn(internal)
  .dependsOn(testing % "test->test")
  .settings(moduleName := "frees-rpc-server")
  .settings(serverSettings)
  .disablePlugins(ScriptedPlugin)

lazy val config = project
  .in(file("modules/config"))
  .dependsOn(common % "test->test")
  .dependsOn(client % "compile->compile;test->test")
  .dependsOn(server % "compile->compile;test->test")
  .dependsOn(testing % "test->test")
  .settings(moduleName := "frees-rpc-config")
  .settings(configSettings)
  .disablePlugins(ScriptedPlugin)

lazy val interceptors = project
  .in(file("modules/interceptors"))
  .settings(moduleName := "frees-rpc-interceptors")
  .settings(interceptorsSettings)
  .disablePlugins(ScriptedPlugin)

lazy val `prometheus-shared` = project
  .in(file("modules/prometheus/shared"))
  .dependsOn(interceptors % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-prometheus-shared")
  .settings(prometheusSettings)
  .disablePlugins(ScriptedPlugin)

lazy val `prometheus-server` = project
  .in(file("modules/prometheus/server"))
  .dependsOn(`prometheus-shared` % "compile->compile;test->test")
  .dependsOn(server % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-prometheus-server")
  .disablePlugins(ScriptedPlugin)

lazy val `prometheus-client` = project
  .in(file("modules/prometheus/client"))
  .dependsOn(`prometheus-shared` % "compile->compile;test->test")
  .dependsOn(client % "compile->compile;test->test")
  .dependsOn(server % "test->test")
  .settings(moduleName := "frees-rpc-prometheus-client")
  .settings(prometheusClientSettings)
  .disablePlugins(ScriptedPlugin)

lazy val `dropwizard-server` = project
  .in(file("modules/dropwizard/server"))
  .dependsOn(`prometheus-server` % "compile->compile;test->test")
  .dependsOn(server % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-dropwizard-server")
  .settings(dropwizardSettings)
  .disablePlugins(ScriptedPlugin)

lazy val `dropwizard-client` = project
  .in(file("modules/dropwizard/client"))
  .dependsOn(`prometheus-client` % "compile->compile;test->test")
  .dependsOn(client % "compile->compile;test->test")
  .dependsOn(server % "test->test")
  .settings(moduleName := "frees-rpc-dropwizard-client")
  .settings(dropwizardSettings)
  .disablePlugins(ScriptedPlugin)

lazy val testing = project
  .in(file("modules/testing"))
  .settings(moduleName := "frees-rpc-testing")
  .settings(testingSettings)
  .disablePlugins(ScriptedPlugin)

lazy val ssl = project
  .in(file("modules/ssl"))
  .dependsOn(server % "test->test")
  .dependsOn(`client-netty` % "compile->compile;test->test")
  .settings(moduleName := "frees-rpc-netty-ssl")
  .settings(nettySslSettings)
  .disablePlugins(ScriptedPlugin)

lazy val `idlgen-core` = project
  .in(file("modules/idlgen/core"))
  .dependsOn(internal)
  .dependsOn(client % "test->test")
  .settings(moduleName := "frees-rpc-idlgen-core")
  .disablePlugins(ScriptedPlugin)

lazy val `idlgen-sbt` = project
  .in(file("modules/idlgen/plugin"))
  .dependsOn(`idlgen-core`)
  .settings(moduleName := "sbt-frees-rpc-idlgen")
  .settings(sbtPlugin := true)
  .settings(crossScalaVersions := Seq(scalac.`2.12`))
  .settings(sbtPluginSettings: _*)
  .enablePlugins(BuildInfoPlugin)
  .settings(buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion))
  .settings(buildInfoPackage := "freestyle.rpc.idlgen")

//////////////////////////
//// MODULES REGISTRY ////
//////////////////////////

lazy val allModules: Seq[ProjectReference] = Seq(
  common,
  internal,
  client,
  `client-netty`,
  `client-okhttp`,
  server,
  config,
  interceptors,
  `prometheus-shared`,
  `prometheus-client`,
  `prometheus-server`,
  `dropwizard-server`,
  `dropwizard-client`,
  testing,
  ssl,
  `idlgen-core`
)

lazy val allModulesDeps: Seq[ClasspathDependency] =
  allModules.map(ClasspathDependency(_, None))

lazy val root = project
  .in(file("."))
  .settings(name := "freestyle-rpc")
  .settings(noPublishSettings)
  .aggregate(allModules: _*)
  .dependsOn(allModulesDeps: _*)
  .disablePlugins(ScriptedPlugin)

lazy val docs = project
  .in(file("docs"))
  .aggregate(allModules: _*)
  .dependsOn(allModulesDeps: _*)
  .settings(name := "frees-rpc-docs")
  .settings(noPublishSettings)
  .settings(docsSettings)
  .enablePlugins(TutPlugin)