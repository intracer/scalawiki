package client.dto.cmd.query.prop

import client.dto.cmd._

/**
 *  ?action=query&amp;prop=revisions
 *
 */
object Revisions extends  EnumArgument[PropArg]("revisions", "Get revision information.") with PropArg with ArgWithParams[RevParam, PropArg]


/**
 * Marker trait for parameters used with prop=revisions
 */
trait RevParam extends Parameter[AnyRef]


/**
 *  ?action=query&amp;prop=info&amp;rvprop=
 *
 */
object RvProp extends EnumParameter[RvPropArg]("rvprop", "") with RevParam


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
object Contentmodel extends EnumArgument[RvPropArg]("contentmodel", "Content model ID of the revision.") with RvPropArg
object Comment extends EnumArgument[RvPropArg]("comment", "Comment by the user for the revision.") with RvPropArg
object Parsedcomment extends EnumArgument[RvPropArg]("parsedcomment", "Parsed comment by the user for the revision.") with RvPropArg
object Content extends EnumArgument[RvPropArg]("content", "Text of the revision.") with RvPropArg
object Tags extends EnumArgument[RvPropArg]("tags", "Tags for the revision.") with RvPropArg