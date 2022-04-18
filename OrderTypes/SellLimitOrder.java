package OrderTypes;

import java.time.LocalDateTime;

public class SellLimitOrder extends LimitOrder{
  public SellLimitOrder(String symbol, int quantity, double cost, LocalDateTime orderSet, LocalDateTime orderFilled, double limitPrice) {
    super(symbol, quantity, cost, orderSet, orderFilled, limitPrice);
  }

  public String toString() {
    return "Sell Limit Order: " + super.toString();
  }

  //ensure that limit price is greater than the market ask
  public boolean checkLimitPrice(double marketAsk) {
    return super.getLimitPrice() >= marketAsk;
  }
}
