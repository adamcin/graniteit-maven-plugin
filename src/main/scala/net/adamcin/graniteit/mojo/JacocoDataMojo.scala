package net.adamcin.graniteit.mojo

import org.apache.maven.plugins.annotations.{Mojo, LifecyclePhase, Parameter}
import net.adamcin.graniteit.{SlingJacocoParameters, HttpParameters}
import dispatch._, Defaults._
import org.apache.maven.plugin.MojoFailureException
import java.io.File

/**
 * Downloads the jacoco runtime data from the server using Sling JacocoServlet,
 * which was introduced in org.apache.sling.junit.core:1.0.9-SNAPSHOT
 * @since 0.8.0
 */
@Mojo(name = "jacoco-data",
  defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
  threadSafe = true)
class JacocoDataMojo
  extends BaseSlingJunitMojo
  with HttpParameters
  with SlingJacocoParameters {

  /**
   * Set to true to specifically disable this goal
   */
  @Parameter(property = "graniteit.skip.jacoco-exec")
  val skip = false

  /**
   * Specify the filename of the downloaded Jacoco exec data
   */
  @Parameter(defaultValue = "${project.build.directory}/remote-jacoco.exec")
  var jacocoDataFile: File = null

  /**
   * Specify the classifier used when attaching the jacoco data artifact to
   * the project
   */
  @Parameter(defaultValue = "jacoco-data")
  val jacocoDataClassifier: String = null

  override def execute(): Unit = {
    skipWithTestsOrExecute(skip) {

      ifJacocoAvailable {
        if (jacocoDataFile.exists) {
          jacocoDataFile.delete()
        }

        val req = urlForPath(jacocoExecPath)
        expedite(req, Http(req > as.File(jacocoDataFile)).either)._2 match {
          case Right(resp) => {
            val jacocoExecArtifact = repositorySystem.createArtifactWithClassifier(
              project.getGroupId,
              project.getArtifactId,
              project.getVersion,
              "exec",
              jacocoDataClassifier
            )
            jacocoExecArtifact.setFile(jacocoDataFile)
            project.addAttachedArtifact(jacocoExecArtifact)
            getLog.info("Jacoco Runtime Data successfully retrieved.")
          }
          case Left(ex: Throwable) => {
            jacocoDataFile.delete()
            if (failOnJacocoError) {
              throw new MojoFailureException("Failed to retrieve Jacoco runtime data. " + ex.getMessage)
            } else {
              getLog.warn("Failed to retrieve Jacoco runtime data. " + ex.getMessage)
            }
          }
        }
      }
    }
  }
}
