package net.adamcin.graniteit

import org.apache.maven.plugins.annotations.Parameter

/**
 * Created with IntelliJ IDEA.
 * User: madamcin
 * Date: 10/25/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */
trait SlingJacocoParameters {

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
}
