package net.adamcin.graniteit.mojo

import scala.collection.JavaConverters._
import org.apache.maven.lifecycle.mapping.{Lifecycle, LifecycleMapping}
import org.codehaus.plexus.component.annotations.{Requirement, Component}
import org.apache.maven.artifact.handler.ArtifactHandler
import net.adamcin.graniteit.Util

object ITEnhancedLifecycleMapping {
  def idForGoal(goal: String) = List(Util.GROUP_ID, Util.ARTIFACT_ID, goal).mkString(":")
  final val preITPhase = List(
    idForGoal("upload-content-package"),
    idForGoal("upload-sling-junit"),
    idForGoal("upload-tests"),
    idForGoal("wait-for-server")
  ).mkString(",")
  final val itPhase = List(
    idForGoal("set-http-properties"),
    idForGoal("init-sling-junit"),
    idForGoal("reset-sling-jacoco"),
    "org.apache.maven.plugins:maven-failsafe-plugin:integration-test"
  ).mkString(",")
  final val postITPhase = idForGoal("jacoco-exec")
  final val verifyPhase = "org.apache.maven.plugins:maven-failsafe-plugin:verify"
  final val ROLE = classOf[LifecycleMapping]
  final val ROLE_HINT = Util.PACKAGING
}

@Component(role = ITEnhancedLifecycleMapping.ROLE, hint = ITEnhancedLifecycleMapping.ROLE_HINT)
class ITEnhancedLifecycleMapping extends LifecycleMapping {

  @Requirement(role = ITEnhancedLifecycleMapping.ROLE, hint = "content-package")
  var cplm: LifecycleMapping = null

  def getLifecycles: java.util.Map[String, Lifecycle] = {
    cplm.getLifecycles.asScala.map(transformDefaultLifecyle).asJava
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