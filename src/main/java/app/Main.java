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
        if (args.length != 2) {
            System.out.println("Application requires two arguments");
            return;
        }

        String paymentAddress = args[1];
        String ordersAddress = args[0];

        // loading the configuration data (payment methods)
        PaymentReader payReader = new PaymentReader(paymentAddress);
        PayMeth[] paymentMethods = payReader.read();
        PaymentMethods paymentMethodsObj = new PaymentMethods(paymentMethods);

        // loading the orders data
        OrderReader ordReader = new OrderReader(ordersAddress);
        Order[] orders = ordReader.read();
        Orders ordersObj = new Orders(orders);

        // setting up the optimization algorithm
        PaymentOptimizer optimizer = new PaymentOptimizer(ordersObj, paymentMethodsObj);
        optimizer.cardsReduce();
//        optimizer.pointsReduce();
//        optimizer.printResults();
//        System.out.println();
        optimizer.pointsSplit();
        optimizer.cardsSplit();
        optimizer.printResults();
    }
}