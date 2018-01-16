import dependencies.DependenciesPlugin.autoImport._
import freestyle.FreestylePlugin
import freestyle.FreestylePlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.templates.badges._
import sbtorgpolicies.runnable.syntax._
import tut.TutPlugin.autoImport._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = FreestylePlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val V = new {
      val avro4s: String = "1.8.0"
      val frees: String  = "0.6.1"
      val grpc: String   = "1.9.0"
      // See https://github.com/grpc/grpc-java/blob/master/SECURITY.md#netty
      val netty              = "4.1.17.Final"
      val nettyBoringssl     = "2.0.7.Final"
      val pbdirect: String   = "0.0.8"
      val prometheus: String = "0.1.0"
      val scalameta: String  = "1.8.0"
    }

    lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
      scalacOptions := Seq(
        "-deprecation",
        "-encoding",
        "UTF-8",
        "-feature",
        "-unchecked",
        "-language:higherKinds"),
      libraryDependencies ++= Seq(
        %%("cats-effect")        % Test,
        %%("cats-core")          % Test,
        %%("scalamockScalatest") % Test
      )
    )

    lazy val asyncSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("cats-core"),
        %%("cats-effect"),
        %%("monix"),
        %%("shapeless")           % Test,
        %%("frees-core", V.frees) % Test
      )
    )

    lazy val internalSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("cats-effect"),
        %%("frees-async-guava", V.frees) exclude ("com.google.guava", "guava"),
        %("grpc-core", V.grpc),
        %("grpc-stub", V.grpc),
        %%("monix"),
        %%("pbdirect", V.pbdirect),
        %%("avro4s", V.avro4s),
        %("grpc-testing", V.grpc) % Test,
        %%("scalamockScalatest")  % Test
      )
    )

    lazy val clientCoreSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("frees-async-cats-effect", V.frees),
        %("grpc-testing", V.grpc) % Test,
        %%("scalamockScalatest")  % Test
      )
    )

    lazy val clientNettySettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("grpc-netty", V.grpc)
      )
    )

    lazy val clientOkHttpSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("grpc-okhttp", V.grpc)
      )
    )

    lazy val serverSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %%("frees-async-cats-effect", V.frees),
        %("grpc-core", V.grpc),
        %("grpc-netty", V.grpc),
        %("grpc-testing", V.grpc) % Test,
        %%("scalamockScalatest")  % Test
      )
    )

    lazy val configSettings = Seq(
      libraryDependencies ++= Seq(
        %%("frees-config", V.frees),
        %("grpc-testing", V.grpc) % Test
      )
    )

    lazy val interceptorsSettings: Seq[Def.Setting[_]] = Seq(
      libraryDependencies ++= Seq(
        %("grpc-core", V.grpc)
      )
    )

    lazy val prometheusSettings = Seq(
      libraryDependencies ++= Seq(
        "io.prometheus" % "simpleclient" % V.prometheus
      )
    )

    lazy val prometheusClientSettings = Seq(
      libraryDependencies ++= Seq(
        %("grpc-netty", V.grpc) % Test
      )
    )

    lazy val docsSettings = Seq(
      // Pointing to https://github.com/frees-io/freestyle/tree/master/docs/src/main/tut/docs/rpc
      tutTargetDirectory := baseDirectory.value.getParentFile.getParentFile / "docs" / "src" / "main" / "tut" / "docs" / "rpc"
    )

  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      resolvers += Resolver.bintrayRepo("beyondthelines", "maven"),
      orgAfterCISuccessTaskListSetting := List(
        depUpdateDependencyIssues.asRunnableItem,
        orgPublishReleaseTask
          .asRunnableItem(allModules = true, aggregated = false, crossScalaVersions = true),
        orgUpdateDocFiles.asRunnableItem
      ),
      orgBadgeListSetting := List(
        TravisBadge.apply,
        CodecovBadge.apply, { info =>
          MavenCentralBadge.apply(info.copy(libName = "frees"))
        },
        ScalaLangBadge.apply,
        LicenseBadge.apply,
        // Gitter badge (owner field) can be configured with default value if we migrate it to the frees-io organization
        { info =>
          GitterBadge.apply(info.copy(owner = "47deg", repo = "freestyle"))
        },
        GitHubIssuesBadge.apply
      )
    ) ++ Seq(
      addCompilerPlugin(%%("scalameta-paradise") cross CrossVersion.full),
      libraryDependencies ++= commonDeps ++ Seq(%%("scalameta", V.scalameta)),
      scalacOptions ++= Seq("-Ywarn-unused-import", "-Xplugin-require:macroparadise"),
      scalacOptions in Tut ~= (_ filterNot Set("-Ywarn-unused-import", "-Xlint").contains),
      scalacOptions in (Compile, console) ~= (_ filterNot (_ contains "paradise")) // macroparadise plugin doesn't work in repl yet.
    ) ++ scalaMetaSettings ++ sharedReleaseProcess

}
