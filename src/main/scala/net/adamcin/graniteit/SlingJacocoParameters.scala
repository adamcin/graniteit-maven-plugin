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
