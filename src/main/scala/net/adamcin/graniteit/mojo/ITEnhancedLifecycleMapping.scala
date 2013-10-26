package net.adamcin.graniteit.mojo

import scala.collection.JavaConverters._
import org.apache.maven.lifecycle.mapping.{Lifecycle, LifecycleMapping}
import org.codehaus.plexus.component.annotations.{Requirement, Component}
import org.apache.maven.artifact.handler.ArtifactHandler
import net.adamcin.graniteit.Util
import org.codehaus.plexus.logging.Logger

/**
 *
 */
object ITEnhancedLifecycleMapping {

  /**
   * convenience method to build the signature for a goal defined by this plugin
   * @param goal the goal name
   * @return
   */
  def graniteitGoal(goal: String) = List(Util.GROUP_ID, Util.ARTIFACT_ID, goal).mkString(":")

  /**
   * convenience method to build the signature for a goal defined by the maven-failsafe-plugin
   * @param goal the goal name
   * @return
   */
  def failsafeGoal(goal: String) = List("org.apache.maven.plugins", "maven-failsafe-plugin", goal).mkString(":")

  /**
   * compiled pre-integration-test phase config
   */
  final val preITPhase = List(

    // upload the main project artifact
    graniteitGoal("upload-content-package"),

    // [sling-junit] upload sling junit framework if used by tests
    graniteitGoal("upload-sling-junit"),

    // upload server-side tests and content
    graniteitGoal("upload-tests"),

    // wait for server to activate installed bundles
    graniteitGoal("wait-for-server"),

    // set User properties to allow HTTP connections from integration test classes
    graniteitGoal("set-http-properties"),

    // [sling-junit] initialize a sling junit resource and wait for the framework
    graniteitGoal("init-sling-junit"),

    // [sling-junit] attempt to reset the sling jacoco servlet
    graniteitGoal("reset-jacoco")

  ).mkString(",")

  // run the maven-failsafe-plugin integration-test goal
  final val itPhase = failsafeGoal("integration-test")

  // download the jacoco.exec file and attach it as an artifact
  final val postITPhase = graniteitGoal("jacoco-exec")

  // execute the maven-failsafe-plugin verify goal
  final val verifyPhase = failsafeGoal("verify")

  final val ROLE = classOf[LifecycleMapping]
  final val ROLE_HINT = Util.PACKAGING
}

@Component(role = ITEnhancedLifecycleMapping.ROLE, hint = ITEnhancedLifecycleMapping.ROLE_HINT)
class ITEnhancedLifecycleMapping extends LifecycleMapping {

  @Requirement(role = ITEnhancedLifecycleMapping.ROLE, hint = "content-package")
  var cplm: LifecycleMapping = null

  @Requirement(role = classOf[Logger])
  var log: Logger = null

  def getLifecycles: java.util.Map[String, Lifecycle] = {
    if ("false" != System.getProperty(Util.PROP_DISABLE_LIFECYLCES, "false")) {
      log.warn(Util.PACKAGING + " default lifecycle downgraded to content-package default lifecycle by property ${" + Util.PROP_DISABLE_LIFECYLCES + "}")
      cplm.getLifecycles
    } else {
      cplm.getLifecycles.asScala.map(transformDefaultLifecyle).asJava
    }
  }

  def transformDefaultLifecyle(lifecycle: (String, Lifecycle)): (String, Lifecycle) = {
    val (name, value) = lifecycle
    if (name == "default") {
      val transformed = new Lifecycle
      transformed.setId(value.getId)
      val transMap = (Map.empty[String, String] ++ value.getPhases.asScala)
        .updated("pre-integration-test", ITEnhancedLifecycleMapping.preITPhase)
        .updated("integration-test", ITEnhancedLifecycleMapping.itPhase)
        .updated("post-integration-test", ITEnhancedLifecycleMapping.postITPhase)
        .updated("verify", ITEnhancedLifecycleMapping.verifyPhase)
      transformed.setPhases(transMap.asJava)
      (name, transformed)
    } else {
      lifecycle
    }
  }

  def getOptionalMojos(p1: String): java.util.List[String] = cplm.getOptionalMojos(p1)

  def getPhases(p1: String): java.util.Map[String, String] = cplm.getPhases(p1)
}

@Component(role = classOf[ArtifactHandler], hint = ITEnhancedLifecycleMapping.ROLE_HINT)
class ITEnhancedArtifactHandler extends ArtifactHandler {

  @Requirement(role = classOf[ArtifactHandler], hint = "content-package")
  var cpah: ArtifactHandler = null

  def getExtension: String = cpah.getExtension

  def getDirectory: String = cpah.getDirectory

  def getClassifier: String = cpah.getClassifier

  def getPackaging: String = cpah.getPackaging

  def isIncludesDependencies: Boolean = cpah.isIncludesDependencies

  def getLanguage: String = cpah.getLanguage

  def isAddedToClasspath: Boolean = cpah.isAddedToClasspath
}