# Frigiderul Meu

[![Compilează APK](https://github.com/laur23212/Frigiderul-Meu/actions/workflows/build.yml/badge.svg)](https://github.com/laur23212/Frigiderul-Meu/actions/workflows/build.yml)
![Platformă](https://img.shields.io/badge/platform%C4%83-Android%208.0%2B-3DDC84)
![Versiune](https://img.shields.io/badge/versiune-0.1.0-blue)
![Offline](https://img.shields.io/badge/offline-100%25-success)
![Licență](https://img.shields.io/badge/licen%C8%9B%C4%83-MIT-yellow)

**Un calendar al frigiderului tău.** Faci o poză produsului, alegi data la care expiră, iar poza apare în calendar exact în ziua aceea. Dintr-o privire vezi ce trebuie consumat curând și ce s-a stricat deja.

Aplicația este mică, nu consumă baterie și funcționează complet offline: fără cont, fără reclame, fără internet. Datele rămân doar pe telefon.

## De ce există

Aplicațiile de „food tracker” de pe Google Play sunt de obicei încărcate: categorii, calorii, conturi, reclame. Frigiderul Meu face un singur lucru: te ajută să nu arunci mâncare uitată în frigider. Seamănă cu un calendar de perete pe care lipești poze, nu cu o bază de date nutrițională.

## Ce face aplicația

### Versiunea 0.1 (disponibilă)

- **Calendar lunar** (săptămâna începe luni) cu miniaturile produselor direct în căsuțe: până la 2 miniaturi mari în modul compact, 3 în modul extins, plus „+N” dacă sunt mai multe. Navigarea se face cu săgeți sau prin glisare. Atingerea unei zile deschide lista produselor din ziua aceea.
- **Culori de expirare**, recalculate automat la schimbarea zilei:
  - 🟢 verde: expiră peste mai mult de 2 zile;
  - 🟠 portocaliu: expiră azi, mâine sau peste 2 zile;
  - 🔴 roșu, cu semnul „!”: expirat și încă neconsumat.
- **Produse deschise**: un comutator „Marchează ca deschis”, care poate fi anulat. Starea apare ca eticheta albastră „DESCHIS” în inventar și ca un cerc albastru cu „D” pe miniatura din calendar. Nu schimbă data expirării și nu scoate produsul din inventar.
- **Inventar tip galerie foto**: grilă de ~3 coloane, sortată după expirare sau după data adăugării. Fără categorii.
- **Pagina produsului**: poți schimba fotografia, numele, cantitatea și data. Poți marca produsul ca deschis sau consumat (cu „Anulează”) și îl poți șterge.
- **Adăugare rapidă**: fotografie cu camera sau din galerie, nume, dată (cu scurtături „Mâine”, „+3”, „+7”…), cantitate opțională. Butonul „Salvează și adaugă încă unul la fel” e pentru același produs cu altă dată de expirare.
- **Fotografii mici**: fiecare poză e redusă la max. 400 px, salvată ca WebP fără metadate în spațiul privat al aplicației. Originalul de la cameră se șterge imediat. APK-ul are în jur de 1,3 MB.

### Versiunea 0.2 (în lucru)

- **Loc de păstrare**: Frigider, Congelator sau Loc uscat (dulciuri etc.), prin bife la adăugare. Pagina „Frigiderul meu” arată implicit toate produsele, cu filtre pentru fiecare categorie.
- **Deschis = 3 zile**: la un produs din frigider, deschiderea mută expirarea la azi + 3 zile (sau mai devreme, dacă data inițială e mai aproape). La mai multe bucăți, „Deschide o bucată” desparte bucata deschisă într-o intrare separată. La Congelator și Loc uscat data rămâne la fel.
- **Consumat mai ușor**: buton cu bifă pe fiecare produs din inventar și din lista unei zile; la mai multe bucăți consumă una, cu „Anulează”.
- **Poza în pătrat**: după cameră sau galerie, poza se încadrează într-un pătrat (mutare cu degetul, mărire prin ciupire), exact cum apare în aplicație.
- **Date în format zi, lună, an**, cu anul afișat peste tot.

### Ce urmează (etapa 2)

- [ ] Scanare cod de bare, offline (structura bazei de date este deja pregătită: tabelul `product_types`).
- [ ] Notificări cu 2 zile înainte și în ziua expirării (WorkManager, o verificare pe zi, fără serviciu permanent).
- [ ] Backup și restaurare manuală.

## Confidențialitate

- Nu există cont, autentificare sau sincronizare în cloud.
- Aplicația **nu cere permisiunea INTERNET**, deci nu poate trimite nimic în afara telefonului.
- Camera se folosește prin aplicația de cameră a telefonului, deci nu este necesară nici permisiunea CAMERA.
- Fotografiile și datele sunt stocate în spațiul privat al aplicației. Dezinstalarea aplicației le șterge.
- Fără Firebase, fără statistici, fără urmărire.

## Cum obții APK-ul

La fiecare modificare urcată pe GitHub, aplicația se compilează automat:

1. Deschide fila **Actions** a acestui repo.
2. Intră în ultima rulare „Compilează APK”, marcată cu bifă verde.
3. Jos, la **Artifacts**, descarcă **FrigiderulMeu-apk** (o arhivă .zip) și dezarhiveaz-o pe telefon.
4. Deschide `FrigiderulMeu.apk`. Android îți va cere permisiunea de a instala aplicații din acea sursă (browser sau manager de fișiere).

Toate APK-urile sunt semnate cu aceeași cheie de test (`app/frigider-test.jks`), deci o versiune nouă se instalează peste cea veche și **datele se păstrează**. Cheia este publică în repo, așa că instalează APK-uri doar din fila Actions a acestui repo.

> Aplicația nu este (încă) publicată pe Google Play. Pentru publicare ar fi nevoie de o cheie de semnare privată.

## Compilare locală (opțional)

Deschide proiectul în Android Studio și apasă Run, sau din terminal (JDK 17):

```
./gradlew assembleRelease     # APK: app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest   # testele pentru date, culori și stări
```

## Tehnologie

Kotlin, Jetpack Compose (Material 3), Room/SQLite, ViewModel + StateFlow, Coroutines. Minim Android 8.0 (API 26), țintește Android 15 (API 35).

Structura codului (`app/src/main/java/ro/frigiderulmeu/`):

- `data/`: entitățile Room (`Item` = exemplar concret, `ProductType` = tip identificat prin cod de bare), DAO-uri, baza de date.
- `domain/`: calculul culorilor (`Expiry`), grila calendarului (`CalendarMonth`), schimbările de stare (`ItemActions`), data curentă (`Today`).
- `image/`: comprimarea fotografiilor (`ImageStore`) și afișarea eficientă a miniaturilor, cu cache (`ProductImage`).
- `ui/`: ecranele Calendar, Frigider, Adaugă, pagina produsului, tema.

## Atribuiri și mulțumiri

Aplicația este construită pe proiecte open source. Mulțumiri echipelor din spatele lor:

| Componentă | Folosită pentru | Licență |
|---|---|---|
| [Kotlin](https://kotlinlang.org/) și Coroutines | limbajul aplicației, lucru asincron | Apache 2.0 |
| [Jetpack Compose și Material 3](https://developer.android.com/jetpack/compose) | interfața | Apache 2.0 |
| [Room](https://developer.android.com/training/data-storage/room) | baza de date locală | Apache 2.0 |
| [AndroidX](https://developer.android.com/jetpack/androidx) (Core, Activity, Lifecycle, ExifInterface) | componente de bază Android | Apache 2.0 |
| [Material Icons](https://fonts.google.com/icons) | pictograme | Apache 2.0 |
| [KSP](https://github.com/google/ksp) | generare de cod pentru Room | Apache 2.0 |
| [JUnit 4](https://junit.org/junit4/) | teste unitare | EPL 1.0 |

Aplicația a fost dezvoltată cu ajutorul asistentului [Claude](https://claude.ai) de la Anthropic, sub îndrumarea autorului, care a decis ideea, funcțiile și designul.

„Android” este marcă înregistrată a Google LLC. Acest proiect nu este afiliat Google.

## Contribuții

Proiectul este personal, dar sugestiile sunt binevenite. Ai găsit o greșeală sau ai o idee? Deschide un [Issue](https://github.com/laur23212/Frigiderul-Meu/issues) și descrie ce s-a întâmplat sau ce ți-ar plăcea să facă aplicația. Pentru modificări de cod, deschide întâi un Issue, apoi un Pull Request.

## Licență

Proiectul este open source, sub licența [MIT](LICENSE): poți folosi, copia, modifica și distribui codul liber, atât timp cât păstrezi mențiunea de copyright. Aplicația este oferită „ca atare”, fără garanții.

## Autor

Creat de [@laur23212](https://github.com/laur23212).
