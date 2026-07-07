package com.example.kantin_mika;

public class Menu {
    private int id;
    private int idTenant;
    private String nama;
    private int harga;
    private String statusStok;

    public Menu(int id, int idTenant, String nama, int harga, String statusStok) {
        this.id = id;
        this.idTenant = idTenant;
        this.nama = nama;
        this.harga = harga;
        this.statusStok = statusStok;
    }

    public int getId() { return id; }
    public int getIdTenant() { return idTenant; }
    public String getNama() { return nama; }
    public int getHarga() { return harga; }
    public String getStatusStok() { return statusStok; }
}
