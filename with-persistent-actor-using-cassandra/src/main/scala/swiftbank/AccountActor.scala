package swiftbank

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

////
import swiftbank.AccountSupervisorActor.AccountRouteRequest
import swiftbank.TransactionActor._

object AccountActor{
  def props(id:BankId, name:String, money:Double) = Props(new AccountActor(id,name, money))
}

class AccountActor(val id:BankId, val name:String, var money:Double) extends Actor with ActorLogging{

  override def preStart(): Unit = {
    log.info(s"$name has been created with ID ${id.idNumber} and money $money")
  }
  val cassandraActorSystem = ActorSystem("cassandraSystem", ConfigFactory.load().getConfig("cassandraDemo"))
  val persistentActor = cassandraActorSystem.actorOf(Props[SimplePersistentActor], "AccountDetails")

  override def receive:Receive  = updateAccount(money)

  def updateAccount(moneybalance:Double):Receive ={
    case msg:Transaction =>
      implicit val timeout: Timeout = Timeout(10.seconds)
      implicit val executionContext: ExecutionContext = ExecutionContext.global

      val routeRequestFuture = (context.parent ? AccountRouteRequest(msg.receiver)).mapTo[ActorRef]

      // on what thread this future??
      routeRequestFuture onComplete{
        case Success(actorref) =>
          val ref = context.actorOf(Props(new TransactionActor(actorref)))
          ref ! msg
          log.info(s" Route request $msg")
        case Failure(exception) => log.info(s"Transaction failed  $exception")

      }

    case msg :MoneyRequest =>
      if(moneybalance >= msg.money){
        val newbalance = moneybalance - msg.money
        sender() ! MoneyResponse(msg.money)
        context.become(updateAccount(newbalance))

        log.info(s" BankId is : $id and [$name], Sent  money request ${msg.money} balance is : $newbalance")
        persistentActor ! s" BankId is : $id and [$name], Sent  money request ${msg.money} balance is : $newbalance"
      }else{
        log.info(s" Insufficient fund request ${msg.money} exceeds balance,failed")
      }

    case msg:MoneyDeposit =>
      val balance = moneybalance + msg.money
      context.become(updateAccount(balance))

      log.info(s"$id and [$name] received ${msg.money} and balance is $balance")
      persistentActor ! s"$id and [$name] received ${msg.money} and balance is $balance"


    case d => log.info(s" I dont understand this message $d")
  }

}

