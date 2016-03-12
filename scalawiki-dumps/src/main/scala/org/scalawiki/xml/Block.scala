package org.scalawiki.xml

case class Block(offset: Long, size: Long) {
  def end = offset + size - 1

  def contains(offsetParam: Long): Boolean =
    offsetParam >= offset && offsetParam <= end

  def contains(offsetParam: Long, sizeParam: Long): Boolean =
    contains(offsetParam) && contains(offsetParam + sizeParam - 1)

  def contains(block: Block): Boolean =
    contains(block.offset, block.size)

//  def intersection(offsetParam: Long, sizeParam: Long): Block =
//    contains(offsetParam) && contains(offsetParam + sizeParam)


}
