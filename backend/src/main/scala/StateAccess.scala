package wust.backend

import wust.api._
import wust.backend.auth._
import wust.graph._
import wust.util.Pipe

import scala.concurrent.{ExecutionContext, Future}

case class RequestResponse[T](result: T, events: ApiEvent*)
object RequestResponse {
  def eventsIf(result: Boolean, events: ApiEvent*) = result match {
    case true => RequestResponse(result, events: _*)
    case false => RequestResponse(result)
  }
}

trait RequestEffect[T]
case class NoEffect[T](response: Future[RequestResponse[T]]) extends RequestEffect[T]
case class StateEffect[T](state: Future[State], response: Future[RequestResponse[T]]) extends RequestEffect[T]

class StateDsl(createImplicitAuth: () => Future[Option[JWTAuthentication]]) {
  private lazy val implicitAuth = createImplicitAuth()
  private def actualOrImplicitAuth(auth: Option[JWTAuthentication])(implicit ec: ExecutionContext): Future[Option[JWTAuthentication]] = auth match {
    case None => implicitAuth
    case auth => Future.successful(auth)
  }

  private def userOrFail(auth: Option[JWTAuthentication]): User =
    auth.map(_.user).getOrElse(throw ApiException(Unauthorized))

  def withUser[T](f: (State, User) => Future[RequestResponse[T]]): State => RequestEffect[T] = state => {
    val user = userOrFail(state.auth)
    val response = f(state, user)
    NoEffect(response)
  }

  def withUserOrImplicit[T](f: (State, User) => Future[RequestResponse[T]])(implicit ec: ExecutionContext): State => RequestEffect[T] = state => {
    val auth = actualOrImplicitAuth(state.auth)
    val newState = auth.map(auth => state.copy(auth = auth))
    val user = auth.map(userOrFail _)
    val response = newState.flatMap(newState => user.flatMap(f(newState, _)))
    StateEffect(newState, response)
  }
}

class StateAccess(initialState: Future[State], publishEvent: ChannelEvent => Unit, createImplicitAuth: () => Future[Option[JWTAuthentication]]) extends StateDsl(createImplicitAuth) {
  private var actualState = initialState
  def state = actualState

  private def returnResult[T](response: Future[RequestResponse[T]])(implicit ec: ExecutionContext): Future[T] = {
    //sideeffect: send out events!
    response.foreach(_.events.foreach(ChannelEvent(Channel.All, _) |> publishEvent))

    response.map(_.result)
  }

  implicit def resultIsRequestResponse[T](result: T)(implicit ec: ExecutionContext): RequestResponse[T] = RequestResponse(result)
  implicit def futureResultIsRequestResponse[T](result: Future[T])(implicit ec: ExecutionContext): Future[RequestResponse[T]] = result.map(RequestResponse(_))
  implicit def resultFunctionIsExecuted[T](f: State => Future[T])(implicit ec: ExecutionContext): Future[T] = state.flatMap(f)
  implicit def responseFunctionIsExecuted[T](f: State => Future[RequestResponse[T]])(implicit ec: ExecutionContext): Future[T] = returnResult(state.flatMap(f))
  implicit def effectFunctionIsExecuted[T](f: State => RequestEffect[T])(implicit ec: ExecutionContext): Future[T] = {
    val stateResponse = state.map(f).map {
      case NoEffect(response) => (state, response)
      case StateEffect(newState, response) => (newState, response)
    }

    val newState = stateResponse.flatMap(_._1)
    val response = stateResponse.flatMap(_._2)

    //sideeffect: set new state
    actualState = newState

    returnResult(response)
  }
}
