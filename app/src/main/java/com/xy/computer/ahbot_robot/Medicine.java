package com.xy.computer.ahbot_robot;

public class Medicine {
    private String id;
    private String medName;
    private String medAmount;
    private String medFrequency;
    private String remarks;

    public Medicine(String id, String medName, String medAmount, String medFrequency, String remarks){
        this.id = id;
        this.medName = medName;
        this.medAmount = medAmount;
        this.medFrequency = medFrequency;
        this.remarks = remarks;
    }
    public Medicine(){

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMedName() {
        return medName;
    }

    public void setMedName(String medName) {
        this.medName = medName;
    }

    public String getMedAmount() {
        return medAmount;
    }

    public void setMedAmount(String medAmount) {
        this.medAmount = medAmount;
    }

    public String getMedFrequency() {
        return medFrequency;
    }

    public void setMedFrequency(String medFrequency) {
        this.medFrequency = medFrequency;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

/*public static Medicine getMedicineData(String s){
        String[] array = s.split("\n");
        Medicine medicine = new Medicine(array[0], Integer.parseInt(array[1]), Integer.parseInt(array[2]), array[3]);
        return medicine;
    }*/
}
