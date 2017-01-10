package backend

import api._, graph._, framework._

object UnauthorizedException extends UserViewableException("unauthorized")
object WrongCredentials extends UserViewableException("wrong credentials")

object Model {
  val users = User("hans") ::
    User("admin") ::
    Nil

  // TODO: the next id will come from the database
  private var currentId: AtomId = 0
  def nextId() = {
    val r = currentId
    currentId += 1
    r
  }
  val post1 = Post(nextId(), "Hallo")
  val post2 = Post(nextId(), "Ballo")
  val responds1 = RespondsTo(nextId(), post2.id, post1.id)
  val post3 = Post(nextId(), "Penos")
  val responds2 = RespondsTo(nextId(), post3.id, responds1.id)
  val post4 = Post(nextId(), "Wost")
  val responds3 = RespondsTo(nextId(), post4.id, responds2.id)
  val container = Post(nextId(), "Container")
  val contains1 = Contains(nextId(), container.id, post1.id)
  val contains2 = Contains(nextId(), container.id, post4.id)
  var graph = Graph(
    Map(post1.id -> post1, post2.id -> post2, post3.id -> post3, post4.id -> post4, container.id -> container),
    Map(responds1.id -> responds1, responds2.id -> responds2, responds3.id -> responds3),
    Map(contains1.id -> contains1, contains2.id -> contains2)
  )
}

class ApiImpl(userOpt: Option[User], emit: ApiEvent => Unit) extends Api {
  import Model._

  def withUser[T](f: User => T): T = userOpt.map(f).getOrElse {
    throw UnauthorizedException
  }

  def withUser[T](f: => T): T = withUser(_ => f)

  def getPost(id: AtomId): Post = graph.posts(id)
  def deletePost(id: AtomId): Unit = {
    graph = graph.remove(id)
    emit(DeletePost(id))
  }

  def getGraph(): Graph = graph
  def addPost(msg: String): Post = withUser {
    //uns fehlt die id im client
    val post = new Post(nextId(), msg)
    graph = graph.copy(
      posts = graph.posts + (post.id -> post)
    )
    emit(NewPost(post))
    post
  }
  def connect(fromId: AtomId, toId: AtomId): RespondsTo = withUser {
    val existing = graph.respondsTos.values.find(r => r.in == fromId && r.out == toId)
    val edge = existing.getOrElse(RespondsTo(nextId(), fromId, toId))
    graph = graph.copy(
      respondsTos = graph.respondsTos + (edge.id -> edge)
    )
    emit(NewRespondsTo(edge))
    edge
  }
  // def getComponent(id: Id): Graph = {
  //   graph.inducedSubGraphData(graph.depthFirstSearch(id, graph.neighbours).toSet)
  // }
  def respond(to: AtomId, msg: String): (Post, RespondsTo) = withUser {
    val post = new Post(nextId(), msg)
    val edge = RespondsTo(nextId(), post.id, to)
    graph = graph.copy(
      posts = graph.posts + (post.id -> post),
      respondsTos = graph.respondsTos + (edge.id -> edge)
    )
    emit(NewPost(post))
    emit(NewRespondsTo(edge))
    (post, edge)
  }
}