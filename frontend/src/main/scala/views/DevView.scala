package wust.frontend.views

import autowire._
import boopickle.Default._
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement
import rx._
import wust.api._
import wust.frontend.{ Client, GlobalState }
import wust.graph._
import wust.ids._
import wust.util.AutoId
import wust.util.tags._

import scala.collection.breakOut
import scala.concurrent.duration.{ span => _, _ }
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalatags.JsDom.all._
import scalatags.rx.all._

object DevView {
  import scala.util.Random.{ nextInt => rInt, nextString => rStr }
  val apiEvents = RxVar[List[ApiEvent]](Nil)

  def apply(state: GlobalState)(implicit ctx: Ctx.Owner) = {
    span(
      div(
        id := "devview",
        position.fixed, right := 0, top := 50, display.flex, flexDirection.column,
        padding := 2,
        backgroundColor := "rgba(248,240,255,0.8)", border := "1px solid #ECD7FF",
        div(position.absolute, right := 0, top := 0, "x", cursor := "pointer", onclick := { () =>
          document.getElementById("devview").asInstanceOf[HTMLElement].style.display = "none"
        }),
        Rx {
          val users = List("a", "b", "c", "d", "e", "f", "g")
          div(
            "login: ",
            users.map(u => button(u, onclick := { () =>
              Client.auth.register(u, u).call().filter(_ == false).foreach { _ =>
                Client.auth.logout().call().foreach { _ =>
                  Client.auth.login(u, u).call()
                }
              }
            }))
          ).render
        },
        Rx {
          def addRandomPost() { Client.api.addPost(rStr(1 + rInt(20)), state.graphSelection(), state.selectedGroupId()).call() }
          div(
            button("create random post", onclick := { () => addRandomPost() }),
            button("10", onclick := { () => for (_ <- 0 until 10) addRandomPost() }),
            button("100", onclick := { () => for (_ <- 0 until 100) addRandomPost() })
          ).render
        },
        Rx {
          val posts = scala.util.Random.shuffle(state.displayGraph().graph.postIds.toSeq)
          def deletePost(id: PostId) { Client.api.deletePost(id, state.graphSelection()).call() }
          div(
            button("delete random post", onclick := { () => posts.take(1) foreach deletePost }),
            button("10", onclick := { () => posts.take(10) foreach deletePost }),
            button("100", onclick := { () => posts.take(100) foreach deletePost })
          ).render
        },
        div(
          "Random Events:",
          br(),
          {
            import scalajs.js.timers._
            def graph = state.rawGraph.now

            val nextPostId = AutoId(100000)
            def randomPostId: Option[PostId] = if (graph.postsById.size > 0) Option((graph.postIds.toIndexedSeq)(rInt(graph.postsById.size))) else None
            def randomConnection: Option[Connection] = if (graph.connections.size > 0) Option((graph.connections.toIndexedSeq)(rInt(graph.connections.size))) else None
            def randomContainment: Option[Containment] = if (graph.containments.size > 0) Option((graph.containments.toIndexedSeq)(rInt(graph.containments.size))) else None
            val events: Array[() => Option[ApiEvent]] = {
              val distribution: List[(Int, () => Option[ApiEvent])] = (
                (1, () => Option(NewPost(Post(nextPostId(), rStr(1 + rInt(20)))))) ::
                (1, () => randomPostId.map(p => UpdatedPost(Post(p, rStr(1 + rInt(20)))))) ::
                (1, () => randomPostId.map(DeletePost(_))) ::
                (2, () => for (p1 <- randomPostId; p2 <- randomPostId) yield NewConnection(Connection(p1, p2))) ::
                (2, () => randomConnection.map(DeleteConnection(_))) ::
                (2, () => for (p1 <- randomPostId; p2 <- randomPostId) yield NewContainment(Containment(p1, p2))) ::
                (2, () => randomContainment.map(DeleteContainment(_))) ::
                Nil
              )
              distribution.flatMap { case (count, f) => List.fill(count)(f) }(breakOut)
            }
            def randomEvent = events(rInt(events.size))()

            def emitRandomEvent() {
              randomEvent foreach state.onApiEvent
            }
            var interval: Option[SetIntervalHandle] = None
            val intervals = (
              5.seconds ::
              2.seconds ::
              1.seconds ::
              0.5.seconds ::
              0.1.seconds ::
              Duration.Inf ::
              Nil
            )
            val prefix = "DevViewRandomEventTimer"
            for (i <- intervals) yield {
              val iid = s"$prefix$i"
              i match {
                case i: FiniteDuration =>
                  span(radio(name := prefix, id := iid), labelfor(iid)(s"${i.toMillis / 1000.0}s"), onclick := { () =>
                    interval.foreach(clearInterval)
                    interval = Option(setInterval(i)(emitRandomEvent))
                  })
                case _ =>
                  span(radio(name := prefix, id := iid, checked), labelfor(iid)(s"none"), onclick := { () =>
                    interval.foreach(clearInterval)
                    interval = None
                  })
              }
            }
          }
        ) // ,Rx {
        //   state.rawGraph().toSummaryString
        // }
        , pre(maxWidth := "400px", maxHeight := "300px", overflow.scroll, fontSize := "11px", Rx {
          apiEvents().mkString("\n")
          // pre(apiEvents().mkString("\n")).render
        }), button("clear", onclick := { () => apiEvents() = Nil })
      ),
      Rx {
        (state.jsError() match {
          case Some(error) =>
            pre(
              position.fixed, right := 0, bottom := 50,
              border := "1px solid #FFD7D7", backgroundColor := "#FFF0F0", color := "#C41A16",
              width := "90%", margin := 10, padding := 10, whiteSpace := "pre-wrap",
              error
            )
          case None => span()
        }).render
      }
    )
  }
}
