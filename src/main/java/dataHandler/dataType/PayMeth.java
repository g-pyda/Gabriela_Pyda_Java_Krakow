package dataHandler.dataType;

public class PayMeth {
    private String id;
    private int discount;
    private double limit;

    public PayMeth() {}

    public PayMeth(String id, int discount, double limit) {
        this.id = id;
        this.discount = discount;
        this.limit = limit;
    }

    public String getId() {
        return id;
    }

    public int getDiscount() {
        return discount;
    }

    public double getLimit() {
        return limit;
    }
}
