package dataHandler.dataSet;

import dataHandler.dataType.Order;

import java.util.HashMap;

public class Orders extends DataSet<Order> {
    public Order[] getOrders() {
        return super.set;
    }

    public Orders(Order[] orders) {
        super(orders);
    }

    public void sortPromotions(PaymentMethods paymentMethods) {
        for (Order order : super.set) {
            order.sortPromotions(paymentMethods);
        }
    }

    public HashMap<String, Order> getHashedOrders() {
        HashMap<String, Order> result = new HashMap<>();
        for (Order order : super.set) {
            result.put(order.getId(), order);
        }
        return result;
    }
}