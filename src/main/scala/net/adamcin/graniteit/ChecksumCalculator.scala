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
import java.security.MessageDigest
import scalax.io.Resource

/**
 * Mutable class used to calculate checksums. This is used to establish
 * whether input data for a given goal has changed between maven executions.
 */
class ChecksumCalculator {
  final val md = MessageDigest.getInstance("SHA-1")
  final val nullBytes = "null".getBytes("UTF-8")

  private def update(item: Array[Byte]): ChecksumCalculator = {
    md.update(item)
    this
  }

  def addNull(): ChecksumCalculator = {
    md.update(nullBytes)
    this
  }

  def add(item: AnyVal): ChecksumCalculator = {
    Option(item) match {
      case Some(v) => add(v.toString)
      case None => addNull()
    }
  }

  def add(item: String): ChecksumCalculator = {
    Option(item) match {
      case Some(s) => update(s.getBytes("UTF-8"))
      case None => addNull()
    }
  }

  def add(item: File): ChecksumCalculator = {
    Option(item) match {
      case Some(file) => add(file.getAbsolutePath)
      case None => addNull()
    }
  }

  def addContents(item: File): ChecksumCalculator = {
    Option(item) match {
      case Some(file) => {
        if (file.exists()) {
          Resource.fromFile(file).bytes.sliding(1024).foreach((bytes) => update(bytes.toArray))
          this
        } else {
          addNull()
        }
      }
      case None => addNull()
    }
  }

  def add(item: Map[String, String]): ChecksumCalculator = {
    Option(item) match {
      case Some(map) => add(map.toString)
      case None => addNull()
    }
  }

  def calculate(): String = md.digest().map(0xFF & _).map { "%02x".format(_) }.mkString("")
}
