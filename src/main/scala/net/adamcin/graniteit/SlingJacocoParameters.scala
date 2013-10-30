package net.adamcin.graniteit

import org.apache.maven.plugins.annotations.Parameter
import dispatch._, Defaults._
import org.apache.maven.plugin.MojoFailureException

/**
 * Trait defining common parameters and methods supporting Sling-based Jacoco
 * functionality and reporting
 * @since 0.8.0
 */
trait SlingJacocoParameters extends HttpParameters {

  /**
   * Set to override the Sling Jacoco Servlet path
   */
  @Parameter(defaultValue = "/system/sling/jacoco")
  val jacocoServletPath: String = null

  /**
   * Set to cause the build to fail if the jacoco servlet request
   * fails for any reason
   */
  @Parameter
  val failOnJacocoError = false

  def jacocoExecPath = jacocoServletPath + "/exec"

  def ifJacocoAvailable(body: => Unit): Unit = {
    val req = urlForPath(jacocoServletPath)
    if (isSuccess(req, expedite(req, Http(req))._2)) {
      body
    } else {
      if (failOnJacocoError) {
        throw new MojoFailureException("Jacoco servlet was not available. Failing build.")
      } else {
        getLog.warn("Jacoco servlet was not available. Skipping Jacoco functionality.")
      }
    }
  }
}
