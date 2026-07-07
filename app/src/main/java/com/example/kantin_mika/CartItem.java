package com.example.kantin_mika;

public class CartItem {
    private int idMenu;
    private int idTenant;
    private String namaMenu;
    private String namaTenant;
    private String namaPemilik;
    private int harga;
    private int qty;

    public CartItem(int idMenu, int idTenant, String namaMenu, String namaTenant, String namaPemilik, int harga, int qty) {
        this.idMenu = idMenu;
        this.idTenant = idTenant;
        this.namaMenu = namaMenu;
        this.namaTenant = namaTenant;
        this.namaPemilik = namaPemilik;
        this.harga = harga;
        this.qty = qty;
    }

    public int getIdMenu() { return idMenu; }
    public int getIdTenant() { return idTenant; }
    public String getNamaMenu() { return namaMenu; }
    public String getNamaTenant() { return namaTenant; }
    public String getNamaPemilik() { return namaPemilik; }
    public int getHarga() { return harga; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
}
