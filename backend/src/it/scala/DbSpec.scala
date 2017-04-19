package wust.backend

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import org.scalatest._

class DbSpec extends AsyncFreeSpec with MustMatchers {
  val publicGroup = 1 //TODO: load from config
  def await[T](future: Future[T]) = Await.result(future, 10.seconds)
  "post" - {
    "add" in Db.post("t", publicGroup).map { post =>
      post.title mustEqual "t"
    }

    "get" in Db.post("t", publicGroup).flatMap { post =>
      Db.post.get(post.id).map { dbPost =>
        dbPost.isDefined mustEqual true
        dbPost.get.id mustEqual post.id
        dbPost.get.title mustEqual post.title
      }
    }

    "update" in Db.post("t", publicGroup).flatMap { post =>
      Db.post.update(post.copy(title = "harals")).flatMap { success =>
        Db.post.get(post.id).map { dbPost =>
          dbPost.isDefined mustEqual true
          dbPost.get.id mustEqual post.id
          dbPost.get.title mustEqual "harals"
        }
      }
    }

    "delete" in Db.post("t", publicGroup).flatMap { post =>
      Db.post.delete(post.id).flatMap { success =>
        Db.post.get(post.id).map { dbPost =>
          dbPost.isDefined mustEqual false
        }
      }
    }
  }

  //TODO: check if actually in db
  "connects" - {
    val post = await(Db.post("t", publicGroup))

    "newPost" in Db.connects.newPost("nu", post.id, publicGroup).map {
      case (newPost, connects) =>
        newPost.title mustEqual "nu"
        connects.sourceId mustEqual newPost.id
        connects.targetId mustEqual post.id
    }

    "add" in Db.post("nu", publicGroup).flatMap { newPost =>
      Db.connects(newPost.id, post.id).map { connects =>
        connects.sourceId mustEqual newPost.id
        connects.targetId mustEqual post.id
      }
    }

    "add hyper" in Db.connects.newPost("nu", post.id, publicGroup).flatMap {
      case (newPost, connects) =>
        Db.connects(newPost.id, connects.id).map { connects2 =>
          connects2.sourceId mustEqual newPost.id
          connects2.targetId mustEqual connects.id
        }
    }

    "delete" in Db.connects.newPost("nu", post.id, publicGroup).flatMap {
      case (_, connects) =>
        Db.connects.delete(connects.id).map { success =>
          success mustEqual true
        }
    }
  }

  //TODO: check if actually in db
  "contains" - {
    val post = await(Db.post("t", publicGroup))

    "add" in Db.post("nu", publicGroup).flatMap { newPost =>
      Db.contains(newPost.id, post.id).map { contains =>
        contains.parentId mustEqual newPost.id
        contains.childId mustEqual post.id
      }
    }

    "delete" in Db.post("nu", publicGroup).flatMap { newPost =>
      Db.contains(newPost.id, post.id).flatMap { contains =>
        Db.contains.delete(contains.id).map { success =>
          success mustEqual true
        }
      }
    }
  }

  "user" - {
    "hasAccessToPost" - {
      import Db.user.hasAccessToPost
      "post in pubic group" in {
        val Some(user) = await(Db.user("u", "123456"))
        val post = await(Db.post("p", publicGroup))
        hasAccessToPost(user.id, post.id).map(_ must be(true))
      }

      "post in private group (user not member)" in {
        val Some(user) = await(Db.user("u2", "123456"))
        val groupId = await(Db.user.createUserGroup())
        val post = await(Db.post.createOwnedPost("p", groupId))
        hasAccessToPost(user.id, post.id).map(_ must be(false))
      }

      "post in private group (user is member)" in {
        val Some(user) = await(Db.user("u3", "123456"))
        val group = await(Db.user.createUsergroupForUser(user.id))
        val post = await(Db.post.createOwnedPost("p", group.id))
        hasAccessToPost(user.id, post.id).map(_ must be(true))
      }
    }
  }

  //TODO: user
}