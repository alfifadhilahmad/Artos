# UI Code Explanation — Artos

Dokumen ini menjelaskan kode UI, UI-logic, data lokal, dan alur fitur pada aplikasi Android Java/XML `Artos`.

Aplikasi ini memakai:

- Java untuk logic halaman.
- XML untuk tampilan.
- SQLite lokal melalui `DatabaseHelper`.
- `SharedPreferences` untuk data kecil seperti nama user dan budget.
- Tidak ada backend/API eksternal.

Dokumen ini dibuat untuk pembaca pemula dan developer yang ingin memahami codebase hasil iterasi AI/vibe coding. Fokusnya adalah bagaimana UI bekerja, bagaimana data ditempel ke tampilan, bagaimana Activity saling terhubung, dan bagian mana yang harus hati-hati jika ingin mengubah UI atau logic lagi.

Status final penting:

- App label final: `Artos`.
- Package/applicationId tetap: `com.java.kalkulatorkeuangan`.
- Database lokal tetap: `keuangan.db`, version `2`.
- Tabel transaksi tetap: `transactions`.
- App tetap lokal, tanpa backend/API.
- Budget yang diimplementasikan adalah budget bulanan global, bukan budget per kategori.
- Custom bottom nav final dipakai pada Home, Pengeluaran, Budget, dan Riwayat.
- Transaction CRUD final sudah tersedia melalui Riwayat -> Update/Delete Transaksi.

---

## Status Final Aplikasi Artos

Bagian ini mengawali dokumen dengan status final aplikasi setelah redesign besar, Budget redesign, transaction CRUD, login polish, bottom nav stabilization, launcher branding, dan final QA fixes.

Jika ada penjelasan lama yang terasa historis, source code final dan status final di dokumen ini adalah referensi utama.

### Identitas Final Aplikasi

Nama aplikasi yang tampil di launcher dan recent apps adalah:

```text
Artos
```

Nama ini berasal dari:

```xml
<string name="app_name">Artos</string>
```

Manifest tetap memakai resource:

```xml
android:label="@string/app_name"
```

Yang tidak berubah:

- `namespace`
- `applicationId`
- Java package folder
- database name
- schema
- SharedPreferences keys

Package/applicationId tetap:

```text
com.java.kalkulatorkeuangan
```

Ini penting: mengganti nama aplikasi untuk user tidak berarti mengganti package identity Android. Kalau package/applicationId diganti sembarangan, app bisa dianggap app berbeda oleh Android, data lokal lama bisa tidak terbaca, dan instalasi/update bisa bermasalah.

### Launcher Icon Final

Launcher icon final memakai:

- background hijau `#306D29`
- logo Artos putih
- foreground adaptive transparan
- legacy mipmap icons untuk density standar

Adaptive icon memakai struktur:

```xml
<adaptive-icon>
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

Background adaptive:

```xml
<solid android:color="#306D29" />
```

Foreground adaptive berasal dari:

```text
app/src/main/res/drawable-nodpi/artos_logo_foreground.png
```

Aset ini adalah logo putih saja dengan background transparan.

Ada juga:

```text
app/src/main/res/drawable-nodpi/artos_logo_preview.png
```

File preview berisi tampilan penuh background hijau + logo putih. File ini berguna sebagai referensi visual, tetapi foreground adaptive sebaiknya tetap memakai asset transparan agar Android adaptive icon mask bekerja dengan benar.

Launcher icon scale final sudah dikecilkan beberapa kali. Target terakhir: logo menempati sekitar 42% canvas agar tidak terlalu zoomed-in dan punya safe padding yang nyaman.

Kenapa perlu padding?

- Android launcher berbeda-beda bentuk mask-nya.
- Ada launcher yang memakai rounded square.
- Ada launcher yang memakai circle.
- Adaptive icon bisa dipotong oleh mask launcher.
- Jika foreground terlalu besar, logo terasa mepet atau cropped.

Karena itu foreground harus punya transparent safe area, bukan memenuhi canvas penuh.

---

## Arsitektur Final Project

Project ini adalah Android Java/XML app dengan pola Activity-based. Tidak ada backend/API. Data utama hidup di SQLite lokal.

Folder penting:

### `app/src/main/java/com/java/kalkulatorkeuangan`

Berisi file Java:

- `MainActivity.java`
- `HomeActivity.java`
- `TambahTransaksiActivity.java`
- `RiwayatActivity.java`
- `TransactionAdapter.java`
- `UpdateTransaksiActivity.java`
- `PengeluaranActivity.java`
- `ExpenseBarChartView.java`
- `BudgetActivity.java`
- `DatabaseHelper.java`
- `Transaction.java`

### `app/src/main/res/layout`

Berisi XML layout halaman dan item:

- `activity_main.xml`
- `activity_home.xml`
- `activity_tambah_transaksi.xml`
- `activity_riwayat.xml`
- `activity_update_transaksi.xml`
- `activity_pengeluaran.xml`
- `activity_budget.xml`
- `item_transaction.xml`
- `item_transaction_date_header.xml`
- `item_analisis.xml`

### `app/src/main/res/drawable`

Berisi shape XML, background, progress drawable, launcher adaptive background, dan helper drawable lain.

Contoh:

- background card
- rounded input
- progress bar
- bottom nav background
- center FAB background
- launcher background

### `app/src/main/res/drawable-nodpi`

Berisi PNG asset yang tidak ingin diskalakan otomatis berdasarkan density Android.

Contoh:

- icon kategori
- icon bottom nav
- icon status budget
- logo Artos source

### `app/src/main/res/mipmap-*`

Berisi launcher icon untuk berbagai density.

### `app/src/main/res/font`

Berisi font seperti Plus Jakarta Sans dan Roboto.

### `app/src/main/res/values`

Berisi token seperti:

- `strings.xml`
- `colors.xml`
- theme/style resources

---

## 1. Gambaran Umum Struktur UI App

Di Android Java/XML, biasanya satu halaman terdiri dari dua bagian besar:

- File XML layout: menggambar tampilan.
- File Java Activity: mengatur perilaku halaman.

Contoh:

```java
setContentView(R.layout.activity_home);
```

Artinya `HomeActivity.java` memakai layout XML bernama `activity_home.xml`.

### XML = tampilan

XML berisi komponen UI seperti:

- `TextView` untuk teks.
- `EditText` untuk input teks.
- `Button` atau `MaterialButton` untuk tombol.
- `ImageView` untuk ikon/gambar.
- `RecyclerView` untuk daftar panjang.
- `LinearLayout`, `ConstraintLayout`, `CoordinatorLayout`, `FrameLayout`, `ScrollView`, dan `NestedScrollView` untuk mengatur posisi komponen.

Contoh XML:

```xml
<TextView
    android:id="@+id/tvSaldo"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

Bagian penting:

- `android:id="@+id/tvSaldo"` memberi nama ID agar Java bisa menemukan view ini.
- `android:layout_width` dan `android:layout_height` menentukan ukuran.
- `android:textColor`, `android:textSize`, `android:fontFamily`, dan `android:background` menentukan gaya visual.

### Java Activity = logic halaman

Activity adalah class Java yang mewakili satu layar.

Contoh:

```java
public class HomeActivity extends AppCompatActivity
```

Artinya:

- `public class` membuat class bisa dipakai oleh Android.
- `HomeActivity` adalah nama halaman.
- `extends AppCompatActivity` berarti class ini adalah Activity Android yang bisa menampilkan layar.

Method yang hampir selalu ada:

```java
@Override
protected void onCreate(Bundle savedInstanceState)
```

`onCreate()` dipanggil ketika halaman pertama kali dibuka.

Di dalam `onCreate()`, biasanya ada:

```java
setContentView(R.layout.nama_layout);
```

dan:

```java
findViewById(R.id.namaId);
```

`findViewById()` mencari komponen XML berdasarkan ID.

### Adapter = penghubung data ke item RecyclerView

`RecyclerView` tidak langsung tahu cara menampilkan data. Ia butuh adapter.

Di project ini, `TransactionAdapter.java` mengubah data `Transaction` menjadi:

- header tanggal
- card transaksi
- icon kategori
- judul catatan
- subtitle kategori
- nominal `+ Rp ...` atau `- Rp ...`

### Drawable = bentuk background/icon

Folder `res/drawable` berisi XML untuk bentuk visual, misalnya:

- `bg_bottom_nav.xml` untuk background navbar bawah.
- `bg_center_input_button.xml` untuk tombol plus tengah.
- `riwayat_search_bg.xml` untuk search bar.
- `riwayat_chip_active_bg.xml` dan `riwayat_chip_inactive_bg.xml` untuk chip filter.
- `home_balance_card_bg.xml` untuk kartu saldo hijau.
- `input_note_bg.xml` untuk input catatan.

Folder `res/drawable-nodpi` berisi PNG icon seperti:

- icon kategori aktif/nonaktif
- icon bottom nav aktif/nonaktif
- icon input plus
- icon income/expense
- icon kalender
- icon catatan

### Model = bentuk data transaksi

`Transaction.java` adalah class data sederhana. Ia menyimpan:

- `id`
- `type`
- `amount`
- `category`
- `note`
- `date`

Class ini tidak menggambar UI sendiri, tetapi datanya dipakai oleh UI.

### DatabaseHelper = sumber data lokal

`DatabaseHelper.java` mengatur SQLite lokal. UI memanggil method seperti:

- `insertTransaction(...)`
- `getAllTransactions()`
- `getTotalPemasukan()`
- `getTotalPengeluaran()`
- `getExpenseAnalysis()`

Penting: file ini sensitif karena berhubungan dengan schema database dan data transaksi.

---

## 2. Alur Navigasi Aplikasi

Alur umum user:

1. User membuka app.
2. App menampilkan Login (`MainActivity`).
3. User mengisi username/password dan menekan `Masuk`.
4. Username disimpan ke `SharedPreferences`.
5. App membuka Home (`HomeActivity`).
6. Dari Home, user bisa:
   - melihat saldo
   - melihat saldo bulan ini
   - melihat pemasukan/pengeluaran bulan ini
   - melihat transaksi terakhir
   - melihat budget
   - klik tombol plus untuk menambah transaksi
   - klik bottom nav ke halaman lain

### Login / MainActivity

`MainActivity.java` memakai `activity_main.xml`.

Yang terjadi:

- User mengetik username di `etUsername`.
- User klik `btnLogin` / tombol `Masuk`.
- Jika username kosong, muncul `Toast`.
- Jika tidak kosong, username disimpan ke `SharedPreferences` dengan nama file `SesiLogin` dan key `USERNAME`.
- App membuka `HomeActivity`.
- `MainActivity` ditutup dengan `finish()`.

### Home

`HomeActivity.java` memakai `activity_home.xml`.

Home menampilkan:

- nama user
- total saldo semua waktu
- saldo bulan ini
- pemasukan bulan ini
- pengeluaran bulan ini
- transaksi terakhir
- budget card
- custom bottom nav
- center plus button

### Tambah Transaksi

`TambahTransaksiActivity.java` memakai `activity_tambah_transaksi.xml`.

User mengisi:

- tipe transaksi (`Pengeluaran` / `Pemasukan`)
- nominal
- tanggal
- kategori
- catatan

Saat tombol `Tambahkan` ditekan, data disimpan melalui:

```java
dbHelper.insertTransaction(tipe, nominal, kategori, catatan, tanggal)
```

### Riwayat

`RiwayatActivity.java` memakai `activity_riwayat.xml`.

Halaman ini menampilkan:

- search bar
- filter chip kategori
- daftar transaksi dalam `RecyclerView`
- date header
- card transaksi
- custom bottom nav

### Pengeluaran

`PengeluaranActivity.java` memakai `activity_pengeluaran.xml`.

Halaman ini menampilkan analisis pengeluaran berdasarkan kategori.

### Budget

`BudgetActivity.java` memakai `activity_budget.xml`.

Halaman ini menampilkan:

- sisa budget
- progress budget
- percentage badge
- status card dinamis
- edit budget card tersembunyi
- tombol edit/simpan/batal budget

### Bottom navigation flow

Bottom navigation final memakai custom bottom nav XML pada semua halaman utama:

- Home memakai custom bottom nav.
- Pengeluaran memakai custom bottom nav.
- Budget memakai custom bottom nav.
- Riwayat memakai custom bottom nav.

Model lama `BottomNavigationView`/`BottomAppBar` sudah tidak menjadi nav utama pada halaman final.

Custom bottom nav memiliki ID:

- `customBottomNav`
- `navHomeButton`
- `navPengeluaranButton`
- `navBudgetButton`
- `navRiwayatButton`
- `fabAdd`

Center plus button membuka `TambahTransaksiActivity`.

---

## 3. Penjelasan File Java UI / UI Logic

### MainActivity.java

#### Status Final MainActivity.java

`MainActivity.java` adalah entry point launcher. Activity ini menampilkan halaman Login.

Layout:

```text
activity_main.xml
```

UI final login memakai teks Indonesia:

- `Halo!`
- `Selamat datang kembali, pantau keuangan Kamu`
- `Masukkan Nama Pengguna`
- `Kata Sandi`
- `Masuk`

Teks/fitur `Recovery Password` sudah dihapus.

##### Data session

Login menyimpan username ke:

```java
getSharedPreferences("SesiLogin", MODE_PRIVATE)
```

Key:

```text
USERNAME
```

`HomeActivity` membaca key yang sama untuk membuat greeting.

##### Username dan password

UI punya username dan password field. Logic login harus tetap mempertahankan perilaku validasi yang sudah ada di source final. Jangan mengganti credential/validasi tanpa audit, karena task polish Login sebelumnya sengaja tidak mengubah login/session behavior.

##### Keyboard transition fix

Final QA menemukan bug: ketika user login saat keyboard masih terbuka, Home terbuka tetapi custom bottom nav sempat terlihat di tengah layar.

Root cause yang paling masuk akal:

- Login memakai `ScrollView` dan `adjustResize`.
- Keyboard masih aktif ketika Home mulai dilayout.
- Home bottom nav pertama kali diukur saat window masih dalam mode resize.

Fix final:

1. Clear focus dari input username/password.
2. Hide keyboard via `InputMethodManager`.
3. Buka Home setelah delay kecil sekitar `120ms`.

Kenapa delay kecil diperlukan?

- Request hide keyboard tidak selalu selesai sinkron pada frame yang sama.
- Delay memberi waktu IME/window untuk kembali normal.
- Home kemudian dilayout dalam tinggi layar normal.

Hal yang jangan diubah sembarangan:

- `SesiLogin`
- `USERNAME`
- logic login yang sudah bekerja
- delay keyboard transition kecuali sudah diuji ulang di device/emulator
- `windowSoftInputMode` Login, karena login butuh keyboard tetap usable

---


#### Fungsi utama file

`MainActivity.java` adalah halaman login. File ini bertanggung jawab untuk:

- menampilkan layout login
- membaca username dari input
- menyimpan username ke `SharedPreferences`
- membuka `HomeActivity`

#### Struktur class

```java
public class MainActivity extends AppCompatActivity
```

- `public class`: membuat class bisa diakses oleh Android.
- `MainActivity`: nama class Activity.
- `extends AppCompatActivity`: artinya class ini adalah layar Android yang kompatibel dengan fitur AppCompat.

```java
@Override
protected void onCreate(Bundle savedInstanceState)
```

- `@Override` berarti method ini menggantikan method dari parent class.
- `protected` berarti method bisa dipakai oleh class ini dan turunan/keluarga tertentu.
- `void` berarti method tidak mengembalikan nilai.
- `Bundle savedInstanceState` berisi state lama jika Activity dibuat ulang oleh sistem.

#### Variabel penting

- `Button btnLogin`
  - Diambil dari `R.id.btnLogin`.
  - Dipakai untuk mendeteksi klik tombol login.

- `EditText etUsername`
  - Diambil dari `R.id.etUsername`.
  - Menyimpan teks username yang diketik user.

- `SharedPreferences prefs`
  - Dibuat dengan `getSharedPreferences("SesiLogin", MODE_PRIVATE)`.
  - Menyimpan username lokal di HP.

#### Method penting dan cara kerjanya

##### `onCreate()`

- Dipanggil otomatis saat halaman Login dibuka.
- Memasang layout:

```java
setContentView(R.layout.activity_main);
```

- Mengambil `btnLogin` dan `etUsername` dari XML.
- Memasang click listener pada tombol login.

##### `btnLogin.setOnClickListener(...)`

- Dipanggil saat user menekan tombol Masuk.
- Membaca username:

```java
String inputNama = etUsername.getText().toString().trim();
```

- Jika kosong, tampilkan `Toast`.
- Jika tidak kosong:
  - simpan username ke `SharedPreferences`
  - buka `HomeActivity`
  - tutup Login dengan `finish()`

#### Alur kerja halaman

1. Login muncul.
2. User mengetik username.
3. User klik Masuk.
4. App validasi username.
5. App simpan username.
6. App pindah ke Home.

#### Hubungan dengan XML

Layout:

- `activity_main.xml`

ID penting:

- `etUsername`
- `etPassword`
- `btnLogin`

Catatan: `etPassword` ada di XML, tetapi logic login saat ini hanya membaca `etUsername`.

#### Hubungan dengan Database / SharedPreferences

Tidak memakai SQLite.

Memakai `SharedPreferences`:

- nama file: `SesiLogin`
- key: `USERNAME`

Nilai ini dibaca lagi oleh `HomeActivity`.

#### Hal yang jangan sembarang diubah

- Jangan ganti ID `etUsername` dan `btnLogin` tanpa mengubah Java.
- Jangan ubah key `USERNAME` jika Home masih membacanya.
- Jangan ubah nama `SesiLogin` tanpa memperbarui pembacaan di Home.

---

### HomeActivity.java

#### Status Final HomeActivity.java

`HomeActivity.java` adalah dashboard utama.

Layout:

```text
activity_home.xml
```

Home menampilkan:

- greeting user
- total saldo all-time
- saldo bulan ini
- pemasukan bulan ini
- pengeluaran bulan ini
- transaksi terakhir
- budget summary card
- custom bottom nav
- center FAB

##### Sumber data

Home memakai:

- SQLite via `DatabaseHelper`
- SharedPreferences `SesiLogin`
- SharedPreferences `BudgetPrefs`

##### Greeting

Nama user dibaca dari:

```text
SesiLogin / USERNAME
```

Lalu ditampilkan sebagai greeting.

##### Total saldo all-time

Total saldo semua waktu:

```text
getTotalPemasukan() - getTotalPengeluaran()
```

Ini memakai query total all-time dari SQLite.

##### Saldo bulan ini

Saldo bulan ini dihitung dari semua transaksi dengan filter bulan/tahun sekarang.

Rumus:

```text
monthlyPemasukan - monthlyPengeluaran
```

Kenapa tidak memakai total all-time?

Karena kartu ini menjawab kondisi bulan berjalan, bukan seluruh riwayat.

##### Recent transactions

Home menampilkan transaksi terbaru.

Sorting memakai:

- tanggal transaksi
- id transaksi sebagai tie-breaker

Tanggal invalid tidak boleh membuat app crash. Transaksi invalid tetap ditangani aman.

##### Budget card

Budget summary membaca budget dari:

```text
BudgetPrefs / budget
```

Lalu membandingkan dengan pengeluaran bulan ini.

##### Refresh di onResume()

Home memanggil refresh dashboard di `onResume()`.

Ini penting karena data Home bisa berubah setelah:

- user menambah transaksi
- user update transaksi
- user delete transaksi
- user edit budget dari Budget page

Tanpa `onResume()`, Home bisa menampilkan data stale sampai Activity dibuat ulang.

##### Bottom nav

Home memasang click listener untuk:

- Home: no-op
- Pengeluaran: buka `PengeluaranActivity`
- Budget: buka `BudgetActivity`
- Riwayat: buka `RiwayatActivity`
- center FAB: buka `TambahTransaksiActivity`

Navigasi bottom nav memakai transisi tanpa animasi besar agar terasa seperti tab.

Hal yang jangan diubah sembarangan:

- format tanggal parser
- type string `Pengeluaran` / `Pemasukan`
- ID dashboard
- refresh `onResume()`
- bottom nav click behavior

---


#### Fungsi utama file

`HomeActivity.java` adalah halaman dashboard utama. File ini bertanggung jawab untuk:

- menampilkan nama user
- menghitung total saldo semua waktu
- menghitung saldo bulan ini
- menampilkan pemasukan/pengeluaran bulan ini
- menampilkan transaksi terakhir
- menampilkan budget card
- mengatur bottom navigation custom
- membuka halaman tambah transaksi

#### Struktur class

```java
public class HomeActivity extends AppCompatActivity
```

Class ini adalah Activity. Karena `extends AppCompatActivity`, Android bisa menampilkan layout dan menjalankan lifecycle seperti `onCreate()` dan `onResume()`.

#### Variabel penting

Sebagian besar variabel di Home dibuat di dalam method, bukan sebagai field class.

- `TextView tvNamaUser`
  - Menampilkan `👋 Hai, User!`.
  - Data berasal dari `SharedPreferences`.

- `TextView tvSaldo`
  - Menampilkan total saldo semua waktu.
  - Data berasal dari total pemasukan dikurangi total pengeluaran.

- `TextView tvPemasukan`
  - Menampilkan pemasukan bulan berjalan.

- `TextView tvPengeluaran`
  - Menampilkan pengeluaran bulan berjalan.

- `TextView tvSaldoBulanIni`
  - Menampilkan saldo bulan ini dengan format `+ Rp ...` atau `- Rp ...`.

- `TextView tvBulanSaldoBulanIni`
  - Menampilkan bulan dan tahun sekarang, misalnya `Juni 2026`.

- `TextView tvStatusSaldoBulanIni`
  - Menampilkan `Positif` atau `Negatif`.

- `ProgressBar pbBudget`
  - Progress bar budget.

- `RecentTransactionItem`
  - Inner class untuk membantu sorting transaksi terakhir berdasarkan tanggal.

#### Method penting dan cara kerjanya

##### `onCreate()`

- Dipanggil saat Home pertama dibuka.
- Memasang layout:

```java
setContentView(R.layout.activity_home);
```

- Memanggil:

```java
updateDashboardData();
setupCustomBottomNavigation();
```

- Memasang click listener pada `fabAdd` agar membuka `TambahTransaksiActivity`.

##### `onResume()`

- Dipanggil saat user kembali ke Home dari halaman lain.
- Memanggil `updateDashboardData()` lagi.
- Tujuannya agar saldo dan transaksi terbaru langsung refresh setelah user menambah transaksi.

##### `setupCustomBottomNavigation()`

- Dipanggil dari `onCreate()`.
- Memasang klik pada:
  - `navHomeButton`: diam karena sudah di Home.
  - `navPengeluaranButton`: buka `PengeluaranActivity`.
  - `navBudgetButton`: buka `BudgetActivity`.
  - `navRiwayatButton`: buka `RiwayatActivity`.
  - `btnRecentToRiwayat`: buka `RiwayatActivity`.

Risiko jika diubah sembarangan:

- Navigasi bawah bisa rusak.
- Tombol arrow transaksi terakhir bisa tidak membuka Riwayat.

##### `openBottomNavActivity(Class<?> destinationActivity)`

- Membuka Activity tujuan.
- Menghilangkan animasi transisi dengan `overridePendingTransition(0, 0)`.
- Menutup Activity sekarang dengan `finish()`.

##### `updateDashboardData()`

Ini method utama Home.

Dipanggil dari:

- `onCreate()`
- `onResume()`

Langkah kerja:

1. Ambil nama user dari `SharedPreferences`.
2. Tampilkan nama di `tvNamaUser`.
3. Ambil total pemasukan dari database.
4. Ambil total pengeluaran dari database.
5. Hitung saldo semua waktu.
6. Hitung pemasukan/pengeluaran bulan ini dari semua transaksi.
7. Tampilkan semua angka ke TextView.
8. Update badge positif/negatif.
9. Update transaksi terakhir.
10. Update budget card.

UI yang berubah:

- `tvNamaUser`
- `tvSaldo`
- `tvPemasukan`
- `tvPengeluaran`
- `tvBulanSaldoBulanIni`
- `tvStatusSaldoBulanIni`
- `tvSaldoBulanIni`
- transaksi terakhir
- budget card

##### `formatRupiah(double amount)`

- Mengubah angka menjadi teks Rupiah.
- Contoh:
  - `2000000` menjadi `Rp 2.000.000`
  - `-741000` menjadi `- Rp 741.000`

##### `formatSignedRupiah(double amount)`

- Mirip `formatRupiah()`, tetapi selalu memberi tanda.
- Contoh:
  - `7500000` menjadi `+ Rp 7.500.000`
  - `-741000` menjadi `- Rp 741.000`

##### `formatIndonesianMonthYear(Calendar date)`

- Mengubah tanggal ke nama bulan Indonesia.
- Contoh: `Juni 2026`.

##### `bindMonthlyStatus(TextView statusView, double monthlySaldo)`

- Jika saldo bulan ini negatif:
  - teks `Negatif`
  - warna merah
  - background `home_status_negative_bg`
- Jika saldo bulan ini nol/positif:
  - teks `Positif`
  - warna hijau
  - background `home_status_positive_bg`

##### `calculateCurrentMonthTotals(...)`

- Membaca semua transaksi dari `DatabaseHelper.getAllTransactions()`.
- Mengecek tanggal transaksi.
- Jika tanggal ada di bulan/tahun sekarang:
  - `Pemasukan` dijumlahkan ke monthly pemasukan.
  - `Pengeluaran` dijumlahkan ke monthly pengeluaran.
- Cursor ditutup di blok `finally`.

Risiko:

- Format tanggal harus tetap `d/M/yyyy`.
- Type harus tetap `Pemasukan` atau `Pengeluaran`.

##### `isCurrentMonthDate(...)`

- Mengecek apakah teks tanggal termasuk bulan dan tahun sekarang.
- Format yang diharapkan: `d/M/yyyy`, misalnya `3/6/2026`.
- Jika format salah, method mengembalikan `false`.

##### `updateRecentTransactions(DatabaseHelper dbHelper)`

- Mengisi 3 baris transaksi terakhir di Home.
- Mengatur visibility baris.
- Mengatur icon kategori.
- Mengatur judul, kategori, nominal, dan warna nominal.

ID yang dipakai:

- `rowTransaksi1`, `rowTransaksi2`, `rowTransaksi3`
- `ivTransaksiIcon1`, `ivTransaksiIcon2`, `ivTransaksiIcon3`
- `tvTransaksi1`, `tvTransaksi2`, `tvTransaksi3`
- `tvTransaksiKategori1`, `tvTransaksiKategori2`, `tvTransaksiKategori3`
- `tvTransaksiAmount1`, `tvTransaksiAmount2`, `tvTransaksiAmount3`
- `tvTransaksiEmpty`

##### `getSortedRecentTransactionItems(...)`

- Membaca semua transaksi.
- Membuat list sementara.
- Mem-parse tanggal.
- Mengurutkan transaksi berdasarkan:
  1. tanggal valid lebih dulu
  2. tahun terbaru
  3. bulan terbaru
  4. hari terbaru
  5. id terbaru sebagai tie-breaker

##### `parseRecentTransactionDate(...)`

- Memecah string tanggal dengan `/`.
- Validasi tanggal memakai `Calendar` dengan `setLenient(false)`.
- Jika tanggal tidak valid, transaksi tetap aman tetapi dianggap invalid.

##### `getTransactionIconRes(...)`

- Memilih icon berdasarkan kategori/type.
- `Pemasukan` memakai `ic_category_income_active`.
- Kategori lain seperti `Makanan`, `Camilan`, `Belanja`, `Tagihan`, dan lainnya memakai icon kategori masing-masing.

#### Alur kerja halaman

1. Home dibuka.
2. `onCreate()` memanggil update data.
3. Dashboard diisi dari SQLite dan SharedPreferences.
4. User bisa klik nav atau plus.
5. Saat kembali dari tambah transaksi, `onResume()` refresh data.

#### Hubungan dengan XML

Layout:

- `activity_home.xml`

ID utama:

- `tvNamaUser`
- `tvSaldo`
- `tvPemasukan`
- `tvPengeluaran`
- `tvBulanSaldoBulanIni`
- `tvStatusSaldoBulanIni`
- `tvSaldoBulanIni`
- `rowTransaksi1/2/3`
- `tvBudgetBulanan`
- `pbBudget`
- `tvTerpakai`
- `tvSisaBudget`
- `customBottomNav`
- `fabAdd`

#### Hubungan dengan Database / SharedPreferences

Database:

- `getTotalPemasukan()`
- `getTotalPengeluaran()`
- `getAllTransactions()`

SharedPreferences:

- `SesiLogin` untuk username.
- `BudgetPrefs` untuk budget.

#### Hal yang jangan sembarang diubah

- Jangan ubah type string `Pemasukan` dan `Pengeluaran`.
- Jangan ubah format tanggal `d/M/yyyy`.
- Jangan ganti ID dashboard tanpa update Java.
- Jangan ubah query database tanpa memahami dampaknya ke Home.
- Jangan ubah `SharedPreferences` key `USERNAME` dan `budget` sembarangan.

---

### TambahTransaksiActivity.java

#### Status Final TambahTransaksiActivity.java

`TambahTransaksiActivity.java` menangani penambahan transaksi baru.

Layout:

```text
activity_tambah_transaksi.xml
```

User bisa mengisi:

- type
- nominal
- tanggal
- kategori
- catatan

##### Type selector

Type transaksi:

- `Pengeluaran`
- `Pemasukan`

Nilai string ini masuk ke database dan dipakai banyak halaman. Jangan diganti ke lowercase, bahasa lain, atau ejaan berbeda tanpa migration menyeluruh.

##### Nominal formatter

Saat user mengetik nominal:

```text
1500000
```

UI menampilkan:

```text
1.500.000
```

Sebelum disimpan:

- titik dihapus
- `Rp` dihapus jika ada
- spasi dihapus
- karakter non-digit dibersihkan

Database menyimpan `amount` sebagai angka `REAL`.

##### DatePicker dan display date

User memilih tanggal via DatePicker.

Display ramah:

- `Hari ini, ...`
- `Kemarin, ...`
- nama hari Indonesia

Tetapi DB date tetap:

```text
d/M/yyyy
```

Contoh:

```text
25/6/2026
```

Ini adalah kontrak penting lintas app.

##### Category selector

Kategori memakai icon selector.

Kategori `Pemasukan` biasanya selaras dengan type `Pemasukan`.

Kategori pengeluaran:

- `Makanan`
- `Camilan`
- `Belanja`
- `Tagihan`
- `Kesehatan`
- `Edukasi`
- `Hiburan`
- `Tabungan`
- `Sewa Kos`
- `Lainnya`

Kategori dipakai oleh:

- Riwayat filter
- icon mapping
- Pengeluaran breakdown
- Home recent transaction icon

##### Catatan auto-scroll

Field Catatan bisa tertutup keyboard. Karena itu Input page punya behavior auto-scroll saat Catatan fokus/diketik.

Pola umumnya:

```java
scrollView.postDelayed(() -> {
    scrollView.smoothScrollTo(0, etCatatan.getBottom());
}, 250);
```

Tujuannya bukan animasi fancy, tetapi usability: user tetap bisa melihat field yang sedang diketik.

##### Insert transaksi

Saat simpan:

```java
insertTransaction(type, amount, category, note, date)
```

Ini membuat row baru. Untuk update transaksi lama, jangan gunakan insert.

---


#### Fungsi utama file

`TambahTransaksiActivity.java` adalah halaman input transaksi. File ini bertanggung jawab untuk:

- memilih tipe transaksi
- memilih kategori
- mengisi nominal
- memilih tanggal
- mengisi catatan
- menyimpan transaksi ke SQLite
- membatalkan input
- auto-scroll ketika catatan diketik

#### Struktur class

```java
public class TambahTransaksiActivity extends AppCompatActivity
```

Class ini Activity yang menampilkan layout `activity_tambah_transaksi.xml`.

#### Variabel penting

- `CATEGORY_PEMASUKAN`
  - Konstanta string `"Pemasukan"`.
  - Dipakai untuk kategori pemasukan.

- `CATEGORY_MAKANAN`
  - Konstanta string `"Makanan"`.
  - Default kategori saat halaman dibuka.

- `selectedDateForDb`
  - Tanggal yang disimpan ke database.
  - Format tetap `d/M/yyyy`.
  - Contoh: `22/6/2026`.

- `selectedCategory`
  - Kategori yang dipilih user.
  - Default `Makanan`.

- `selectedYear`, `selectedMonth`, `selectedDay`
  - Menyimpan tanggal terpilih untuk DatePicker.

- `isFormattingNominal`
  - Flag agar `TextWatcher` nominal tidak memanggil dirinya sendiri terus-menerus.

- `isSyncingTypeAndCategory`
  - Flag agar sinkronisasi tipe dan kategori tidak berulang tanpa henti.

- `CategoryOption[] categoryOptions`
  - Daftar kategori, ID view, icon aktif, icon nonaktif, dan warna label.

#### Method penting dan cara kerjanya

##### `onCreate()`

- Memasang layout:

```java
setContentView(R.layout.activity_tambah_transaksi);
```

- Mengambil view:
  - `btnBatal`
  - `etNominal`
  - `etCatatan`
  - `tvTanggal`
  - `radioGroupTipe`
  - `radioPengeluaran`
  - `radioPemasukan`
  - `spKategori`
  - `inputScrollView`
  - `btnSimpan`

- Membuat `DatabaseHelper`.
- Mengatur Spinner lama.
- Mengatur category icon selector.
- Mengatur segmented type selector.
- Mengatur tanggal default hari ini.
- Mengatur formatting nominal.
- Mengatur auto-scroll catatan.
- Mengatur DatePicker.
- Mengatur tombol simpan.
- Mengatur tombol batal.

##### `setupCategorySelector(RadioGroup radioGroup)`

- Membuat array `CategoryOption`.
- Tiap option berisi:
  - nama kategori
  - root view ID
  - icon view ID
  - label view ID
  - icon aktif
  - icon nonaktif
  - warna label aktif

Kategori yang didukung:

- `Pemasukan`
- `Makanan`
- `Camilan`
- `Belanja`
- `Tagihan`
- `Kesehatan`
- `Edukasi`
- `Hiburan`
- `Tabungan`
- `Sewa Kos`
- `Lainnya`

##### `updateCategorySelection(String category, RadioGroup radioGroup)`

- Menyimpan kategori yang dipilih ke `selectedCategory`.
- Mengganti icon aktif/nonaktif.
- Mengganti warna label.
- Jika kategori `Pemasukan`, radio `Pemasukan` ikut dipilih.
- Jika kategori lain, radio `Pengeluaran` ikut dipilih.

Risiko:

- Jika nama kategori diubah, filter Riwayat dan icon mapping bisa ikut terdampak.

##### `updateTypeSegmentColors(...)`

- Mengatur warna teks radio button.
- Yang aktif hijau.
- Yang tidak aktif warna teks utama.

##### `setSelectedDate(...)`

- Menyimpan tanggal dalam tiga field:
  - `selectedYear`
  - `selectedMonth`
  - `selectedDay`
- Membuat `selectedDateForDb` dalam format `d/M/yyyy`.
- Mengubah teks `tvTanggal` ke format ramah user.

##### `formatDisplayDate(...)`

- Menampilkan tanggal:
  - `Hari ini, dd MMMM yy`
  - `Kemarin, dd MMMM yy`
  - `{Nama Hari}, dd MMMM yy`

Contoh:

- `Hari ini, 22 Juni 26`
- `Kemarin, 21 Juni 26`
- `Sabtu, 20 Juni 26`

##### `cleanNominalInput(String value)`

- Menghapus:
  - `Rp`
  - titik
  - spasi
  - karakter selain angka

Tujuannya agar nilai yang disimpan tetap angka murni.

##### `formatNominalInput(String value)`

- Mengubah input angka menjadi ribuan Indonesia.
- Contoh:
  - `1500000` menjadi `1.500.000`

##### `TextWatcher` nominal

`TextWatcher` memantau perubahan teks.

Method di dalamnya:

- `beforeTextChanged(...)`: sebelum teks berubah.
- `onTextChanged(...)`: saat teks berubah.
- `afterTextChanged(...)`: setelah teks berubah.

Di project ini, logic formatting dilakukan di `afterTextChanged()`.

##### `scrollToCatatan(...)`

- Dipanggil saat catatan fokus/diklik/diketik.
- Melakukan scroll agar input catatan terlihat saat keyboard muncul.
- Menggunakan `postDelayed` 250ms dan 450ms agar scroll terjadi setelah keyboard mulai muncul.

##### DatePicker click listener

- Dipasang pada `tvTanggal` dan parent tanggal.
- Membuka `DatePickerDialog`.
- Setelah user memilih tanggal, `setSelectedDate()` dipanggil.

##### Tombol simpan

Saat `btnSimpan` diklik:

1. Ambil nominal dari `etNominal`.
2. Bersihkan nominal dari titik/Rp.
3. Ambil catatan dari `etCatatan`.
4. Ambil tanggal dari `selectedDateForDb`.
5. Validasi nominal tidak kosong.
6. Tentukan tipe:
   - `radioPemasukan` -> `Pemasukan`
   - selain itu -> `Pengeluaran`
7. Ambil kategori dari `selectedCategory`.
8. Convert nominal ke `double`.
9. Simpan:

```java
dbHelper.insertTransaction(tipe, nominal, kategori, catatan, tanggal)
```

10. Jika berhasil, tampilkan Toast dan `finish()`.

##### Tombol batal

- `btnBatal.setOnClickListener(v -> finish())`
- Menutup halaman input tanpa menyimpan.

#### Alur kerja halaman

1. Halaman input dibuka.
2. Default type: `Pengeluaran`.
3. Default kategori: `Makanan`.
4. Default tanggal: hari ini.
5. User mengisi nominal/catatan/kategori.
6. User menekan `Tambahkan`.
7. Data disimpan ke SQLite.
8. Halaman ditutup.

#### Hubungan dengan XML

Layout:

- `activity_tambah_transaksi.xml`

ID penting:

- `radioGroupTipe`
- `radioPengeluaran`
- `radioPemasukan`
- `etNominal`
- `etCatatan`
- `tvTanggal`
- `spKategori`
- `btnBatal`
- `btnSimpan`
- `inputScrollView`
- kategori icon selector IDs seperti `catMakanan`, `ivCatMakanan`, `tvCatMakanan`

#### Hubungan dengan Database / SharedPreferences

Database:

- Memanggil `insertTransaction(...)`.

Tidak memakai SharedPreferences.

#### Hal yang jangan sembarang diubah

- Jangan ubah type string `Pemasukan` / `Pengeluaran`.
- Jangan ubah format tanggal database `d/M/yyyy`.
- Jangan masukkan `Rp` ke data nominal.
- Jangan hapus `selectedDateForDb`.
- Jangan hapus ID `etNominal`, `etCatatan`, `tvTanggal`, `btnSimpan`.
- Jangan ubah nama kategori tanpa menyesuaikan Riwayat/Home icon mapping.

---

### RiwayatActivity.java

#### Status Final RiwayatActivity.java

`RiwayatActivity.java` menampilkan daftar transaksi.

Layout:

```text
activity_riwayat.xml
```

Komponen:

- search bar
- filter chips
- RecyclerView
- empty state
- custom bottom nav

##### allTransactions

Riwayat menyimpan data mentah dari SQLite dalam list seperti `allTransactions`.

Data ini menjadi sumber untuk:

- search
- filter
- sort
- date grouping

##### Search

Search mencocokkan teks terhadap:

- note/catatan
- category
- type

Search bersifat case-insensitive.

##### Filter

Filter chip bisa menampilkan:

- semua transaksi
- pemasukan
- kategori tertentu

Search dan filter bekerja bersama. Jika search aktif dan filter aktif, hasil harus memenuhi keduanya.

##### Sorting

Sorting memakai tanggal hasil parsing `d/M/yyyy`.

Urutan:

- transaksi tanggal terbaru lebih atas
- jika tanggal sama, id lebih besar lebih atas

Kenapa id penting?

Karena dua transaksi bisa punya tanggal sama. ID membantu menampilkan transaksi yang dibuat belakangan lebih atas.

##### Date grouping

Setelah sorting, list diberi header tanggal.

Adapter menerima campuran:

- header tanggal
- transaction item

Header tanggal tidak clickable.

##### Click to Update/Delete

Transaction row punya click listener. Saat diklik:

```java
Intent intent = new Intent(RiwayatActivity.this, UpdateTransaksiActivity.class);
intent.putExtra(UpdateTransaksiActivity.EXTRA_TRANSACTION_ID, transaction.getId());
startActivity(intent);
```

Riwayat tidak mengirim seluruh data transaksi sebagai extra. Riwayat hanya mengirim ID, lalu UpdateTransaksiActivity mengambil data terbaru dari SQLite.

Ini lebih aman karena:

- data yang dibuka selalu sesuai row database
- Intent tidak membawa payload besar
- update/delete memakai primary key yang jelas

##### Refresh di onResume()

Riwayat reload data di `onResume()`.

Setelah Update/Delete selesai dan Activity ditutup, user kembali ke Riwayat dan list langsung fresh.

Hal yang jangan diubah sembarangan:

- search/filter state
- sorting by parsed date + id
- grouping header
- click hanya untuk transaction row, bukan header
- `EXTRA_TRANSACTION_ID`

---


#### Fungsi utama file

`RiwayatActivity.java` adalah halaman daftar riwayat transaksi. File ini bertanggung jawab untuk:

- membaca transaksi dari SQLite
- menyimpan list master transaksi
- melakukan pencarian
- melakukan filter chip
- sorting berdasarkan tanggal transaksi
- menampilkan empty state
- mengatur adapter RecyclerView
- mengatur custom bottom nav

#### Struktur class

```java
public class RiwayatActivity extends AppCompatActivity
```

Class ini adalah Activity halaman Riwayat.

#### Variabel penting

- `FILTER_SEMUA`
  - Nilai filter default.
  - Artinya semua transaksi ditampilkan.

- `CHIP_ACTIVE_TEXT_COLOR`
  - Warna teks chip aktif.

- `CHIP_INACTIVE_TEXT_COLOR`
  - Warna teks chip nonaktif.

- `ArrayList<Transaction> allTransactions`
  - List master semua transaksi dari database.
  - Search dan filter bekerja dari list ini, bukan query database setiap ketikan.

- `RecyclerView rvRiwayat`
  - Daftar transaksi.

- `TextView tvRiwayatEmpty`
  - Teks ketika hasil pencarian/filter kosong.

- `String currentSearchQuery`
  - Query search saat ini.

- `String currentCategoryFilter`
  - Filter chip saat ini.

- `ChipFilterOption[] chipFilterOptions`
  - Daftar chip dan ID view-nya.

#### Method penting dan cara kerjanya

##### `onCreate()`

Langkah kerja:

1. Memasang layout `activity_riwayat`.
2. Mengambil `rvRiwayat`, `tvRiwayatEmpty`, dan `etSearchRiwayat`.
3. Memasang `LinearLayoutManager` ke `RecyclerView`.
4. Membaca transaksi dari `DatabaseHelper.getAllTransactions()`.
5. Membuat objek `Transaction` untuk tiap row database.
6. Menutup cursor.
7. Memanggil `applyFiltersAndRender()`.
8. Memasang `TextWatcher` pada search.
9. Memasang keyboard action listener.
10. Memasang chip filter.
11. Memasang custom bottom nav.
12. Mengatur tombol back agar kembali ke Home.

##### `setupCustomBottomNavigation()`

- Memasang listener untuk:
  - `navHomeButton`
  - `navPengeluaranButton`
  - `navBudgetButton`
  - `navRiwayatButton`
  - `fabAdd`

`navRiwayatButton` tidak membuka halaman baru karena user sudah di Riwayat.

##### `openBottomNavActivity(...)`

- Membuka halaman tujuan.
- Mematikan animasi transisi.
- Menutup Riwayat dengan `finish()`.

##### `applyFiltersAndRender()`

Method ini pusat update list Riwayat.

Langkah:

1. Panggil `filterTransactions()`.
2. Sort hasil filter dengan `sortTransactions(...)`.
3. Buat adapter baru:

```java
TransactionAdapter adapter = new TransactionAdapter(filteredTransactions);
```

4. Pasang adapter ke `rvRiwayat`.
5. Jika hasil kosong:
   - `rvRiwayat` disembunyikan
   - `tvRiwayatEmpty` ditampilkan
6. Jika ada hasil:
   - `tvRiwayatEmpty` disembunyikan
   - `rvRiwayat` ditampilkan

##### `setupFilterChips()`

- Membuat daftar chip:
  - `Semua`
  - `Pemasukan`
  - `Makanan`
  - `Camilan`
  - `Belanja`
  - `Tagihan`
  - `Kesehatan`
  - `Edukasi`
  - `Hiburan`
  - `Tabungan`
  - `Sewa Kos`
  - `Lainnya`

- Tiap chip diberi click listener.
- Saat chip diklik:
  - `currentCategoryFilter` berubah.
  - visual chip diupdate.
  - list dirender ulang.

##### `updateChipSelection()`

- Mengganti background chip:
  - aktif: `riwayat_chip_active_bg`
  - nonaktif: `riwayat_chip_inactive_bg`
- Mengganti warna teks chip.

##### Search `TextWatcher`

Saat teks search berubah:

```java
currentSearchQuery = s == null ? "" : s.toString();
applyFiltersAndRender();
```

Artinya list langsung difilter saat user mengetik.

##### Keyboard Search/Enter listener

Jika user menekan Search/Done/Enter:

- keyboard disembunyikan
- focus search dihapus
- teks search tidak dihapus
- hasil filter tetap terlihat

##### `filterTransactions()`

- Mulai dari `allTransactions`.
- Memasukkan transaksi jika:
  - cocok dengan chip filter
  - cocok dengan search query

Search dan chip memakai AND behavior.

Contoh:

- Search `kos`, chip `Tagihan`: hanya transaksi Tagihan yang juga mengandung `kos`.

##### `matchesCategoryFilter(...)`

- `Semua`: semua transaksi lolos.
- `Pemasukan`: lolos jika type atau category adalah `Pemasukan`.
- Chip lain: cocok berdasarkan `category`.

##### `matchesSearchQuery(...)`

Mencari query pada:

- note
- category
- type

Case-insensitive karena semua dibandingkan dalam lowercase.

##### `sortTransactions(...)`

Mengurutkan transaksi:

1. Tanggal valid dulu.
2. Tahun terbaru.
3. Bulan terbaru.
4. Hari terbaru.
5. ID terbaru jika tanggal sama.

##### `parseTransactionDate(...)`

- Mem-parse format `d/M/yyyy`.
- Menggunakan `Calendar` untuk validasi.
- Jika tanggal salah, dianggap invalid.

#### Alur kerja halaman

1. Riwayat dibuka.
2. Semua transaksi dibaca dari database.
3. List difilter dan diurutkan.
4. Adapter menampilkan date header dan card.
5. User bisa search atau klik chip.
6. List dirender ulang.

#### Hubungan dengan XML

Layout:

- `activity_riwayat.xml`

ID penting:

- `etSearchRiwayat`
- `rvRiwayat`
- `tvRiwayatEmpty`
- chip IDs
- `customBottomNav`
- `fabAdd`
- nav button IDs

#### Hubungan dengan Database / SharedPreferences

Database:

- Memakai `getAllTransactions()`.

SharedPreferences:

- Tidak dipakai langsung.

#### Hal yang jangan sembarang diubah

- Jangan ubah urutan column cursor tanpa menyesuaikan pembacaan.
- Jangan ganti ID chip tanpa update Java.
- Jangan ubah `FILTER_SEMUA` sembarangan.
- Jangan ubah format tanggal tanpa update parser.
- Jangan ubah adapter flow tanpa memahami grouping header.

---

### UpdateTransaksiActivity.java

`UpdateTransaksiActivity.java` adalah halaman edit/delete transaksi.

Layout:

```text
activity_update_transaksi.xml
```

Halaman ini dibuat terpisah dari `TambahTransaksiActivity` agar add flow tidak ikut berisiko rusak.

#### Entry point

Halaman dibuka dari Riwayat melalui transaction ID:

```java
public static final String EXTRA_TRANSACTION_ID = "transaction_id";
```

Jika ID missing/invalid:

- tampilkan pesan aman
- jangan query/update/delete row invalid

#### Load transaction by ID

Data dimuat via:

```java
getTransactionById(transactionId)
```

Cursor harus ditutup setelah dipakai.

Jika cursor kosong:

- transaksi tidak ditemukan
- halaman bisa ditutup aman

#### Prefill

Field yang diprefill:

- type
- nominal
- date display
- selected DB date
- category
- note/catatan

#### Type locked

Type selector terlihat, tetapi tidak boleh mengubah jenis transaksi.

Alasan:

- Mengubah `Pengeluaran` menjadi `Pemasukan` bisa mengubah makna data historis.
- Reporting Home/Pengeluaran/Budget bergantung pada type.
- Flow update difokuskan pada edit amount/date/category/note, bukan convert transaction type.

Saat update, Activity memakai:

```java
originalType
```

bukan mengambil nilai type dari UI yang mungkin berubah.

#### Update / Simpan

Tombol `Simpan`:

1. validasi transaction ID
2. parse nominal
3. validasi nominal > 0
4. validasi selected DB date
5. validasi selected category
6. trim note
7. panggil:

```java
updateTransaction(transactionId, originalType, amount, selectedCategory, note, selectedDbDate)
```

Jika sukses:

- Toast sukses
- `setResult(RESULT_OK)`
- `finish()`

Jika gagal:

- Toast gagal
- tetap di halaman

Tidak boleh memanggil `insertTransaction(...)` dari update page.

#### Delete / Hapus

Tombol `Hapus` menampilkan confirmation dialog.

Dialog:

- title: `Hapus Transaksi?`
- message: `Transaksi yang dihapus tidak dapat dikembalikan.`
- negative: `Batal`
- positive: `Hapus`

Warna button dialog:

- `Batal`: `#306D29`
- `Hapus`: `#B3261E`

Positive click memanggil:

```java
deleteTransaction(transactionId)
```

Jika sukses:

- Toast `Transaksi berhasil dihapus`
- `setResult(RESULT_OK)`
- `finish()`

#### Catatan auto-scroll

Update page memakai auto-scroll Catatan seperti Input page. Ini penting karena layout edit panjang dan keyboard bisa menutup field Catatan.

Hal yang jangan diubah sembarangan:

- `EXTRA_TRANSACTION_ID`
- `originalType`
- parse amount
- selected DB date
- category string
- delete confirmation
- dialog positive button logic

---

### PengeluaranActivity.java

#### Status Final PengeluaranActivity.java

`PengeluaranActivity.java` adalah halaman analisis pengeluaran bulanan.

Layout:

```text
activity_pengeluaran.xml
```

Fitur:

- total pengeluaran bulan terpilih
- month chip / month selector
- chart 6 bulan
- category breakdown
- custom bottom nav

##### Selected month state

Halaman menyimpan bulan yang sedang dipilih user.

Saat `onResume()`:

- data refresh
- selected month tidak direset ke bulan sekarang

Kenapa selected month harus dipertahankan?

Jika user sedang melihat bulan lama, lalu membuka halaman lain dan kembali, user tidak ingin tiba-tiba kembali ke bulan sekarang.

##### Data source

Pengeluaran membaca transaksi SQLite, lalu filter:

- type `Pengeluaran`
- date sesuai bulan/tahun target

Date parser harus tetap sesuai format:

```text
d/M/yyyy
```

##### Category breakdown

Breakdown kategori menjumlahkan amount per kategori pengeluaran.

Kategori yang tidak ada transaksi biasanya tidak perlu tampil sebagai card aktif.

##### Chart 6 bulan

Chart menampilkan data pengeluaran beberapa bulan terakhir. Rendering chart dilakukan oleh custom view `ExpenseBarChartView`.

---


#### Fungsi utama file

`PengeluaranActivity.java` menampilkan analisis pengeluaran berdasarkan kategori.

#### Struktur class

```java
public class PengeluaranActivity extends AppCompatActivity
```

Ini Activity biasa yang memakai layout `activity_pengeluaran.xml`.

#### Variabel penting

- `tvTotalPengeluaran`
  - Menampilkan total pengeluaran.

- `containerKategori`
  - `LinearLayout` tempat item analisis kategori ditempel secara dinamis.

- `DatabaseHelper dbHelper`
  - Mengambil total pengeluaran dan analisis kategori.

- `Cursor cursor`
  - Hasil query `getExpenseAnalysis()`.

#### Method penting dan cara kerjanya

##### `onCreate()`

Langkah:

1. Memasang layout.
2. Mengambil `tvTotalPengeluaran` dan `containerKategori`.
3. Menghapus isi lama container.
4. Ambil total pengeluaran.
5. Ambil data analisis kategori.
6. Untuk setiap kategori:
   - inflate `item_analisis.xml`
   - isi nama kategori
   - isi persen
   - isi progress bar
   - isi total
   - tambahkan ke `containerKategori`
7. Pasang back handler ke Home.
8. Pasang bottom navigation bawaan Material.
9. Pasang FAB untuk tambah transaksi.

##### `LayoutInflater.inflate(...)`

Digunakan untuk membuat UI item analisis dari XML:

```java
View itemView = inflater.inflate(R.layout.item_analisis, containerKategori, false);
```

Ini seperti mencetak template `item_analisis.xml`, lalu Java mengisi datanya.

#### Alur kerja halaman

1. Pengeluaran dibuka.
2. Total pengeluaran ditampilkan.
3. Data per kategori ditampilkan sebagai list manual dalam `LinearLayout`.
4. User bisa pindah halaman lewat bottom nav.

#### Hubungan dengan XML

Layout:

- `activity_pengeluaran.xml`
- `item_analisis.xml`

ID:

- `tvTotalPengeluaran`
- `containerKategori`
- `tvNamaKategori`
- `tvPersenKategori`
- `pbKategori`
- `tvTotalKategori`
- `bottomNavigation`
- `fabAdd`

#### Hubungan dengan Database / SharedPreferences

Database:

- `getTotalPengeluaran()`
- `getExpenseAnalysis()`

#### Hal yang jangan sembarang diubah

- Jangan ubah ID `containerKategori`.
- Jangan ubah ID dalam `item_analisis.xml` tanpa update Java.
- Jangan ubah query analisis kategori tanpa memahami efek ke UI.

---

### ExpenseBarChartView.java

`ExpenseBarChartView.java` adalah custom view untuk menggambar chart pengeluaran.

Karena custom view menggambar manual ke Canvas, perubahan kecil pada konstanta spacing bisa berdampak besar.

Elemen chart:

- bar 6 bulan
- label bulan
- nilai/scale visual
- average line / reference line jika ada
- spacing kiri/kanan/atas/bawah

Kenapa hati-hati?

- Chart bukan layout XML biasa.
- Posisi bar dan label dihitung manual.
- Jika padding/spacing diubah sembarangan, label bisa overlap, bar bisa kepotong, atau chart terlihat tidak center.

Jika ingin mengubah chart:

1. audit ukuran canvas
2. audit padding internal
3. audit tinggi bar max
4. audit label baseline
5. test di layar kecil dan besar

---

### BudgetActivity.java

#### Status Final BudgetActivity.java

`BudgetActivity.java` adalah halaman budget global bulanan.

Layout:

```text
activity_budget.xml
```

Budget bukan per kategori.

Storage:

```text
BudgetPrefs / budget
```

##### Current-month expense

Budget menghitung pengeluaran bulan berjalan dari SQLite:

1. baca `getAllTransactions()`
2. filter `type == "Pengeluaran"`
3. parse date `d/M/yyyy`
4. ambil hanya bulan/tahun sekarang
5. jumlahkan amount

Budget tidak memakai `getTotalPengeluaran()` untuk terpakai, karena method itu total all-time.

##### UI yang di-bind

Budget mengisi:

- `tvSisaBudget`
- `tvBudgetUsed`
- `tvBudgetTotal`
- `tvBudgetPercentBadge`
- `progressBudget`
- status card icon/title/description

##### Rupiah formatting

Angka diformat dengan titik:

```text
Rp 3.000.000
```

Sisa budget boleh negatif:

```text
- Rp 245.000
```

Progress bar capped 0-100, tetapi percentage badge boleh menunjukkan lebih dari 100%.

##### Status card

Kondisi:

- `< 80%`: aman / terkendali
- `80% - 100%`: hampir mencapai batas
- `> 100%`: melebihi budget

Status mengganti:

- icon
- background icon container
- title
- description

##### Edit budget card

Card edit hidden by default.

Alur:

1. user tap `Edit`
2. `cardEditBudget` tampil
3. input diprefill dengan `Rp ...`
4. user edit nominal
5. `Simpan` validasi dan simpan ke `BudgetPrefs/budget`
6. UI refresh tanpa recreate
7. `Batal` hide card tanpa save

Input menerima:

- `3500000`
- `3.500.000`
- `Rp 3.500.000`

##### onResume refresh

Budget refresh di `onResume()` agar update/delete transaksi dari halaman lain langsung mempengaruhi:

- terpakai
- sisa
- progress
- percentage
- status card

---


#### Fungsi utama file

`BudgetActivity.java` menampilkan dan mengubah budget global bulanan user. Budget final bukan budget per kategori.

#### Struktur class

```java
public class BudgetActivity extends AppCompatActivity
```

#### Variabel penting

- `etTargetBudget`
  - Input budget baru.

- `btnUpdateBudget`
  - Tombol simpan budget.

- `tvSisaBudget`
  - Menampilkan sisa budget.

- `tvInfoBudget`
  - Menampilkan persen pemakaian budget.

- `progressBudget`
  - ProgressBar budget.

- `SharedPreferences prefs`
  - Menyimpan budget ke `BudgetPrefs`.

#### Method penting dan cara kerjanya

##### `onCreate()`

Langkah:

1. Mengatur back button agar kembali ke Home.
2. Memasang layout `activity_budget`.
3. Mengambil view dengan `findViewById`.
4. Mengambil total pengeluaran dari database.
5. Mengambil budget dari `SharedPreferences`.
6. Menghitung sisa budget.
7. Menghitung persentase pemakaian.
8. Menampilkan data ke UI.
9. Mengatur bottom navigation bawaan Material.
10. Mengatur FAB tambah transaksi.
11. Mengatur tombol update budget.

##### `btnUpdateBudget.setOnClickListener(...)`

- Membaca input budget.
- Jika kosong, tampilkan `Toast`.
- Jika ada:
  - parse ke `float`
  - simpan ke `SharedPreferences` key `budget`
  - tampilkan Toast
  - `recreate()` agar halaman reload

#### Alur kerja halaman

1. Budget dibuka.
2. App baca budget lama atau default `5000000`.
3. App hitung pemakaian berdasarkan total pengeluaran.
4. User bisa input budget baru.
5. User klik update.
6. Budget disimpan lokal.

#### Hubungan dengan XML

Layout:

- `activity_budget.xml`

ID:

- `etTargetBudget`
- `btnUpdateBudget`
- `tvSisaBudget`
- `tvInfoBudget`
- `progressBudget`
- `bottomNavigation`
- `fabAdd`

#### Hubungan dengan Database / SharedPreferences

Database:

- `getTotalPengeluaran()`

SharedPreferences:

- `BudgetPrefs`
- key `budget`

#### Hal yang jangan sembarang diubah

- Jangan ubah key `budget` tanpa update Home.
- Jangan ubah ID input/button tanpa update Java.

---

### TransactionAdapter.java

#### Status Final TransactionAdapter.java

`TransactionAdapter.java` adalah penghubung data transaksi ke RecyclerView.

Adapter menangani dua item type:

- date header
- transaction row

##### Date header item

Header menampilkan tanggal group.

Header tidak boleh clickable karena bukan transaksi dan tidak punya transaction ID.

Jika header ikut clickable, app bisa mencoba membuka UpdateTransaksiActivity dengan ID invalid.

##### Transaction item

Transaction row menampilkan:

- icon kategori
- note/catatan sebagai title
- category/type sebagai subtitle
- amount di kanan

Amount formatting:

- `Pemasukan`: `+ Rp ...`, hijau
- `Pengeluaran`: `- Rp ...`, merah

##### Category icon mapping

Adapter memilih icon berdasarkan category/type.

Mapping ini harus konsisten dengan Input dan Home.

Jika kategori baru ditambahkan, update juga:

- Input category selector
- TransactionAdapter icon mapping
- Home icon mapping
- Pengeluaran breakdown/icon mapping jika ada
- Riwayat filter chips jika kategori harus bisa difilter

##### OnTransactionClickListener

Adapter punya callback:

```java
interface OnTransactionClickListener {
    void onTransactionClick(Transaction transaction);
}
```

Riwayat memasang listener ini agar click row transaksi membuka Update/Delete.

Keuntungan pola callback:

- Adapter tidak perlu tahu Activity tujuan.
- Adapter hanya memberi tahu bahwa transaction diklik.
- Riwayat yang menentukan navigation.

---


#### Fungsi utama file

`TransactionAdapter.java` menghubungkan data transaksi ke UI `RecyclerView` di Riwayat.

Adapter ini mendukung dua jenis item:

- header tanggal
- card transaksi

#### Struktur class

```java
public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
```

Artinya:

- class ini adapter untuk RecyclerView.
- `RecyclerView.ViewHolder` dipakai karena ada lebih dari satu tipe view.

#### Variabel penting

- `VIEW_TYPE_HEADER`
  - Angka untuk item header.

- `VIEW_TYPE_TRANSACTION`
  - Angka untuk item transaksi.

- `List<RiwayatListItem> riwayatListItems`
  - List gabungan antara header dan transaksi.

#### Method penting dan cara kerjanya

##### Constructor `TransactionAdapter(List<Transaction> transactionList)`

- Menerima list transaksi.
- Memanggil `buildGroupedItems(transactionList)`.
- Data mentah transaksi diubah menjadi list header + card.

##### `getItemViewType(int position)`

- Mengembalikan jenis item pada posisi tertentu.
- Jika header, RecyclerView akan memakai layout header.
- Jika transaksi, RecyclerView akan memakai layout card transaksi.

##### `onCreateViewHolder(...)`

- Membuat tampilan item.
- Jika `VIEW_TYPE_HEADER`, inflate:
  - `item_transaction_date_header.xml`
- Jika transaksi, inflate:
  - `item_transaction.xml`

##### `onBindViewHolder(...)`

- Mengisi data ke tampilan.
- Jika holder adalah header:
  - isi `tvDateHeader`
- Jika holder adalah transaksi:
  - isi icon kategori
  - isi note
  - isi category
  - sembunyikan `tvDate`
  - isi amount dengan format `+ Rp ...` atau `- Rp ...`
  - atur warna amount

##### `getItemCount()`

- Mengembalikan jumlah item gabungan.
- Jumlah ini termasuk header dan card.

##### `buildGroupedItems(...)`

- Membuat header tanggal ketika tanggal berubah.
- Jika beberapa transaksi tanggalnya sama, header hanya dibuat sekali.
- Jika tanggal invalid, header menjadi `Tanggal tidak valid`.

##### `formatHeaderText(...)`

- Mengubah tanggal menjadi:
  - `Hari ini, dd MMMM yyyy`
  - `Kemarin, dd MMMM yyyy`
  - `{Nama Hari}, dd MMMM yyyy`

##### `parseTransactionDate(...)`

- Mem-parse tanggal `d/M/yyyy`.
- Jika invalid, mengembalikan status invalid.

##### `formatSignedRupiah(...)`

- Income: `+ Rp 2.000.000`
- Expense: `- Rp 20.000`

##### `getTransactionIconRes(...)`

- Menentukan icon berdasarkan kategori atau type.
- `Pemasukan` memakai income icon.
- Kategori seperti `Makanan`, `Camilan`, `Belanja`, dan lain-lain punya icon sendiri.

#### Alur kerja halaman

1. `RiwayatActivity` memberi list transaksi ke adapter.
2. Adapter membuat list header + card.
3. RecyclerView memanggil `onCreateViewHolder`.
4. RecyclerView memanggil `onBindViewHolder`.
5. Card transaksi tampil di layar.

#### Hubungan dengan XML

Layout item:

- `item_transaction.xml`
- `item_transaction_date_header.xml`

ID item:

- `ivTransactionIcon`
- `tvNote`
- `tvCategory`
- `tvDate`
- `tvAmount`
- `tvDateHeader`

#### Hal yang jangan sembarang diubah

- Jangan hapus `tvDate` walaupun sekarang disembunyikan, karena adapter masih mencari ID-nya.
- Jangan ganti ID item tanpa update ViewHolder.
- Jangan ubah view type sembarangan.

---

### Transaction.java

#### Status Final Transaction.java

`Transaction.java` adalah model sederhana untuk satu transaksi.

Field:

- `id`
- `type`
- `amount`
- `category`
- `note`
- `date`

`id` sangat penting untuk fitur Update/Delete.

Tanpa id, Riwayat hanya tahu data visual transaksi, tetapi tidak tahu row database mana yang harus diedit/dihapus.

Current flow tidak membutuhkan setter besar-besaran karena data biasanya:

1. dibaca dari SQLite
2. dibuat menjadi object `Transaction`
3. dikirim ke adapter
4. dipakai untuk render/click

Update dilakukan melalui `DatabaseHelper`, bukan dengan mengubah object lalu auto-sync.

---


#### Fungsi utama file

`Transaction.java` adalah model data transaksi.

Class ini tidak menampilkan UI, tetapi UI membaca data dari class ini.

#### Struktur class

```java
public class Transaction
```

Ini class biasa, bukan Activity.

#### Variabel penting

- `id`
  - ID transaksi dari SQLite.
  - Dipakai untuk sorting tie-breaker.

- `type`
  - `Pemasukan` atau `Pengeluaran`.

- `amount`
  - Nominal transaksi.

- `category`
  - Kategori transaksi.

- `note`
  - Catatan transaksi.

- `date`
  - Tanggal transaksi dalam format `d/M/yyyy`.

#### Method penting dan cara kerjanya

##### Constructor

Ada beberapa constructor agar class bisa dibuat dengan data lama atau data lengkap.

Constructor lengkap:

```java
public Transaction(int id, String type, double amount, String category, String note, String date)
```

##### Getter

- `getId()`
- `getType()`
- `getAmount()`
- `getCategory()`
- `getNote()`
- `getDate()`

Getter dipakai oleh adapter dan Activity untuk membaca data.

#### Hubungan dengan XML

Tidak langsung.

`Transaction` dipakai oleh:

- `RiwayatActivity`
- `TransactionAdapter`

#### Hal yang jangan sembarang diubah

- Jangan hapus getter.
- Jangan ubah nama field tanpa update pemakai.
- Jangan ubah type `amount` tanpa memahami efek format Rupiah.

---

### DatabaseHelper.java

#### Status Final DatabaseHelper.java

Database:

```text
keuangan.db
```

Version:

```text
2
```

Table:

```text
transactions
```

Columns:

- `id INTEGER PRIMARY KEY AUTOINCREMENT`
- `type TEXT`
- `amount REAL`
- `category TEXT`
- `note TEXT`
- `date TEXT`

##### Insert

```java
insertTransaction(String type, double amount, String category, String note, String date)
```

Dipakai oleh Input page untuk membuat transaksi baru.

##### Read

```java
getAllTransactions()
getLastTransactions()
getTotalPemasukan()
getTotalPengeluaran()
getExpenseAnalysis()
```

`getAllTransactions()` dipakai banyak halaman karena fleksibel untuk filter di Java:

- Home monthly totals
- Riwayat list
- Budget current-month expense
- Pengeluaran selected month

##### CRUD by id

```java
getTransactionById(int id)
updateTransaction(int id, String type, double amount, String category, String note, String date)
deleteTransaction(int id)
```

Update/delete memakai `id` karena:

- id adalah primary key
- note/amount/date/category tidak unik
- transaksi dengan data mirip bisa lebih dari satu
- update/delete harus menyasar row yang tepat

Schema tidak perlu berubah untuk CRUD karena semua kolom edit sudah ada.

##### Kontrak penting

Jangan ubah string:

```text
Pengeluaran
Pemasukan
```

Jangan ubah format date:

```text
d/M/yyyy
```

Jika dua kontrak ini berubah, banyak halaman akan salah filter/hitung.

---


#### Fungsi utama file

`DatabaseHelper.java` mengatur SQLite lokal.

Dalam konteks UI, file ini menjadi sumber data untuk:

- Home dashboard
- Riwayat
- Pengeluaran analysis
- Budget calculation
- Input transaksi

#### Struktur class

```java
public class DatabaseHelper extends SQLiteOpenHelper
```

Artinya class ini helper resmi Android untuk membuat dan membaca database SQLite.

#### Variabel penting

- `DATABASE_NAME = "keuangan.db"`
  - Nama database lokal.

- `DATABASE_VERSION = 2`
  - Versi database.

#### Method penting dan cara kerjanya

##### `onCreate(SQLiteDatabase db)`

- Membuat table `transactions`.
- Kolom:
  - `id`
  - `type`
  - `amount`
  - `category`
  - `note`
  - `date`

##### `onUpgrade(...)`

- Drop table lama dan buat ulang.
- Sangat sensitif karena bisa menghapus data saat versi database naik.

##### `insertTransaction(...)`

- Dipanggil oleh `TambahTransaksiActivity`.
- Menyimpan:
  - type
  - amount
  - category
  - note
  - date

##### `getAllTransactions()`

- Mengambil semua transaksi.
- Dipakai oleh Home dan Riwayat.
- Query saat ini `ORDER BY id DESC`, tetapi Riwayat/Home melakukan sorting tambahan di Java untuk kebutuhan UI.

##### `getTotalPemasukan()`

- Mengambil total amount untuk `type='Pemasukan'`.
- Dipakai Home.

##### `getTotalPengeluaran()`

- Mengambil total amount untuk `type='Pengeluaran'`.
- Dipakai Home, Budget, Pengeluaran.

##### `getExpenseAnalysis()`

- Mengambil total pengeluaran per kategori.
- Dipakai `PengeluaranActivity`.

#### Hal yang jangan sembarang diubah

- Jangan ubah schema table tanpa migration yang aman.
- Jangan ubah nama kolom tanpa update semua Activity.
- Jangan ubah type string `Pemasukan` / `Pengeluaran`.
- Jangan ubah `onUpgrade()` sembarangan karena bisa menghapus data.

---

## 4. Penjelasan File XML Layout

### Ringkasan Final XML Layout Penting

#### activity_main.xml

Layout Login final.

Berisi:

- ScrollView untuk keyboard usability
- title `Halo!`
- subtitle Indonesia
- username field
- password field
- tombol `Masuk`

Recovery password sudah tidak ada.

ScrollView + `adjustResize` membantu tombol tetap reachable saat keyboard terbuka.

#### activity_home.xml

Layout dashboard Home.

Bagian utama:

- greeting user
- green balance card
- monthly finance card
- recent transaction card
- budget summary card
- custom bottom nav
- center FAB

Home white card shadow sudah dipoles agar tidak terlihat seperti border/stroke.

#### activity_tambah_transaksi.xml

Layout Input transaksi.

Bagian:

- amount section
- type selector
- date pill
- category grid
- note field
- bottom buttons `Batal` dan `Tambahkan`

Date pill memakai white background, no harsh stroke, dan subtle shadow dengan clipping fix.

#### activity_riwayat.xml

Layout Riwayat.

Bagian:

- search bar
- horizontal filter chips
- RecyclerView transaksi
- empty state
- custom bottom nav

RecyclerView diberi padding agar item tidak tertutup nav/FAB.

#### item_transaction.xml

Layout row transaksi.

Berisi:

- icon kategori
- note sebagai title
- category/type sebagai subtitle
- amount kanan

Seluruh transaction row adalah click target untuk membuka Update/Delete.

#### item_transaction_date_header.xml

Layout header tanggal.

Header hanya label group, bukan transaksi.

#### activity_update_transaksi.xml

Layout Update/Delete transaksi.

Visualnya mengikuti Input page, tetapi tombol bawah:

- `Hapus`
- `Simpan`

Type selector terlihat tetapi dikunci. Date pill, category grid, amount, dan catatan diprefill dari transaksi existing.

#### activity_pengeluaran.xml

Layout Pengeluaran final.

Bagian:

- header/summary
- month chip
- chart card
- category breakdown list
- custom bottom nav
- center FAB final

#### item_analisis.xml

Layout card/item analisis kategori pengeluaran.

Menampilkan:

- icon/kategori
- total kategori
- persentase/progress jika ada

#### activity_budget.xml

Layout Budget final.

Bagian:

- green header
- `Sisa Budget`
- used/total text
- progress bar
- percentage badge
- status card
- hidden edit budget card
- custom bottom nav

Tidak ada category budget section karena app belum punya storage budget per kategori.

---


### activity_main.xml

#### Fungsi utama layout

Layout halaman Login.

#### Struktur utama layout

Parent:

- `ConstraintLayout`

Di dalamnya ada `LinearLayout` `loginContent` sebagai container vertikal.

#### Komponen penting

- `loginContent`
  - Container utama isi login.

- `tvLoginTitle`
  - Judul `Hello Again!`.

- `tvLoginSubtitle`
  - Subtitle login.

- `etUsername`
  - Input username.
  - Dipakai `MainActivity`.

- `etPassword`
  - Input password.
  - Saat ini ada di UI, tetapi tidak dipakai logic login.

- `tvRecoveryPassword`
  - Teks visual recovery password.
  - Tidak ada logic reset password.

- `btnLogin`
  - Tombol login.
  - Dipakai `MainActivity`.

#### Style/tampilan

- Background: `@color/login_background` (`#FFFDF7`).
- Input memakai `@drawable/login_input_background`.
- Button memakai `MaterialButton` dengan tint hijau `@color/login_button`.
- Font Plus Jakarta Sans jika tersedia.

#### Hubungan dengan Java

Dipakai oleh:

- `MainActivity.java`

#### Hal yang jangan sembarang diubah

- ID `etUsername`, `etPassword`, `btnLogin`.
- Warna button harus tetap hijau jika mengikuti Figma.

---

### activity_home.xml

#### Fungsi utama layout

Layout dashboard Home.

#### Struktur utama layout

Parent:

- `CoordinatorLayout`

Isi utama:

- `NestedScrollView` untuk konten scroll.
- `customBottomNav` sebagai overlay bawah.
- `fabAdd` sebagai tombol plus tengah.

#### Komponen penting

- `tvNamaUser`
  - Nama user.
  - Diisi oleh `HomeActivity`.

- `tvSaldo`
  - Total saldo semua waktu.

- `cardSaldoBulanIni`
  - Card kecil di dalam green card.

- `tvBulanSaldoBulanIni`
  - Bulan/tahun sekarang.

- `tvStatusSaldoBulanIni`
  - Badge Positif/Negatif.

- `tvSaldoBulanIni`
  - Saldo bulan ini.

- `tvPemasukan`
  - Pemasukan bulan ini.

- `tvPengeluaran`
  - Pengeluaran bulan ini.

- `btnRecentToRiwayat`
  - Tombol arrow untuk membuka Riwayat.

- `rowTransaksi1/2/3`
  - Baris transaksi terakhir.

- `tvBudgetBulanan`
  - Label budget.

- `tvSisaBudget`
  - Sisa budget.

- `tvTerpakai`
  - Total pengeluaran.

- `pbBudget`
  - Progress budget.

- `customBottomNav`
  - Navbar bawah custom.

- `fabAdd`
  - Tombol input tengah.

#### Style/tampilan

- Background: `@color/home_background`.
- Green balance card: `home_balance_card_bg`.
- Inner monthly card: `home_monthly_card_bg`.
- Month chip: `home_month_chip_bg`.
- White cards: `home_white_card_bg`.
- Mini stat cards: `home_stat_card_bg`.
- Bottom nav: `bg_bottom_nav`.
- Center button: `bg_center_input_button`.

#### Hubungan dengan Java

Dipakai oleh:

- `HomeActivity.java`

#### Hal yang jangan sembarang diubah

- ID dashboard.
- ID transaksi terakhir.
- ID budget.
- ID nav.
- Struktur scroll dan bottom nav karena berhubungan dengan kenyamanan layout.

---

### activity_tambah_transaksi.xml

#### Fungsi utama layout

Layout halaman tambah transaksi.

#### Struktur utama layout

Parent:

- `LinearLayout` vertikal.

Isi:

- `ScrollView` untuk form input.
- Bottom button area untuk `Batal` dan `Tambahkan`.

#### Komponen penting

- `inputScrollView`
  - ScrollView utama form.
  - Dipakai untuk auto-scroll saat catatan fokus.

- `radioGroupTipe`
  - Grup pilihan `Pengeluaran` / `Pemasukan`.

- `radioPengeluaran`
  - Pilihan expense.
  - Disimpan sebagai type `Pengeluaran`.

- `radioPemasukan`
  - Pilihan income.
  - Disimpan sebagai type `Pemasukan`.

- `tvNominalCurrency`
  - Label visual `Rp`.

- `etNominal`
  - Input nominal asli.
  - Java membersihkan titik dan menyimpan angka.

- `tvTanggal`
  - Teks tanggal visual.
  - Klik membuka DatePicker.

- `spKategori`
  - Spinner lama.
  - Masih dipertahankan sebagai compatibility, tetapi category utama sekarang dari icon selector.

- Category selector:
  - `catPemasukan`
  - `catMakanan`
  - `catCamilan`
  - `catBelanja`
  - `catTagihan`
  - `catKesehatan`
  - `catEdukasi`
  - `catHiburan`
  - `catTabungan`
  - `catSewaKos`
  - `catLainnya`

- `ivNotesInput`
  - Icon catatan.

- `etCatatan`
  - Input catatan.
  - Mendukung multiline.

- `btnBatal`
  - Menutup halaman.

- `btnSimpan`
  - Menyimpan transaksi.

#### Style/tampilan

- Background: `#FFFDF7`.
- Segmented control: `input_segment_bg` dan `input_segment_option_bg`.
- Nominal background: `input_amount_bg`.
- Date pill: `input_date_pill_bg`.
- Note input: `input_note_bg`.
- Button cancel: `input_button_cancel_bg`.
- Button add: `input_button_add_bg`.
- Category icon memakai PNG aktif/nonaktif.

#### Hubungan dengan Java

Dipakai oleh:

- `TambahTransaksiActivity.java`

#### Hal yang jangan sembarang diubah

- ID input dan tombol.
- ID category selector.
- `spKategori` meskipun tersembunyi/kurang utama, karena Java masih mencari ID ini.
- `etCatatan` harus tetap satu ID visible.

---

### activity_riwayat.xml

#### Fungsi utama layout

Layout halaman Riwayat transaksi.

#### Struktur utama layout

Parent:

- `CoordinatorLayout`

Isi:

- `LinearLayout` `riwayatScreenWrapper` untuk search, chips, RecyclerView, empty state.
- `customBottomNav` sebagai overlay bawah.
- `fabAdd` sebagai tombol plus overlay.

#### Komponen penting

- `etSearchRiwayat`
  - Search bar.
  - Dipakai `RiwayatActivity`.

- `chipSemua`
- `chipPemasukan`
- `chipMakanan`
- `chipCamilan`
- `chipBelanja`
- `chipTagihan`
- `chipKesehatan`
- `chipEdukasi`
- `chipHiburan`
- `chipTabungan`
- `chipSewaKos`
- `chipLainnya`
  - Filter chip.
  - Dipakai `RiwayatActivity`.

- `rvRiwayat`
  - RecyclerView utama transaksi.
  - Adapter: `TransactionAdapter`.

- `tvRiwayatEmpty`
  - Empty state.

- `customBottomNav`
  - Navbar bawah custom.

- `fabAdd`
  - Tombol input tengah.

#### Style/tampilan

- Background: `#FFFDF7`.
- Search bar: `riwayat_search_bg`.
- Active chip: `riwayat_chip_active_bg`.
- Inactive chip: `riwayat_chip_inactive_bg`.
- Bottom nav: `bg_bottom_nav`.
- Center button: `bg_center_input_button`.

#### Hubungan dengan Java

Dipakai oleh:

- `RiwayatActivity.java`

#### Hal yang jangan sembarang diubah

- ID search, chips, RecyclerView, nav.
- `rvRiwayat` bottom behavior sudah sering dipoles; ubah hati-hati.
- Jangan tambahkan fade/mask tanpa kebutuhan jelas.

---

### activity_pengeluaran.xml

#### Fungsi utama layout

Layout halaman analisis pengeluaran.

#### Struktur utama layout

Parent:

- `CoordinatorLayout`

Isi final:

- `NestedScrollView` untuk konten.
- custom bottom nav (`customBottomNav`).
- center FAB (`fabAdd`) dengan struktur/ripple final.

#### Komponen penting

- `tvTotalPengeluaran`
  - Total pengeluaran.

- `containerKategori`
  - Tempat item kategori dimasukkan secara dinamis.

- `customBottomNav`
  - Bottom nav custom final.

- `fabAdd`
  - Tombol tambah transaksi di tengah nav.

#### Style/tampilan

- Ada card hijau untuk total pengeluaran.
- Item analisis dibuat dari `item_analisis.xml`.

#### Hubungan dengan Java

Dipakai oleh:

- `PengeluaranActivity.java`

#### Hal yang jangan sembarang diubah

- ID `tvTotalPengeluaran`.
- ID `containerKategori`.
- ID bottom nav/FAB jika Java belum diubah.

---

### activity_budget.xml

#### Fungsi utama layout

Layout halaman Budget.

#### Struktur utama layout

Parent:

- `CoordinatorLayout`

Isi:

- `NestedScrollView`.
- custom bottom nav (`customBottomNav`).
- center FAB (`fabAdd`).

#### Komponen penting

- `tvSisaBudget`
- `progressBudget`
- `tvInfoBudget`
- `etTargetBudget`
- `btnUpdateBudget`
- `bottomNavigation`
- `fabAdd`

#### Style/tampilan

- Card budget memakai `card_transaksi`.
- Input budget memakai `rounded_edittext`.

#### Hubungan dengan Java

Dipakai oleh:

- `BudgetActivity.java`

#### Hal yang jangan sembarang diubah

- ID budget yang dipakai Java.
- Custom bottom nav final; jaga ID nav dan FAB yang dipakai Java.

---

### item_transaction.xml

#### Fungsi utama layout

Layout satu card transaksi di Riwayat.

#### Struktur utama layout

Parent:

- `LinearLayout` horizontal.

#### Komponen penting

- `ivTransactionIcon`
  - Icon kategori.

- `tvNote`
  - Judul transaksi.
  - Sumber: note/catatan.

- `tvCategory`
  - Subtitle kategori.

- `tvDate`
  - Tanggal raw.
  - Saat ini disembunyikan oleh adapter.

- `tvAmount`
  - Nominal.

#### Style/tampilan

- Background: `riwayat_transaction_card_bg`.
- Icon sekitar 40dp.
- Teks nominal berada kanan.

#### Hubungan dengan Java

Dipakai oleh:

- `TransactionAdapter.TransactionViewHolder`

#### Hal yang jangan sembarang diubah

- Jangan hapus ID yang dicari adapter.

---

### item_transaction_date_header.xml

#### Fungsi utama layout

Layout header tanggal di Riwayat.

#### Struktur utama layout

Parent:

- `TextView`

#### Komponen penting

- `tvDateHeader`
  - Teks header tanggal.

#### Hubungan dengan Java

Dipakai oleh:

- `TransactionAdapter.HeaderViewHolder`

---

### item_analisis.xml

#### Fungsi utama layout

Layout item analisis kategori pengeluaran.

#### Komponen penting

- `tvNamaKategori`
- `tvPersenKategori`
- `pbKategori`
- `tvTotalKategori`

#### Hubungan dengan Java

Dipakai oleh:

- `PengeluaranActivity.java`

---

### Layout tambahan

Ada juga:

- `activity_transaction.xml`
- `activity_transaction_adapter.xml`

Dokumen ini fokus pada file yang aktif dipakai Activity utama. Jika file tambahan tidak dipanggil oleh Java utama, anggap sebagai layout lama/eksperimen sampai terbukti dipakai.

---

## 5. Penjelasan Adapter dan Item RecyclerView

`RecyclerView` adalah komponen Android untuk daftar panjang yang efisien.

Di Riwayat:

- `rvRiwayat` adalah RecyclerView.
- `TransactionAdapter` adalah adapter.
- `Transaction` adalah model data.
- `item_transaction.xml` adalah tampilan card transaksi.
- `item_transaction_date_header.xml` adalah tampilan header tanggal.

### Cara kerja sederhana

1. `RiwayatActivity` mengambil data dari database.
2. Data disimpan sebagai `ArrayList<Transaction>`.
3. Search dan filter diproses.
4. Data disortir.
5. List dikirim ke `TransactionAdapter`.
6. Adapter membuat header dan card.
7. RecyclerView menampilkan item.

### Date header

Adapter membuat header ketika tanggal berubah.

Contoh:

```text
Hari ini, 23 Juni 2026
[card transaksi]
[card transaksi]

Kemarin, 22 Juni 2026
[card transaksi]
```

Jika tanggal invalid:

```text
Tanggal tidak valid
```

### Card transaksi

Card transaksi menampilkan:

- icon kategori
- note/catatan sebagai judul
- kategori sebagai subtitle
- nominal kanan

### Icon kategori

Icon dipilih dari kategori:

- `Pemasukan` -> `ic_category_income_active`
- `Makanan` -> `ic_category_food_active`
- `Camilan` -> `ic_category_snack_active`
- `Belanja` -> `ic_category_shopping_active`
- `Tagihan` -> `ic_category_bill_active`
- `Kesehatan` -> `ic_category_health_active`
- `Edukasi` -> `ic_category_education_active`
- `Hiburan` -> `ic_category_entertainment_active`
- `Tabungan` -> `ic_category_savings_active`
- `Sewa Kos` -> `ic_category_house_active`
- `Lainnya` -> `ic_category_other_active`

### Warna dan format nominal

- `Pemasukan`: hijau `#306D29`, format `+ Rp ...`
- `Pengeluaran`: merah `#B3261E`, format `- Rp ...`

---

## 6. Penjelasan Search dan Filter Riwayat

Search dan filter ada di `RiwayatActivity.java`.

### Search bar

ID:

- `etSearchRiwayat`

Saat user mengetik, `TextWatcher` berjalan.

Yang dicari:

- note/catatan
- category
- type

Search case-insensitive.

### TextWatcher

`TextWatcher` memiliki:

- `beforeTextChanged`
- `onTextChanged`
- `afterTextChanged`

Project ini memakai `onTextChanged` untuk update query dan render ulang list.

### Keyboard Search/Enter

Saat user menekan Search/Done/Enter:

- keyboard disembunyikan
- focus input dihapus
- teks search tetap ada

### Filter chip

Chip yang tersedia:

- `Semua`
- `Pemasukan`
- `Makanan`
- `Camilan`
- `Belanja`
- `Tagihan`
- `Kesehatan`
- `Edukasi`
- `Hiburan`
- `Tabungan`
- `Sewa Kos`
- `Lainnya`

### Arti `Semua`

`Semua` berarti tidak ada filter kategori/type.

### Arti `Pemasukan`

`Pemasukan` menampilkan transaksi yang:

- `type == Pemasukan`
- atau `category == Pemasukan`

### Filter kategori lain

Kategori lain mencocokkan `transaction.getCategory()`.

### AND behavior

Search dan chip digabung dengan AND.

Contoh:

- Query: `kos`
- Chip: `Tagihan`

Hasil: hanya transaksi Tagihan yang juga mengandung kata `kos`.

### Empty state

Jika tidak ada hasil:

- RecyclerView disembunyikan.
- `tvRiwayatEmpty` ditampilkan.

### Sorting setelah filtering

Setelah filter, transaksi disortir lagi berdasarkan tanggal transaksi.

### Grouping setelah filtering

Setelah sorting, adapter membuat header tanggal lagi sesuai hasil filter.

---

## 7. Penjelasan Bottom Navigation

### Status Final Custom Bottom Navigation

Custom bottom nav diduplikasi di:

- `activity_home.xml`
- `activity_pengeluaran.xml`
- `activity_budget.xml`
- `activity_riwayat.xml`

Ini sengaja dipertahankan sebagai pattern project saat ini. Refactor menjadi reusable component bisa dilakukan di masa depan, tetapi jangan dilakukan casual karena nav sudah banyak dipoles.

#### Item nav

Item:

- Beranda
- Pengeluaran
- Budget
- Riwayat

Setiap item punya:

- icon `ImageView`
- label `TextView`

#### FAB tengah

Center FAB:

- id `fabAdd`
- membuka `TambahTransaksiActivity`
- memakai icon `ic_input`
- memakai background `bg_center_input_button`
- memakai ripple/halo circular final

#### Alignment final

Final QA menemukan Pengeluaran active/inactive tampak shift.

Perbaikan final:

- nav icon fixed `24dp x 24dp`
- `scaleType="centerInside"`
- `adjustViewBounds="false"`
- label `gravity="center"`
- label `textAlignment="center"`
- `includeFontPadding="false"`
- `singleLine="true"`
- font label regular untuk active/inactive
- Pengeluaran label `9sp`, sama dengan label lain

Kenapa active/inactive tidak memakai font weight berbeda?

Karena perubahan regular -> medium bisa mengubah metrik teks. Untuk label panjang seperti `Pengeluaran`, perubahan kecil ini terlihat seperti shift.

Kenapa PNG shift attempt tidak dijadikan fix utama?

Karena audit menunjukkan masalah tidak hanya asset. XML text metrics juga bisa membuat label/kelompok visual terlihat bergeser. Fix final menstabilkan layout metrics.

---


### Custom bottom nav

Custom bottom nav dipakai di:

- Home
- Riwayat

Struktur:

- `CoordinatorLayout` sebagai root.
- `customBottomNav` ditempel di bawah.
- `fabAdd` ditempel di bawah tengah.

### Tombol nav

ID:

- `navHomeButton`
- `navPengeluaranButton`
- `navBudgetButton`
- `navRiwayatButton`

Tiap tombol berisi:

- `ImageView` icon
- `TextView` label

### Center plus button

ID:

- `fabAdd`

Visual:

- background oval `bg_center_input_button`
- icon `ic_input`

Klik:

- membuka `TambahTransaksiActivity`.

### Active/inactive icons

Home aktif:

- `ic_home_active`

Riwayat aktif:

- `ic_history_active`

Inactive:

- `ic_home_inactive`
- `ic_expenditure_inactive`
- `ic_budget_inactive`
- `ic_history_inactive`

### Kenapa active state berbeda per page

Pada Home:

- Beranda hijau/aktif.
- Riwayat inactive.

Pada Riwayat:

- Riwayat hijau/aktif.
- Beranda inactive.

### Nav click listener

Java memakai:

```java
findViewById(R.id.navHomeButton).setOnClickListener(...)
```

Saat diklik:

- `Intent` dibuat.
- `startActivity(...)` membuka halaman tujuan.
- `finish()` menutup halaman sekarang.

---

## 8. Penjelasan Input Transaksi

### Type segmented switch

UI memakai:

- `radioGroupTipe`
- `radioPengeluaran`
- `radioPemasukan`

Walaupun tampil sebagai segmented control, logic tetap memakai RadioGroup.

Mapping:

- `radioPengeluaran` -> DB type `Pengeluaran`
- `radioPemasukan` -> DB type `Pemasukan`

### Category selector

Kategori sekarang menggunakan icon selector.

User klik kategori, Java menjalankan:

```java
updateCategorySelection(...)
```

Jika user klik `Pemasukan`, type otomatis menjadi `Pemasukan`.

Jika user klik kategori selain `Pemasukan`, type otomatis menjadi `Pengeluaran`.

### Nominal formatter

User mengetik angka di `etNominal`.

`TextWatcher` mengubah:

- `1500000` menjadi `1.500.000`

Sebelum disimpan, Java membersihkan:

- titik
- `Rp`
- spasi
- karakter non-angka

Jadi database tetap menerima angka murni.

### Date picker/display

`tvTanggal` menampilkan tanggal ramah:

- `Hari ini, 22 Juni 26`
- `Kemarin, 21 Juni 26`
- `Sabtu, 20 Juni 26`

Tetapi database menerima:

- `22/6/2026`

Format database wajib:

```text
d/M/yyyy
```

### Note field

`etCatatan` adalah input catatan.

UI punya icon:

- `ic_notes_input`

Catatan bisa multiline dan auto-scroll saat keyboard muncul.

### Save button

`btnSimpan`:

- validasi nominal
- ambil type
- ambil category
- ambil note
- ambil date
- panggil `insertTransaction(...)`

### Cancel button

`btnBatal`:

- `finish()`
- tidak menyimpan data

---

## 9. Penjelasan Home Dashboard

### Total saldo all-time

Home menghitung:

```text
total pemasukan semua waktu - total pengeluaran semua waktu
```

Ditampilkan di:

- `tvSaldo`

### Saldo bulan ini

Home membaca semua transaksi dan mengecek tanggal bulan ini.

```text
monthlyPemasukan - monthlyPengeluaran
```

Ditampilkan di:

- `tvSaldoBulanIni`

### Pemasukan/Pengeluaran bulan ini

Ditampilkan di:

- `tvPemasukan`
- `tvPengeluaran`

### Positif/Negatif badge

Jika saldo bulan ini >= 0:

- `Positif`
- hijau

Jika saldo bulan ini < 0:

- `Negatif`
- merah

### Recent transactions

Home menampilkan maksimal 3 transaksi.

Sorting:

- tanggal transaksi terbaru
- jika tanggal sama, ID terbaru

### Budget card

Home membaca budget dari:

- `SharedPreferences` file `BudgetPrefs`
- key `budget`

Jika tidak ada, default `5000000`.

### Arrow to Riwayat

`btnRecentToRiwayat` membuka `RiwayatActivity`.

### Category icons

Home memakai mapping kategori yang mirip dengan Riwayat.

---

## 10. Penjelasan Drawables / Background / Icons

### UI Polish Final

Token warna utama:

- app background: `#FFFDF7`
- primary green: `#306D29`
- main text: `#262A24`
- destructive/expense red: `#B3261E`
- orange progress/badge: `#FFA43C`
- neutral input/card border: `#EDEDE8`

Font:

- Plus Jakarta Sans dipakai untuk banyak label dan UI modern.
- Roboto masih bisa muncul pada area tertentu sesuai desain lama/Android default.

#### Shadow polish

Riwayat, Pengeluaran, Budget, Home, Input, dan Update mengalami beberapa iterasi shadow.

Pelajaran penting:

- Android `elevation` tidak sama persis dengan Figma `box-shadow`.
- `6dp` native elevation terasa terlalu berat.
- Banyak card akhirnya dibuat lebih soft dengan `1dp-3dp`.
- Shadow clipping diperbaiki dengan parent `clipChildren=false` / `clipToPadding=false` pada area yang tepat.
- Jangan menambahkan shadow ke button, chips, progress bar, bottom nav, atau FAB jika desain tidak meminta.

#### Date pill polish

Input dan Update date pill:

- white background
- no visible harsh stroke
- subtle shadow
- clipping diperbaiki

#### Delete dialog polish

Dialog delete tetap default AlertDialog, tetapi button color disesuaikan:

- `Batal`: hijau
- `Hapus`: merah

Tidak perlu custom dialog layout karena default layout sudah cukup.

#### Login polish

Login:

- teks Indonesia
- Recovery Password dihapus
- button keyboard reachable
- transition keyboard -> Home diperbaiki

---


### Bottom nav

- `bg_bottom_nav.xml`
  - putih
  - top-left/top-right radius 16dp
  - dipakai `customBottomNav`

- `bg_center_input_button.xml`
  - oval
  - warna `#FBF5DD`
  - stroke putih
  - dipakai `fabAdd`

### Login

- `login_input_background.xml`
  - putih
  - radius 12dp
  - stroke `login_input_stroke`

- `login_button_background.xml`
  - hijau `login_button`
  - radius 12dp

### Home

- `home_balance_card_bg.xml`
  - green card utama.

- `home_monthly_card_bg.xml`
  - inner card transparan putih.

- `home_month_chip_bg.xml`
  - chip bulan.

- `home_stat_card_bg.xml`
  - mini card pemasukan/pengeluaran.

- `home_white_card_bg.xml`
  - card putih Recent/Budget.

- `home_status_positive_bg.xml`
  - badge positif.

- `home_status_negative_bg.xml`
  - badge negatif.

- `home_chevron_circle_bg.xml`
  - background tombol arrow ke Riwayat.

- `home_progress_bar.xml`
  - progress bar budget custom.

### Input

- `input_segment_bg.xml`
  - background segmented type.

- `input_segment_option_bg.xml`
  - selector checked/uncheck radio segment.

- `input_amount_bg.xml`
  - background nominal transparan.

- `input_date_pill_bg.xml`
  - pill tanggal.

- `input_note_bg.xml`
  - background input catatan.

- `input_button_cancel_bg.xml`
  - tombol Batal.

- `input_button_add_bg.xml`
  - tombol Tambahkan.

### Riwayat

- `riwayat_search_bg.xml`
  - search bar abu-abu.

- `riwayat_chip_active_bg.xml`
  - chip aktif hijau.

- `riwayat_chip_inactive_bg.xml`
  - chip nonaktif putih.

- `riwayat_transaction_card_bg.xml`
  - card transaksi putih radius 16dp.

### Category icons

Aktif/nonaktif:

- `ic_category_income_active/inactive`
- `ic_category_food_active/inactive`
- `ic_category_snack_active/inactive`
- `ic_category_shopping_active/inactive`
- `ic_category_bill_active/inactive`
- `ic_category_health_active/inactive`
- `ic_category_education_active/inactive`
- `ic_category_entertainment_active/inactive`
- `ic_category_savings_active/inactive`
- `ic_category_house_active/inactive`
- `ic_category_other_active/inactive`

### Nav icons

- `ic_home_active/inactive`
- `ic_expenditure_active/inactive`
- `ic_budget_active/inactive`
- `ic_history_active/inactive`
- `ic_input`

### Other icons

- `ic_income`
- `ic_expense`
- `ic_calendar_input`
- `ic_notes_input`

### Colors and fonts

`colors.xml` berisi token penting:

- `login_background`
- `login_button`
- `home_background`
- `home_primary_green`
- `home_text_primary`
- `home_card_surface`
- `home_border`

Folder font:

- Plus Jakarta Sans
- Roboto

---

## 11. Ringkasan File yang Aman dan Sensitif

| File | Role | Safe to edit? | Risk level | Notes |
|---|---|---:|---:|---|
| `activity_main.xml` | UI Login | Ya, visual | Low/Medium | Jangan ganti `etUsername`, `etPassword`, `btnLogin`. |
| `MainActivity.java` | Logic Login | Hati-hati | Medium | Menyimpan username ke `SharedPreferences`. |
| `activity_home.xml` | UI Home | Ya, visual | Medium | Banyak ID dipakai `HomeActivity`. |
| `HomeActivity.java` | Dashboard logic | Hati-hati | High | Menghitung saldo, budget, transaksi terakhir. |
| `activity_tambah_transaksi.xml` | UI input transaksi | Ya, visual | Medium/High | Banyak ID dipakai save flow. |
| `TambahTransaksiActivity.java` | Logic input/save transaksi | Sangat hati-hati | High | Menentukan type, category, nominal, tanggal, insert DB. |
| `activity_riwayat.xml` | UI Riwayat | Ya, visual | Medium | Jangan ubah ID search/chip/RV/nav. |
| `RiwayatActivity.java` | Search/filter/sort/nav Riwayat | Hati-hati | High | Mengatur filter, sorting, adapter. |
| `item_transaction.xml` | Card transaksi | Ya, visual | Medium | ID dipakai adapter. |
| `item_transaction_date_header.xml` | Header tanggal | Ya, visual | Low/Medium | ID `tvDateHeader` dipakai adapter. |
| `TransactionAdapter.java` | Adapter Riwayat | Hati-hati | Medium/High | Mengatur header, card, icon, amount. |
| `Transaction.java` | Model transaksi | Jarang | Medium/High | Dipakai adapter dan Activity. |
| `DatabaseHelper.java` | SQLite lokal | Jangan sembarang | High | Schema dan query data utama. |
| `activity_pengeluaran.xml` | UI analisis pengeluaran | Ya, visual | Medium | ID dipakai `PengeluaranActivity`. |
| `PengeluaranActivity.java` | Logic analisis pengeluaran | Hati-hati | Medium/High | Membaca cursor analisis kategori. |
| `activity_budget.xml` | UI budget | Ya, visual | Medium | ID dipakai `BudgetActivity`. |
| `BudgetActivity.java` | Logic budget | Hati-hati | Medium/High | Memakai `SharedPreferences` budget. |
| Drawable XML | Background/icon shape | Umumnya aman | Low/Medium | Hati-hati jika dipakai banyak halaman. |
| PNG icons | Asset visual | Aman jika tambah baru | Low/Medium | Jangan rename asset yang sudah direferensikan. |
| `colors.xml` | Token warna | Hati-hati | Medium | Bisa berdampak global. |
| Gradle files | Build config | Jangan kecuali perlu | High | Bisa memutus build. |
| `AndroidManifest.xml` | Deklarasi app/activity | Jangan kecuali perlu | High | Bisa memutus navigasi Activity. |

---

## 12. Checklist Jika Mau Edit UI Lagi

### Data Consistency dan Refresh Flow Final

Semua halaman data-driven harus refresh ketika user kembali dari halaman lain.

#### Home

Refresh di `onResume()`:

- saldo
- saldo bulan ini
- pemasukan/pengeluaran bulan ini
- recent transactions
- budget card

#### Riwayat

Refresh di `onResume()`:

- transaksi baru
- transaksi update
- transaksi delete
- search/filter tetap dirender ulang dengan data terbaru

#### Pengeluaran

Refresh di `onResume()`:

- total pengeluaran bulan terpilih
- chart
- breakdown kategori

Selected month dipertahankan.

#### Budget

Refresh di `onResume()`:

- terpakai bulan ini
- sisa budget
- progress
- percentage badge
- status card

#### Kenapa tidak butuh restart app?

Karena tiap halaman yang bergantung pada SQLite/SharedPreferences membaca ulang data saat kembali aktif. Ini membuat update/delete transaksi langsung tercermin di halaman lain.

---

### Catatan Penting: Do Not Change Carelessly

Jangan ubah hal berikut tanpa rencana:

#### DB date format

```text
d/M/yyyy
```

Dipakai oleh parser lintas halaman.

#### Transaction type strings

```text
Pengeluaran
Pemasukan
```

Dipakai oleh filter, total, chart, budget, dan warna amount.

#### SharedPreferences keys

```text
SesiLogin / USERNAME
BudgetPrefs / budget
```

Jika diubah, data lama user tidak terbaca.

#### Package/applicationId

Jangan ganti hanya untuk rename app. App label cukup diubah lewat `app_name`.

#### Bottom nav duplication

Custom bottom nav memang duplicate di beberapa XML. Jangan refactor cepat tanpa test semua halaman karena alignment/FAB/ripple sudah dipoles manual.

#### Chart spacing constants

`ExpenseBarChartView` memakai Canvas drawing manual. Ubah spacing hanya setelah visual QA.

#### CRUD schema

Update/delete sudah cukup dengan id dan kolom yang ada. Jangan ubah schema tanpa migration plan.

#### Category budget

Budget per kategori belum diimplementasikan. Jangan menambahkan UI category budget yang seolah-olah editable kalau storage/logic belum ada.

---

### Final QA Checklist Detail

#### Login

- App label tampil `Artos`.
- Login menampilkan teks Indonesia.
- Recovery Password tidak muncul.
- Username/password field bisa difokuskan.
- Keyboard tidak menutup tombol `Masuk` secara permanen.
- Login sukses membuka Home.
- Login saat keyboard terbuka tidak membuat Home bottom nav glitch.

#### Home

- Greeting tampil.
- Total saldo all-time benar.
- Saldo bulan ini benar.
- Pemasukan/pengeluaran bulan ini benar.
- Recent transactions tampil.
- Budget card mengikuti budget terbaru.
- Data refresh setelah tambah/update/delete transaksi.
- Bottom nav tidak shift.

#### Tambah Transaksi

- Bisa pilih `Pengeluaran`.
- Bisa pilih `Pemasukan`.
- Nominal format titik.
- DatePicker tampil.
- Display date memakai Hari ini/Kemarin/nama hari.
- DB date tetap `d/M/yyyy`.
- Category selector bekerja.
- Catatan auto-scroll.
- Simpan insert row baru.

#### Riwayat

- List tampil.
- Search bekerja.
- Filter chips bekerja.
- Date grouping benar.
- Header tidak clickable.
- Row transaksi clickable.
- Setelah update/delete, list refresh.
- Empty state tampil saat tidak ada hasil.

#### Update/Delete

- Dibuka dari Riwayat dengan ID benar.
- Data prefill benar.
- Type terlihat tapi locked.
- Nominal update tersimpan.
- Category update tersimpan.
- Date update memindahkan group Riwayat.
- Catatan update tersimpan.
- Hapus menampilkan dialog.
- Batal dialog tidak delete.
- Hapus dialog delete row.
- Tidak membuat duplicate row.

#### Pengeluaran

- Total pengeluaran bulan terpilih benar.
- Month selector bekerja.
- Chart 6 bulan tampil.
- Category breakdown tampil.
- Data refresh setelah update/delete.
- Selected month tidak reset sembarangan.

#### Budget

- Sisa Budget tampil.
- Terpakai bulan ini benar.
- Total budget dari `BudgetPrefs/budget`.
- Progress bar capped secara visual.
- Percentage badge bisa lebih dari 100%.
- Status card safe/warning/over budget benar.
- Edit budget show/hide.
- Simpan budget menerima angka, titik, `Rp`, spasi.
- Home budget card ikut update setelah kembali.

#### Bottom nav

- Home active benar.
- Pengeluaran active benar.
- Budget active benar.
- Riwayat active benar.
- Pengeluaran label tidak lebih kecil.
- Icon/label tidak shift active/inactive.
- FAB tengah membuka Tambah Transaksi.
- Ripple FAB tidak kotak.

#### Launcher

- App name `Artos`.
- Icon hijau `#306D29`.
- Logo putih centered.
- Tidak ada white square.
- Logo tidak cropped.
- Jika launcher cache membandel, uninstall app dulu lalu install ulang.

#### Build

Build command:

```powershell
.\gradlew.bat :app:assembleDebug --no-configuration-cache --console=plain --no-daemon
```

Jika sandbox menolak akses SDK `android.jar`, itu masalah environment/tooling, bukan otomatis error app. Jalankan dengan akses yang mengizinkan Android SDK.


Sebelum edit:

- Cek apakah ID XML dipakai Java dengan `findViewById`.
- Cek apakah layout dipakai oleh Activity atau Adapter.
- Cek apakah drawable dipakai di halaman lain.
- Pastikan perubahan hanya visual jika tugasnya UI-only.

Saat edit XML:

- Jangan rename ID penting.
- Jangan hapus view yang dicari Java.
- Hati-hati dengan `layout_weight`.
- Hati-hati dengan `clipChildren` dan `clipToPadding`.
- Hati-hati dengan `RecyclerView` padding.
- Cek apakah `MaterialButton` butuh `app:backgroundTint`.
- Cek apakah PNG tidak tertint secara tidak sengaja.

Saat edit Java:

- Jangan ubah stored type string:
  - `Pemasukan`
  - `Pengeluaran`
- Jangan ubah format tanggal database:
  - `d/M/yyyy`
- Jangan ubah schema DB tanpa rencana migration.
- Jangan lupa tutup `Cursor`.
- Jangan ubah `SharedPreferences` key sembarangan:
  - `USERNAME`
  - `budget`
- Jangan ubah urutan column cursor tanpa update pembacaan.

Setelah edit:

- Jalankan build:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain --no-daemon
```

- Test Login.
- Test Home dashboard.
- Test tambah transaksi `Pengeluaran`.
- Test tambah transaksi `Pemasukan`.
- Test tanggal input.
- Test category selector.
- Test Riwayat search.
- Test Riwayat filter chip.
- Test Riwayat sorting/date grouping.
- Test bottom navigation.
- Test tombol plus.
- Test Budget update.
- Test Pengeluaran analysis.
- Buat checkpoint/commit jika semua stabil.

---

## Lampiran: Pola Syntax Umum yang Sering Muncul

### `setContentView(...)`

Menghubungkan Activity dengan XML layout.

```java
setContentView(R.layout.activity_home);
```

### `findViewById(...)`

Mengambil view dari XML.

```java
TextView tvSaldo = findViewById(R.id.tvSaldo);
```

### `setOnClickListener(...)`

Menjalankan aksi saat view diklik.

```java
btnSimpan.setOnClickListener(v -> {
    // aksi
});
```

### `Intent`, `startActivity`, `finish`

Untuk pindah halaman.

```java
startActivity(new Intent(getApplicationContext(), HomeActivity.class));
finish();
```

### `ArrayList<>`

List data dinamis.

```java
ArrayList<Transaction> allTransactions = new ArrayList<>();
```

### `for (...)`

Loop untuk memproses banyak data.

```java
for (Transaction transaction : allTransactions) {
    // proses
}
```

### `if (...)`

Percabangan.

```java
if (nominalText.isEmpty()) {
    return;
}
```

### `try/catch`

Menangani error agar app tidak crash.

```java
try {
    int day = Integer.parseInt(dateParts[0]);
} catch (IllegalArgumentException exception) {
    // tanggal invalid
}
```

### `Cursor`

Objek hasil query SQLite.

```java
Cursor cursor = dbHelper.getAllTransactions();
while (cursor.moveToNext()) {
    // baca data
}
cursor.close();
```

### `SharedPreferences`

Penyimpanan sederhana lokal.

```java
SharedPreferences prefs = getSharedPreferences("BudgetPrefs", MODE_PRIVATE);
```

### `RecyclerView.Adapter`

Penghubung data list ke tampilan item.

Method penting:

- `onCreateViewHolder`
- `onBindViewHolder`
- `getItemCount`
- `getItemViewType`

### XML `android:id`

Nama view agar Java bisa mengambilnya.

```xml
android:id="@+id/rvRiwayat"
```

### XML `layout_width` dan `layout_height`

Ukuran view.

```xml
android:layout_width="match_parent"
android:layout_height="wrap_content"
```

### XML `layout_weight`

Membagi ruang tersisa dalam `LinearLayout`.

```xml
android:layout_height="0dp"
android:layout_weight="1"
```

### XML `clipChildren` dan `clipToPadding`

- `clipChildren`: apakah parent memotong child yang keluar batas.
- `clipToPadding`: apakah isi view dipotong di area padding.

Ini penting untuk layout scroll dan bottom nav.

### XML `app:` attributes

`app:` dipakai oleh library/custom view seperti Material Components atau ConstraintLayout.

Contoh:

```xml
app:backgroundTint="@color/login_button"
```

---
