# Kantin Mika App 🍽️

Kantin Mika adalah aplikasi manajemen pemesanan makanan kantin berbasis Android yang dirancang untuk memudahkan interaksi antara pelanggan dan pemilik gerai (tenant). Aplikasi ini memungkinkan proses pemesanan yang lebih cepat, transparan, dan terupdate secara real-time.

## 🚀 Fitur Utama

### 📱 Sisi Pelanggan (Customer)
*   **Identifikasi Meja Pintar:** Menggunakan fitur Scan QR Code (CameraX) atau input manual untuk menentukan lokasi meja pelanggan.
*   **Sistem Keranjang Lokal:** Manajemen pesanan menggunakan database internal (SQLite), memungkinkan pelanggan mengelola menu sebelum checkout.
*   **Pelacakan Pesanan Real-time:** Menampilkan status pesanan (Menunggu, Diproses, Diantar, Selesai) yang diperbarui secara otomatis setiap 5 detik.
*   **UI/UX Modern:** Desain antarmuka yang bersih menggunakan Material Design 3.

### 🏪 Sisi Tenant (Pemilik Gerai)
*   **Manajemen Pesanan Masuk:** Menerima pesanan secara instan dengan sistem sinkronisasi otomatis.
*   **Kontrol Status Menu:** Mengubah status menu (Tersedia/Habis) secara instan melalui sistem toggle yang langsung berdampak pada tampilan pelanggan.
*   **Kelola Pesanan:** Fitur untuk mengubah tahapan proses makanan hingga siap disajikan ke meja pelanggan.
*   **Dashboard Statistik:** Ringkasan jumlah pesanan yang masuk berdasarkan kategori statusnya.

## 🛠️ Teknologi yang Digunakan
*   **Bahasa Pemrograman:** Java (Android Studio)
*   **Penyimpanan Lokal:** SQLite (Cart Management) & SharedPreferences (Session Management)
*   **Backend:** PHP 8.x (REST API)
*   **Database:** MySQL
*   **Komunikasi Data:** JSON melalui HttpURLConnection
*   **Library Utama:**
    *   Android CameraX (Scan QR)
    *   Google Material Design
    *   RecyclerView & ViewPager2

## 📦 Panduan Instalasi

### 1. Konfigurasi Backend (Server)
1.  Pastikan kamu sudah menginstal **XAMPP**.
2.  Buat database baru di phpMyAdmin bernama `kantin_mika`.
3.  Impor file `.sql` kamu ke database tersebut.
4.  Pindahkan folder API PHP kamu ke direktori: `C:\xampp\htdocs\pmob\api_uas\`.

### 2. Konfigurasi Android Studio
1.  Clone repository ini: https://github.com/jkev31/Kantin_Mika
2.  Buka Android Studio, pilih Open, lalu arahkan ke folder project tersebut.
3. Lakukan pencarian massal dengan menekan tombol Ctrl + Shift + F di Android Studio.
4. Cari alamat IP lama (misal: 192.168.1.5) dan ganti semua kecocokannya dengan Alamat IP Laptop kamu yang sekarang (bisa dicek melalui perintah ipconfig di Command Prompt).
5. Pastikan HP Android dan Laptop kamu terhubung ke jaringan WiFi yang sama agar bisa saling berkomunikasi.
6. Tunggu proses Gradle Sync selesai, lalu klik tombol Run (ikon Play hijau) untuk menginstal aplikasi ke HP.

### 👨‍💻 Dikembangkan oleh:
*   **Michael Christopher**
*   **Farrel Endra Asrory** 
*   **Jonathan Kevin Santoso**
