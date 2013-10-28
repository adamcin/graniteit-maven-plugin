package net.adamcin.graniteit.mojo

import org.apache.maven.plugins.annotations.{Mojo, LifecyclePhase, Parameter}
import net.adamcin.graniteit.{SlingJacocoParameters, HttpParameters}
import dispatch._, Defaults._
import org.apache.maven.plugin.MojoFailureException
import java.io.File

@Mojo(name = "jacoco-exec",
  defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
  threadSafe = true)
class JacocoExecMojo
  extends BaseSlingJunitMojo
  with HttpParameters
  with SlingJacocoParameters {

  @Parameter(property = "graniteit.skip.jacoco-exec")
  val skip = false

  @Parameter(defaultValue = "${project.build.directory}/jacoco.exec")
  var jacocoExecFile: File = null

  @Parameter(defaultValue = "jacoco-data")
  val jacocoExecClassifier: String = null

  override def execute(): Unit = {
    skipWithTestsOrExecute(skip) {

      ifJacocoAvailable {
        if (jacocoExecFile.exists) {
          jacocoExecFile.delete()
        }

        val req = urlForPath(jacocoExecPath)
        expedite(req, Http(req > as.File(jacocoExecFile)).either)._2 match {
          case Right(resp) => {
            val jacocoExecArtifact = repositorySystem.createArtifactWithClassifier(
              project.getGroupId,
              project.getArtifactId,
              project.getVersion,
              "exec",
              jacocoExecClassifier
            )
            jacocoExecArtifact.setFile(jacocoExecFile)
            project.addAttachedArtifact(jacocoExecArtifact)
            getLog.info("Jacoco Runtime Data successfully retrieved.")
          }
          case Left(ex: Throwable) => {
            jacocoExecFile.delete()
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
