package wust.frontend

import org.scalatest._
import rx.Ctx.Owner.Unsafe._
import wust.graph._
import wust.ids._

class GlobalStateSpec extends FreeSpec with MustMatchers {

  //TODO: test the number of rx updates

  "raw graph" - {
    "be not consistent" in {
      val state = new GlobalState
      val graph = Graph(
        posts = List(Post("grenom", "title")),
        connections = List(Connection("grenom", "zeilinda")),
        containments = List(Containment("grenom", "telw"))
      )

      state.rawGraph() = graph

      state.rawGraph.now mustEqual graph
    }
  }

  "graph" - {
    "be complete with empty view" in {
      val state = new GlobalState
      state.rawGraph() = Graph(
        posts = List(Post("grenom", "title"), Post("zeilinda", "title2")),
        connections = List(Connection("grenom", "zeilinda")),
        containments = List(Containment("zeilinda", "grenom"))
      )

      state.rawGraph.now mustEqual state.displayGraph.now.graph
    }

    "be consistent with focused" in {
      val state = new GlobalState
      state.focusedPostId() = Option("heinz")
      state.focusedPostId.now mustEqual None

      state.rawGraph() = Graph(posts = List(Post("grenom", "title")))
      state.focusedPostId.now mustEqual Option(PostId("heinz"))

      state.rawGraph() = Graph.empty
      state.focusedPostId.now mustEqual None
    }

    "be consistent with edited" in {
      val state = new GlobalState
      state.editedPostId() = Option("heinz")
      state.editedPostId.now mustEqual None

      state.rawGraph() = Graph(posts = List(Post("grenom", "title")))
      state.editedPostId.now mustEqual Option(PostId("heinz"))

      state.rawGraph() = Graph.empty
      state.editedPostId.now mustEqual None
    }

    "be consistent with mode" in {
      val state = new GlobalState
      state.editedPostId() = Option("heinz")
      state.focusedPostId() = Option("heinz")
      state.mode.now mustEqual DefaultMode

      state.rawGraph() = Graph(posts = List(Post("grenom", "title")))
      state.mode.now mustEqual EditMode("heinz")
    }

    "have view" in {
      val state = new GlobalState
      state.rawGraph() = Graph(
        posts = List(Post("grenom", "title"), Post("zeilinda", "title2")),
        connections = Nil,
        containments = List(Containment("grenom", "zeilinda"))
      )
      state.currentView() = Perspective(collapsed = Selector.IdSet(Set("heinz")))

      state.displayGraph.now.graph mustEqual Graph(posts = List(Post("grenom", "title")))
    }

  }

  "view" - {
    "be consistent with collapsed" in {
      val state = new GlobalState
      state.collapsedPostIds() = Set("heinz")
      state.currentView.now mustEqual Perspective().union(Perspective(collapsed = Selector.IdSet(Set("heinz"))))

      state.collapsedPostIds() = Set.empty
      state.currentView.now mustEqual Perspective().union(Perspective(collapsed = Selector.IdSet(Set.empty)))
    }
  }
}
