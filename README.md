# Frigiderul Meu

Aplicație Android simplă pentru alimentele din frigider. Fotografiile produselor apar **direct în calendarul lunar**, în ziua în care expiră.
Funcționează complet offline: fără cont, fără reclame, fără internet. Datele rămân doar pe telefon.

## Ce face versiunea 0.1

- **Calendar lunar** (săptămâna începe luni) cu miniaturile produselor direct în căsuțe: până la 4 în modul compact, 2 mai mari în modul extins, plus „+N” dacă sunt mai multe. Navigarea se face cu săgeți sau prin glisare. Atingerea unei zile deschide lista produselor din ziua aceea.
- **Culori de expirare**, recalculate automat la schimbarea zilei:
  - verde: expiră peste mai mult de 2 zile;
  - portocaliu: expiră azi, mâine sau peste 2 zile;
  - roșu, cu semnul „!”: expirat și încă neconsumat.
- **Produse deschise**: un comutator „Marchează ca deschis”, care poate fi anulat. Starea apare ca eticheta albastră „DESCHIS” în inventar și ca un cerc albastru cu „D” pe miniatura din calendar. Nu schimbă data expirării și nu scoate produsul din inventar.
- **Inventar tip galerie foto**: grilă de ~3 coloane, sortată după expirare sau după data adăugării.
- **Pagina produsului**: poți schimba fotografia, numele, cantitatea și data. Poți marca produsul ca deschis sau consumat (cu „Anulează”) și îl poți șterge.
- **Adăugare rapidă**: fotografie cu camera sau din galerie, nume, dată (cu scurtături „Mâine”, „+3”, „+7”…), cantitate opțională. Butonul „Salvează și adaugă încă unul la fel” e pentru același produs cu altă dată de expirare.
- **Fotografii mici**: fiecare poză e redusă la max. 400 px, salvată ca WebP fără metadate în spațiul privat al aplicației. Originalul de la cameră se șterge imediat.

## Ce urmează (etapa 2)

- Scanare cod de bare, offline (structura bazei de date este deja pregătită: tabelul `product_types`).
- Notificări cu 2 zile înainte și în ziua expirării (WorkManager, o verificare pe zi, fără serviciu permanent).
- Backup/restaurare manuală.

## Cum obții APK-ul

La fiecare modificare urcată pe GitHub, aplicația se compilează automat:

1. Deschide fila **Actions** a acestui repo.
2. Intră în ultima rulare „Compilează APK”, marcată cu bifă verde.
3. Jos, la **Artifacts**, descarcă **FrigiderulMeu-apk** (o arhivă .zip) și dezarhiveaz-o pe telefon.
4. Deschide `FrigiderulMeu.apk`. Android îți va cere permisiunea de a instala aplicații din acea sursă (browser sau manager de fișiere).

Toate APK-urile sunt semnate cu aceeași cheie de test (`app/frigider-test.jks`), deci o versiune nouă se instalează peste cea veche și **datele se păstrează**. Cheia este publică în repo, așa că instalează APK-uri doar din fila Actions a acestui repo.

## Compilare locală (opțional)

Deschide proiectul în Android Studio și apasă Run, sau din terminal:

```
./gradlew assembleRelease     # APK: app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest   # testele pentru date, culori și stări
```

## Tehnologie

Kotlin, Jetpack Compose (Material 3), Room/SQLite, ViewModel + StateFlow, Coroutines. Minim Android 8.0 (API 26).
Fără Firebase, fără biblioteci de rețea, fără permisiunea INTERNET. Camera se folosește prin aplicația de cameră a telefonului, deci nu este necesară permisiunea CAMERA.

Structura codului (`app/src/main/java/ro/frigiderulmeu/`):

- `data/`: entitățile Room (`Item` = exemplar concret, `ProductType` = tip identificat prin cod de bare), DAO-uri, baza de date.
- `domain/`: calculul culorilor (`Expiry`), grila calendarului (`CalendarMonth`), schimbările de stare (`ItemActions`), data curentă (`Today`).
- `image/`: comprimarea fotografiilor (`ImageStore`) și afișarea eficientă a miniaturilor, cu cache (`ProductImage`).
- `ui/`: ecranele Calendar, Frigider, Adaugă, pagina produsului, tema.
