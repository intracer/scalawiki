package client.dto.cmd.query.prop

import client.dto.cmd._
import client.dto.cmd.query.Module

/**
 *  ?action=query&amp;prop=revisions
 *
 */
case class Revisions(override val params: RvParam*) extends Module[PropArg]("rv", "revisions", "Get revision information.") with PropArg with ArgWithParams[RvParam, PropArg]

/**
 * Marker trait for parameters used with prop=revisions
 */
trait RvParam extends Parameter[AnyRef]

/**
 *  ?action=query&amp;prop=info&amp;rvprop=
 *
 */
case class RvProp(override val args: RvPropArg*) extends EnumParameter[RvPropArg]("rvprop", "Which properties to get for each revision:") with RvParam

case class RvLimit(override val arg: String) extends StringParameter("rvlimit", "") with RvParam
/**
 *  Trait for rvprop= arguments
 *
 */

trait RvPropArg extends EnumArg[RvPropArg] { val param = RvProp }

/**
 *  rvprop= arguments
 *
 */
object Ids extends EnumArgument[RvPropArg]("ids", "The ID of the revision.") with RvPropArg
object Flags extends EnumArgument[RvPropArg]("flags", "Revision flags (minor).") with RvPropArg
object Timestamp extends EnumArgument[RvPropArg]("timestamp", "The timestamp of the revision.") with RvPropArg
object User extends EnumArgument[RvPropArg]("user", "User that made the revision.") with RvPropArg
object UserId extends EnumArgument[RvPropArg]("userid", "User ID of the revision creator.") with RvPropArg
object Size extends EnumArgument[RvPropArg]("size", "Length (bytes) of the revision.") with RvPropArg
object Sha1 extends EnumArgument[RvPropArg]("sha1", "SHA-1 (base 16) of the revision.") with RvPropArg
object ContentModel extends EnumArgument[RvPropArg]("contentmodel", "Content model ID of the revision.") with RvPropArg
object Comment extends EnumArgument[RvPropArg]("comment", "Comment by the user for the revision.") with RvPropArg
object ParsedComment extends EnumArgument[RvPropArg]("parsedcomment", "Parsed comment by the user for the revision.") with RvPropArg
object Content extends EnumArgument[RvPropArg]("content", "Text of the revision.") with RvPropArg
object Tags extends EnumArgument[RvPropArg]("tags", "Tags for the revision.") with RvPropArg
