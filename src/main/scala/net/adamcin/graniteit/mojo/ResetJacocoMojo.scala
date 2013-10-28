package net.adamcin.graniteit.mojo

import org.apache.maven.plugins.annotations.{Parameter, LifecyclePhase, Mojo}
import net.adamcin.graniteit.{SlingJacocoParameters, HttpParameters}
import dispatch._, Defaults._
import org.apache.maven.plugin.MojoFailureException

@Mojo(name = "reset-jacoco",
  defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
  threadSafe = true)
class ResetJacocoMojo
  extends BaseSlingJunitMojo
  with HttpParameters
  with SlingJacocoParameters {

  /**
   * set to true to specifically disable this goal
   */
  @Parameter(property = "graniteit.skip.reset-jacoco")
  val skip = false

  override def execute(): Unit = {
    skipWithTestsOrExecute(skip) {

      ifJacocoAvailable {
        val req = urlForPath(jacocoExecPath) << Map()
        val resp = expedite(req, Http(req))._2

        if (isSuccess(req, resp) && resp.getContentType == "application/octet-stream") {
          getLog.info("Jacoco Runtime Data successfully reset.")
        } else {
          if (failOnJacocoError) {
            throw new MojoFailureException("Failed to reset jacoco runtime content. " + getReqRespLogMessage(req, resp))
          } else {
            getLog.warn("Failed to reset jacoco runtime content. " + getReqRespLogMessage(req, resp))
          }
        }
      }
    }
  }
}
