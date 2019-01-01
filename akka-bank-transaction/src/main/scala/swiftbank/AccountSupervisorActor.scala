package swiftbank

import akka.actor.{Actor, ActorLogging, Props}

/////
import TransactionActor.BankId
import swiftbank.AccountSupervisorActor.{AccountCreateCmd, AccountRouteRequest}

object AccountSupervisorActor{
  case class AccountCreateCmd(name:String,money:Double)
  case class AccountRouteRequest(id: BankId)
}
class AccountSupervisorActor extends Actor with ActorLogging{
  override def preStart(): Unit = {
    log.info(s"Created Supervisor ACTOR")
  }

  override def receive: Receive = updateBankId(1)

  def updateBankId(count:Int):Receive = {

    case cmd: AccountCreateCmd =>
      val name = count.toString
      val ref = context.actorOf(Props(new AccountActor(BankId(count),cmd.name,cmd.money)),name) // count is unique account id
      log.info(s"Created actor  $ref with this id $count")
      context.become(updateBankId(count +1))//increment for next id bank Id
      sender() ! "Created Account"

    case msg:AccountRouteRequest =>

      context.child(msg.id.idNumber.toString) match {      // find child id

        case Some(actorRef) =>
          sender() ! actorRef
          log.info(s" sender $actorRef")

        case None => log.info(s"I found nothing ${msg.id.idNumber}")
      }

    case _ => log.info("I dont understand")

  }

}
