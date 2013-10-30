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

import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import org.apache.maven.plugin.MojoFailureException
import dispatch._, Defaults._
import net.adamcin.graniteit.HttpParameters

/**
 * Waits for server readiness before continuing on to integration-test phase. Fails the build if
 * the configured timeout is exceeded.
 * @since 0.6.0
 */
@Mojo(
  name = "wait-for-server",
  defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
  threadSafe = true)
class WaitForServerMojo
  extends BaseITMojo
  with HttpParameters {

  /**
   * Set to true to specifically disable this goal
   */
  @Parameter(property = "graniteit.skip.wait-for-server")
  val skip = false

  /**
   * Number of seconds to wait while pinging the configured IT server for the expected response
   */
  @Parameter(defaultValue = "60")
  val timeout = 60

  /**
   * The server path to ping.
   */
  @Parameter(defaultValue = "/libs/granite/core/content/login.html")
  val serverReadyPath = "/"

  /**
   * The content that is expected anywhere in the response text.
   */
  @Parameter(defaultValue = "QUICKSTART_HOMEPAGE")
  val expectedContent = ""

  def checkContent(response: Future[String]): Future[Boolean] = {
    response.fold(
      (ex) => {
        getLog.info("server ready check exception: " + ex.getMessage)
        false
      },
      (content) => if (content contains expectedContent) {
        getLog.info("server ready check succeeded")
        true
      } else {
        getLog.info("server ready check failed to return expected content")
        false
      }
    )
  }

  override def execute() {
    skipWithTestsOrExecute(skip) {
      val until = System.currentTimeMillis() + (timeout * 1000)
      if (!waitForResponse(0)(until, () => urlForPath(serverReadyPath).subject OK as.String, checkContent)) {
        throw new MojoFailureException("Server failed to respond as expected within " + timeout + " seconds.")
      }
    }
  }
}
