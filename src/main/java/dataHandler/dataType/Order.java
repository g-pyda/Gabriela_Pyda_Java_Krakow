package dataHandler.dataType;

import dataHandler.dataSet.PaymentMethods;


import java.util.*;

public class Order {
    private String id;
    private double value;
    private String[] promotions;

    private HashMap<Double, ArrayList<String>> promotionsRate = new HashMap<>();
    private ArrayList<Double> promotionsRateSorted = new ArrayList<>();

    public Order() {}

    public Order(String id, double value, String[] promotions) {
        this.id = id;
        this.value = value;
        this.promotions = promotions;
    }

    public void sortPromotions(PaymentMethods paymentMethods) {
        // no promotions available on this order
        if (Objects.isNull(promotions))
            return;

        // getting the hashed payment methods, so their search is easier
        HashMap<String, PayMeth> availablePayMeth = paymentMethods.getHashedPaymentMethods();

        // checking every promotion aligned with the order
        for (int i = 0; i < this.promotions.length; i++) {
            double dis = 0.0;

            // payment is available
            if (availablePayMeth.containsKey(this.promotions[i])) {
                PayMeth method = availablePayMeth.get(this.promotions[i]);
                dis = method.getDiscount();
            }
            //payment isn't available - 0.0 discount

            // this promotion rate already exists - append to the list
            if (promotionsRate.containsKey(dis)) {
                ArrayList<String> disProm = promotionsRate.get(dis);
                disProm.add(this.promotions[i]);
                promotionsRate.put(dis, disProm);
            }
            // new promotion rate
            else {
                ArrayList<String> disProm = new ArrayList<String>();
                disProm.add(this.promotions[i]);
                promotionsRate.put(dis, disProm);
            }
        }

        // creating the ArrayList of sorted single promotion rates - eases search for the best subsequent payment methods
        promotionsRateSorted = new ArrayList<Double>(promotionsRate.keySet());
        Collections.sort(promotionsRateSorted, Collections.reverseOrder());
    }

    public String getId() {
        return id;
    }

    public double getValue() {
        return value;
    }

    public String[] getPromotions() {
        return promotions;
    }

    public HashMap<Double, ArrayList<String>> getPromotionsRate() {
        return promotionsRate;
    }

    public ArrayList<Double> getPromotionsRateSorted() {
        return promotionsRateSorted;
    }

    public String moveToLowerDiscount(PayMeth currentPayment) {
        // looking for the position of the current payment
        ArrayList<String> disProm = promotionsRate.get((double) currentPayment.getDiscount());
        // looking for the next available promotion
        int index = -1;
        for (int i = 0; i < disProm.size(); i++) {
            if (disProm.get(i).equals(currentPayment.getId())) {
                index = i;
                break;
            }
        }
        if (index < disProm.size() - 1)
            return disProm.get(index+1);

        // the last bank with the same discount rate - new position
        index = promotionsRateSorted.indexOf((double) currentPayment.getDiscount());
        if (index < promotionsRateSorted.size() - 1)
            return promotionsRate.get(promotionsRateSorted.get(index+1)).getFirst();
        // no positions further - no other discounts available
        return null;
    }
}
