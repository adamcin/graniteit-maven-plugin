/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.graniteit.mojo

import scala.collection.JavaConverters._
import org.apache.maven.lifecycle.mapping.{Lifecycle, LifecycleMapping}
import org.codehaus.plexus.component.annotations.{Requirement, Component}
import org.apache.maven.artifact.handler.ArtifactHandler
import net.adamcin.graniteit.Util
import org.codehaus.plexus.logging.Logger

/**
 * Constants for the {@link ContentPackageITLifecycleMapping} component
 */
object ContentPackageITLifecycleMapping {

  /**
   * compiled pre-integration-test phase config
   */
  final val preITPhase = List(

    // upload the main project artifact
    Util.graniteitGoal("upload-content-package"),

    // [sling-junit] upload sling junit framework if used by tests
    Util.graniteitGoal("upload-sling-junit"),

    // upload server-side tests and content
    Util.graniteitGoal("upload-tests"),

    // wait for server to activate installed bundles
    Util.graniteitGoal("wait-for-server"),

    // set User properties to allow HTTP connections from integration test classes
    Util.graniteitGoal("set-http-properties"),

    // [sling-junit] initialize a sling junit resource and wait for the framework
    Util.graniteitGoal("init-sling-junit"),

    // [sling-junit] attempt to reset the sling jacoco servlet
    Util.graniteitGoal("reset-jacoco")

  ).mkString(",")

  // run the maven-failsafe-plugin integration-test goal
  final val itPhase = Util.failsafeGoal("integration-test")

  // download the jacoco.exec file and attach it as an artifact
  final val postITPhase = Util.graniteitGoal("jacoco-data")

  // execute the maven-failsafe-plugin verify goal
  final val verifyPhase = Util.failsafeGoal("verify")

  final val ROLE = classOf[LifecycleMapping]
  final val ROLE_HINT = Util.PACKAGING
}

/**
 * "content-package-it" {@link LifecycleMapping} implementation designed to merge explicit integration-test phase mappings
 * with whatever the "content-package" {@link LifecycleMapping} component defines
 */
@Component(role = ContentPackageITLifecycleMapping.ROLE, hint = ContentPackageITLifecycleMapping.ROLE_HINT)
class ContentPackageITLifecycleMapping extends LifecycleMapping {

  @Requirement(role = ContentPackageITLifecycleMapping.ROLE, hint = "content-package")
  var cplm: LifecycleMapping = null

  @Requirement(role = classOf[Logger])
  var log: Logger = null

  def getLifecycles: java.util.Map[String, Lifecycle] = {
    if ("false" != System.getProperty(Util.PROP_DISABLE_LIFECYLCES, "false")) {
      log.warn(ContentPackageITLifecycleMapping.ROLE_HINT + " default lifecycle downgraded to content-package default lifecycle by property ${" + Util.PROP_DISABLE_LIFECYLCES + "}")
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
        .updated("pre-integration-test", ContentPackageITLifecycleMapping.preITPhase)
        .updated("integration-test", ContentPackageITLifecycleMapping.itPhase)
        .updated("post-integration-test", ContentPackageITLifecycleMapping.postITPhase)
        .updated("verify", ContentPackageITLifecycleMapping.verifyPhase)
      transformed.setPhases(transMap.asJava)
      (name, transformed)
    } else {
      lifecycle
    }
  }

  def getOptionalMojos(p1: String): java.util.List[String] = cplm.getOptionalMojos(p1)

  def getPhases(p1: String): java.util.Map[String, String] = cplm.getPhases(p1)
}

/**
 * "content-package-it" {@link ArtifactHandler} that simply wraps the component defined by the
 * content-package-maven-plugin
 */
@Component(role = classOf[ArtifactHandler], hint = ContentPackageITLifecycleMapping.ROLE_HINT)
class ContentPackageITArtifactHandler extends ArtifactHandler {

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