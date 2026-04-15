package org.scalawiki.spark
/*

Problem Statement:
Implement a solution that creates a binary tree, performs a breadth-first search (BFS) traversal, and returns the results.
Requirements:

Create a binary tree with least 5-7 nodes (you can hardcode the tree structure)
Implement BFS traversal to visit all nodes level by level from left to right
Store results in a List/Array data structure
Print/return the final result

Expected Output Format:
The output should be a list of node values in the order they were visited during BFS traversal.

       1
      / \
     2   3
    / \   \
   4   5   6

Expected output: [1, 2, 3, 4, 5, 6]
 */

sealed trait Tree {
  def elem: Option[Int]
}

final case class Leaf(elem: Some[Int]) extends Tree
object Leaf {
  //def apply(elem: Int): Leaf = Leaf(Some(elem))
}
final case class Node(elem: Some[Int], left: Tree, right: Tree) extends Tree
object Node {
 // def apply(elem: Int, left: Tree, right: Tree): Node = Node(Some(elem), left, right)
}
object End extends Tree {
  override def elem: Option[Int] = None
}

//object Node {
//  //def full(elem: Int, left: Tree, right: Tree) = Node(elem, Some(left), Some(right))
//}

//trait Traversal[T] {
//  def elems(x: T): List[Int]
//}
//object BfsTraversal {
//  implicit val leaf: Traversal[Leaf] = new Traversal[Leaf] {
//    override def elems(x: Leaf): List[Int] = Nil
//  }
//
//  implicit val nodeOpt: Traversal[Option[Tree]] = new Traversal[Option[Tree]] {
//    override def elems(x: Option[Tree]): List[Int] = x.map(traverse).getOrElse(Nil)
//  }
//
//  implicit val node: Traversal[Node] = new Traversal[Node] {
//    override def elems(x: Node): List[Int] = {
//      x.left.fold(List.empty[Int])(x => List(x.elem)) ++
//      x.right.fold(List.empty[Int])(x => List(x.elem)) ++
//        traverse(x.left) ++ traverse(x.right)
//    }
//  }

//    def traverse[T](x: T)(implicit traversal: Traversal[T]): List[Int] = traversal.elems(x)
//}

object Main extends App {
  implicit def intToOpt(int: Int): Some[Int] = Some(int)
  val tree = Node(
    1,
    left = Node(2, Leaf(4), Leaf(5)),
    right = Node(3, left = End, right = Leaf(6))
  )

  def bfs(tree: Tree): List[Int] = {
    tree.elem.toList ++ bfsInternal(tree)
  }

  def bfsInternal(tree: Tree): List[Int] = {
    tree match {
      case End        => Nil
      case Leaf(elem) => Nil
      case Node(_, left, right) =>
        left.elem.toList ++
          right.elem.toList ++
          bfsInternal(left) ++
          bfsInternal(right)
    }
  }

  println("Hello LeetCoder " + bfs(tree))
}
