/*
 * Copyright (C) 2011-2013 spray.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spray.can.parsing

import akka.util.ByteIterator
import spray.can.MessageLine
import spray.util._
import spray.http._
import HttpHeaders.RawHeader
import StatusCodes.RequestEntityTooLarge

class ToCloseBodyParser(settings: ParserSettings,
                        messageLine: MessageLine,
                        headers: List[RawHeader],
                        connectionHeader: Option[String],
                        contentType: Option[ContentType]) extends IntermediateState {

  private var body: Array[Byte] = EmptyByteArray

  def read(data: ByteIterator) = {
    val array = new Array[Byte](data.len)
    data.getBytes(array)
    body match {
      case EmptyByteArray ⇒ body = array; this
      case _ ⇒ {
        if (body.length + array.length <= settings.maxContentLength) {
          body = body concat array
          this
        } else ErrorState(RequestEntityTooLarge, "HTTP message body size exceeds the configured limit of " +
          settings.maxContentLength)
      }
    }
  }

  def complete = CompleteMessageState(messageLine, headers, connectionHeader, contentType, body)
}