package OrderTypes;

import java.time.LocalDateTime;

public class BuyLimitOrder extends LimitOrder{
  public BuyLimitOrder(String symbol, int quantity, double cost, LocalDateTime orderSet, LocalDateTime orderFilled, double limitPrice) {
    super(symbol, quantity, cost, orderSet, orderFilled, limitPrice);
  }

  //check that the limit price is above the bid price
  public boolean checkLimitPrice(double bidPrice) {
    return super.getLimitPrice() <= bidPrice;
  }

  //toString method
  public String toString() {
    return "Buy Limit Order: " + super.toString();
  }

}
