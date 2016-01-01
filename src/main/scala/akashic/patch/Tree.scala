package akashic.tree

case class Tree(root: Path) {
  def bucket(name): Path = root.resolve(name)
}
