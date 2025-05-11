package app;

import algorithm.PaymentOptimizer;
import dataHandler.dataReader.OrderReader;
import dataHandler.dataReader.PaymentReader;
import dataHandler.dataSet.Orders;
import dataHandler.dataSet.PaymentMethods;
import dataHandler.dataType.PayMeth;
import dataHandler.dataType.Order;

public class Main {
    public static void main(String[] args) {

        // loading the configuration data (payment methods)
        PaymentReader payReader = new PaymentReader("C:\\Users\\gabri\\Downloads\\Zadanie2025v2\\paymentmethods.json");
        PayMeth[] paymentMethods = payReader.read();
        PaymentMethods paymentMethodsObj = new PaymentMethods(paymentMethods);

        // loading the orders data
        OrderReader ordReader = new OrderReader("C:\\Users\\gabri\\Downloads\\Zadanie2025v2\\orders.json");
        Order[] orders = ordReader.read();
        Orders ordersObj = new Orders(orders);
        ordersObj.sortPromotions(paymentMethodsObj);

        // setting up the optimization algorithm
        PaymentOptimizer optimizer = new PaymentOptimizer(ordersObj, paymentMethodsObj);
        optimizer.printResults();
        optimizer.cardsReduce();
        optimizer.printResults();
    }
}