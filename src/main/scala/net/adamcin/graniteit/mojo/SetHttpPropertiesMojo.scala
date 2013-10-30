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
import net.adamcin.graniteit.HttpParameters
import scala.collection.JavaConverters._

/**
 * Exports HTTP properties defined in {@link HttpParameters} and the active Maven Proxy configuration for integration
 * tests to use via {@link java.lang.System#getProperties}.
 * @since 0.6.0
 */
@Mojo(name = "set-http-properties",
  defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
  requiresProject = false,
  threadSafe = true)
class SetHttpPropertiesMojo
  extends BaseITMojo
  with HttpParameters {

  /**
   * Set to true to specifically disable this goal
   */
  @Parameter(property = "graniteit.skip.set-http-properties")
  val skip = false

  /**
   * Maps the effective username value to the specified system property
   */
  @Parameter(defaultValue = "test.server.username")
  val usernameProperty = ""

  /**
   * Maps the effective password value to the specified system property
   */
  @Parameter(defaultValue = "test.server.password")
  val passwordProperty = ""

  /**
   * Maps the effective base URL (including context path, but without trailing slash) to the specified system
   * property
   */
  @Parameter(defaultValue = "test.server.url")
  val baseUrlProperty = ""

  /**
   * Set to true to set the standard java proxy properties using the active maven proxy values
   */
  @Parameter(defaultValue = "true")
  val setProxyProperties = true

  override def execute() {
    skipWithTestsOrExecute(skip) {
      val (u, p) = credentials
      session.getUserProperties.setProperty(baseUrlProperty, baseUrl)
      session.getUserProperties.setProperty(usernameProperty, u)
      session.getUserProperties.setProperty(passwordProperty, p)

      if (setProxyProperties) {
        activeProxy.foreach {
          (proxy) => {
            val proto = proxy.getProtocolAsString.toLowerCase + "."

            session.getUserProperties.setProperty(proto + "proxyHost", proxy.getHost)
            session.getUserProperties.setProperty(proto + "proxyPort", Integer.toString(proxy.getPort))

            Option(proxy.getPrincipal) foreach { session.getUserProperties.setProperty(proto + "proxyUser", _) }
            Option(proxy.getPassword) foreach { session.getUserProperties.setProperty(proto + "proxyPass", _) }
            proxy.getNonProxyHosts.asScala.toList match {
              case Nil => ()
              case hosts => session.getUserProperties.setProperty(proto + "nonProxyHosts", hosts.mkString("|"))
            }
          }
        }
      }
    }
  }
}
