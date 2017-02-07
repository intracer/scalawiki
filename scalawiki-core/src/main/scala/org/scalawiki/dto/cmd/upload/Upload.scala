package org.scalawiki.dto.cmd.upload

import org.scalawiki.dto.cmd._


case class Upload(override val params: UploadParam[Any]*)
  extends  EnumArgument[ActionArg]("upload", "upload files.")
  with ActionArg
  with ArgWithParams[UploadParam[Any], ActionArg] {

  def watch(params: WatchParam[Any]*) = ???

}

trait UploadParam[+T] extends Parameter[T]

case class Filename(override val arg: String) extends StringParameter("filename", "Target filename.") with UploadParam[String]

case class Comment(override val arg: String) extends StringParameter("comment",
  "Upload comment. Also used as the initial page text for new files if text parameter not provided") with UploadParam[String]

case class Text(override val arg: String) extends StringParameter("text", "Initial page text for new files.") with UploadParam[String]

case class Token(override val arg: String) extends StringParameter("token", "Initial page text for new files.") with UploadParam[String]

case class IgnoreWarnings(override val arg: Boolean = true) extends BooleanParameter("ignorewarnings",
  "Ignore any warnings. This must be set to upload a new version of an existing image.") with UploadParam[Boolean]

case class File(override val arg: Array[Byte]) extends ByteArrayParameter("file",
  "File contents") with UploadParam[Array[Byte]]

case class Url(override val arg: String) extends StringParameter("url",  "Url to fetch the file from") with UploadParam[String]

case class FileKey(override val arg: String) extends StringParameter("filekey",
  "/Key returned by a previous upload that failed due to warnings, or (with httpstatus) The upload_session_key of an asynchronous upload. " +
    "Key that identifies a previous upload that was stashed temporarily.") with UploadParam[String]

case class SessionKey(override val arg: String) extends StringParameter("sessionkey",
  "Same as filekey, maintained for backward compatibility") with UploadParam[String]

case class Stash(override val arg: String) extends StringParameter("stash",
  "If set, the server will not add the file to the repository and stash it temporarily") with UploadParam[String]

case class Chunk(override val arg: Array[Byte]) extends ByteArrayParameter("chunk", "Chunk contents") with UploadParam[Array[Byte]]

case class Offset(override val arg: Long) extends LongParameter("offset", "Offset of chunk in bytes") with UploadParam[Long]

case class FileSize(override val arg: Long) extends LongParameter("filesize", "Filesize of entire upload") with UploadParam[Long]

case class Async(override val arg: Boolean = true) extends BooleanParameter("async",
"Make potentially large file operations asynchronous when possible") with UploadParam[Boolean]

case class AsyncDownload(override val arg: Boolean = true) extends BooleanParameter("asyncdownload", "Filesize of entire upload") with UploadParam[Boolean]

case class LeaveMessage(override val arg: Boolean = true) extends BooleanParameter("leavemessage",
  "If asyncdownload is used, leave a message on the user talk page if finished ") with UploadParam[Boolean]

case class StatusKey(override val arg: String) extends StringParameter("statuskey",
  "Fetch the upload status for this file key (upload by URL)") with UploadParam[String]

case class Checkstatus(override val arg: Boolean = true) extends BooleanParameter("checkstatus",
  "Only fetch the upload status for the given file key") with UploadParam[Boolean]
