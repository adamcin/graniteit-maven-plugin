package net.adamcin.graniteit

import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.artifact.Artifact

import scala.collection.JavaConverters._
import org.apache.maven.artifact.versioning.ArtifactVersion

trait DetectsSlingJunitDependency
  extends ResolvesArtifacts {

  final val groupIdSlingJunit = "org.apache.sling"
  final val artifactIdCore = "org.apache.sling.junit.core"
  final val artifactIdRemote = "org.apache.sling.junit.remote"
  final val artifactIdScriptable = "org.apache.sling.junit.scriptable"

  final val slingJunitArtifactIds = Set(artifactIdCore, artifactIdRemote, artifactIdScriptable)

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
