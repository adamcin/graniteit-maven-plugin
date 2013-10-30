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

import dispatch._, Defaults._
import java.io.File
import org.apache.maven.plugin.MojoExecutionException
import org.slf4j.LoggerFactory
import org.apache.jackrabbit.vault.util.Text
import org.apache.maven.plugins.annotations.Parameter
import java.util.jar.{Manifest, JarFile}
import com.ning.http.multipart.{FilePart, StringPart}

/**
 * Trait defining common mojo parameters and methods for uploading OSGi bundles to the configured CQ server
 * using POST or PUT HTTP methods
 * @since 0.8.0
 */
trait UploadsBundles extends HttpParameters {
  private val log = LoggerFactory.getLogger(getClass)

  /**
   * Specify the OSGi start level of the bundle
   */
  @Parameter(defaultValue = "30")
  val startLevel = 30

  /**
   * Use a PUT request instead of a POST request
   */
  @Parameter
  val usePut = false

  /**
   * Set to true to fail the build if an error occurs during upload
   */
  @Parameter
  val failOnError = true

  final val defaultTestBundlePathPrefix = "/system/console"

  /**
   * Set the request path prefix under which test bundles will be installed (an "/install/" segment will be appended
   * to this value and if <code>usePut</code> is set to <code>true</code> then the start level will be appended to that.
   */
  @Parameter(defaultValue = defaultTestBundlePathPrefix)
  var testBundlePathPrefix: String = defaultTestBundlePathPrefix

  def getTestBundleRepoPath(filename: String): String = {
    testBundlePathPrefix.replaceAll("/+$", "") + "/install/" + startLevel + "/" + filename
  }

  def getTestBundlePath(file: File): String = if (usePut) {
    getTestBundleRepoPath(file.getName)
  } else {
    testBundlePathPrefix.replaceAll("/+$", "") + "/install"
  }

  def getBundleSymbolicName(file: File): Either[Throwable, String] = {
    try {
      (for {
        f: File <- Option(file)
        m: Manifest <- Option(new JarFile(f).getManifest)
        s: String <- Option(m.getMainAttributes.getValue("Bundle-SymbolicName"))
      } yield Right(s)).getOrElse(Left(new MojoExecutionException(file.getAbsolutePath + " is not a bundle.")))
    } catch {
      case ex: Throwable => Left(ex)
    }
  }

  def uploadTestBundle(file: File): Either[Throwable, List[String]] = {
    handleError(getBundleSymbolicName(file) match {
      case Right(symbolicName) => {
        if (usePut) {
          putTestBundle(symbolicName, file)
        } else {
          // replace with POST to /system/console
          postTestBundle(symbolicName, file)
        }
      }
      case Left(ex) => Left(ex)
    })
  }

  def postTestBundle(symbolicName: String, file: File): Either[Throwable, List[String]] = {
    val postReq = urlForPath(getTestBundlePath(file)).POST.underlying(_.setHeader("Referer", "about:blank"))
    val req = List(
      new StringPart("action", "install"),
      new StringPart("_noredir_", "_noredir_"),
      new FilePart("bundlefile", file.getName, file),
      new StringPart("bundlestartlevel", startLevel.toString),
      new StringPart("bundlestart", "start"),
      new StringPart("refreshPackages", "true")
    ).foldLeft(postReq) {
      (req, part) => req.addBodyPart(part)
    }

    expedite(req, Http(req).either)._2 match {
      case Right(resp) => {
        if (isSuccess(req, resp)) {
          Right(List("Successfully installed bundle " + symbolicName))
        } else {
          Left(new MojoExecutionException("Failed to install bundle " + symbolicName))
        }
      }
      case Left(ex: Throwable) => Left(ex)
    }
  }

  def handleError(message: Either[Throwable, List[String]], messages: List[String] = Nil): Either[Throwable, List[String]] = {
    message match {
      case Right(m) => Right(messages ++ m)
      case Left(ex) => {
        if (failOnError) {
          Left(ex)
        } else {
          Right(messages ++ List(ex.getMessage))
        }
      }
    }
  }

  /**
   * Puts the specified file to the configured test bundle install location
   * @param file file to put
   * @return either log messages or a throwable
   */
  def putTestBundle(symbolicName: String, file: File): Either[Throwable, List[String]] = {
    putBundleToPath(symbolicName, file, getTestBundleRepoPath(file.getName))
  }

  def putBundleToPath(symbolicName: String, file: File, path: String): Either[Throwable, List[String]] = {
    lazy val (putReq, putResp) = {
      val req = urlForPath(path).underlying(_.addHeader("Content-Type", "application/java-archive")) <<< file
      expedite(req, Http(req))
    }

    val fromMkdirs: Either[Throwable, List[String]] = if (!skipMkdirs) {
      val (mkReq, mkResp) = mkdirs(Text.getRelativeParent(path, 1))
      if (isSuccess(mkReq, mkResp)) {
        Right(List("successfully created path at " + mkReq.url))
      } else {
        log.debug("[putBundle] {}", getReqRespLogMessage(mkReq, mkResp))
        Left(new MojoExecutionException("failed to create path at: " + mkReq.url))
      }
    } else {
      Right(List("skipping mkdirs"))
    }

    fromMkdirs match {
      case Right(messages) => {
        if (file == null || !file.exists || !file.canRead) {
          Left(new MojoExecutionException("A valid file must be specified"))
        }
        if (isSuccess(putReq, putResp)) {
          Right(messages ++ List("successfully uploaded " + file + " to " + putReq.url))
        } else {
          log.debug("[putBundle] {}", getReqRespLogMessage(putReq, putResp))
          Left(new MojoExecutionException("failed to upload " + file + " to " + putReq.url))
        }
      }
      case _ => fromMkdirs
    }
  }

}