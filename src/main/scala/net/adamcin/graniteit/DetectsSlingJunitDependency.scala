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

package net.adamcin.graniteit

import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.artifact.Artifact

import scala.collection.JavaConverters._
import org.apache.maven.artifact.versioning.ArtifactVersion

/**
 * Defines common parameters and methods for supporting Sling Junit Framework
 * functionality
 * @since 0.6.0
 */
trait DetectsSlingJunitDependency
  extends ResolvesArtifacts {

  final val groupIdSlingJunit = "org.apache.sling"
  final val artifactIdCore = "org.apache.sling.junit.core"
  final val artifactIdRemote = "org.apache.sling.junit.remote"
  final val artifactIdScriptable = "org.apache.sling.junit.scriptable"

  final val slingJunitArtifactIds = Set(artifactIdCore, artifactIdRemote, artifactIdScriptable)

  /**
   * Specify to override the resolved version of Sling Junit Framework dependencies. Leave blank
   * to detect the version from project-defined sling junit dependencies
   */
  @Parameter(property = "sling.junit.version")
  val slingJunitVersion: String = null

  lazy val explicitDependencies = dependencies.filter((artifact) => {
    artifact.getGroupId == groupIdSlingJunit &&
      slingJunitArtifactIds.contains(artifact.getArtifactId)
  }).foldLeft(Map[String, Artifact]()) {
    (m, a) => m.updated(a.getArtifactId, a)
  }

  lazy val effectiveSlingJunitVersion = Option(slingJunitVersion) match {
    case Some(v) => Option(v)
    case None => explicitDependencies.foldLeft(Option.empty[ArtifactVersion]) {
      (leftVersion: Option[ArtifactVersion], artifact: (String, Artifact)) => {
        val otherVersion = Option(artifact._2.getSelectedVersion)

        (leftVersion, otherVersion) match {
          case (None, Some(ov)) => otherVersion
          case (Some(v), Some(ov)) => {
            if (v.asInstanceOf[Comparable[ArtifactVersion]].compareTo(ov) < 0) {
              otherVersion
            } else {
              leftVersion
            }
          }
          case _ => leftVersion
        }
      }

    } match {
      case Some(v) => Option(v.toString)
      case None => None
    }
  }

  lazy val slingJunitSupportEnabled = !effectiveSlingJunitVersion.isEmpty

  lazy val slingJunitArtifacts: List[Artifact] = {
    effectiveSlingJunitVersion match {
      case Some(version) => slingJunitArtifactIds.flatMap {
        (artifactId: String) => explicitDependencies.get(artifactId) match {
          case Some(a) => Some(a)
          case None => resolveByCoordinates(groupIdSlingJunit, artifactId, version, "jar", null)
        }
      }.toList
      case None => Nil
    }
  }
}
