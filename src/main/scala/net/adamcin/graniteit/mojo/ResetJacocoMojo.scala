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

import org.apache.maven.plugins.annotations.{Parameter, LifecyclePhase, Mojo}
import net.adamcin.graniteit.{SlingJacocoParameters, HttpParameters}
import dispatch._, Defaults._
import org.apache.maven.plugin.MojoFailureException

/**
 * Resets the remote jacoco data prior to execution of the integration-test phase.
 * Because the Jacoco runtime is singular for the instance, be careful to avoid
 * running multiple content-package-it builds against the same server at the same
 * time, as that might result in corrupt code coverage results.
 * @since 0.8.0
 */
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
