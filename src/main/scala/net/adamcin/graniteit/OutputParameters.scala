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

import java.io.File
import org.slf4j.LoggerFactory
import scalax.io.Resource

/**
 * Trait defining common output path variables
 * @since 0.6.0
 * @author Mark Adamcin
 */
trait OutputParameters extends RequiresProject {
  private val log = LoggerFactory.getLogger(getClass)

  /**
   * target directory
   */
  lazy val outputDirectory = getExistingDir(new File(project.getBuild.getDirectory))

  /**
   * target content-package file
   */
  lazy val targetFile: File = new File(outputDirectory, project.getBuild.getFinalName + ".zip")

  /**
   * directory containing graniteit-generated files
   */
  lazy val graniteitDirectory = getExistingDir(new File(outputDirectory, "graniteit"))

  lazy val uploadDirectory = getExistingDir(new File(graniteitDirectory, "upload-content-package"))
  lazy val uploadSha = new File(uploadDirectory, "checksum.sha1")

  lazy val uploadSlingJunitDirectory = getExistingDir(new File(graniteitDirectory, "upload-sling-junit"))
  lazy val uploadSlingJunitSha = new File(uploadSlingJunitDirectory, "checksum.sha1")

  lazy val uploadTestsDirectory = getExistingDir(new File(graniteitDirectory, "upload-tests"))
  lazy val uploadTestsSha = new File(uploadTestsDirectory, "checksum.sha1")

  def getExistingDir(file: File): File = {
    if (!file.exists() && !file.mkdir()) {
      log.error("[getExistingDir] failed to create directory: {}", file)
    }
    file
  }

  def listFiles(file: File): Stream[File] = {
    if (file.isDirectory) {
      file.listFiles().flatMap(listFiles(_)).toStream
    } else {
      Stream(file)
    }
  }

  def inputFileModified(output: File, inputFiles: List[File]): Boolean = {
    inputFileModified(output, inputFiles.toStream)
  }

  def inputFileModified(output: File, inputFiles: Stream[File]): Boolean = {
    !output.exists() || inputFiles.exists {
      (file) => Option(file) match {
        case None => false
        case Some(f) => !file.exists() || file.lastModified() >= output.lastModified()
      }
    }
  }

  def overwriteFile(file: File, content: String) {
    if (file.exists()) file.delete()
    Resource.fromFile(file).write(content)
  }
}