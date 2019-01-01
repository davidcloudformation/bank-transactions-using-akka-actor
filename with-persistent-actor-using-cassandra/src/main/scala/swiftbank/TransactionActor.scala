package swiftbank

import java.util.UUID
import akka.actor.{Actor, ActorLogging, ActorRef}

object TransactionActor{
  case class Transaction(transactionUUID:UUID,
                         sender:BankId,
                         receiver:BankId,
                         money:Double)
  case class MoneyRequest(money:Double)
  case class MoneyResponse(money:Double)
  case class MoneyDeposit(money:Double)

  case class BankId(idNumber:Int){ // parsing
    if(idNumber<=0){
      throw new RuntimeException(s"ID number $idNumber is invalid")
    }

  }
}

///  account super -> account actor -> transaction actor
//// Account actor will trigger transaction
class TransactionActor(receiver:ActorRef) extends Actor with ActorLogging{
  import TransactionActor._

  override def receive: Receive = {
    case msg :Transaction =>
      sender() ! MoneyRequest(msg.money)
      log.info(s" A money request ${msg.money}")

    case msg:MoneyResponse =>
      receiver ! MoneyDeposit(msg.money)
      log.info(s"A money deposit ${msg.money}")
  }

}
