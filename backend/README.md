# TicTacGO - Dokumentacja Techniczna Serwera (Backend)

TicTacGO to aplikacja typu klient-serwer implementująca grę w kółko i krzyżyk w trybie wieloosobowym czasu rzeczywistego. Poniższa dokumentacja opisuje architekturę, technologie, interfejsy API oraz warstwę mechaniki gry dla całkowicie zmodernizowanej części backendowej.

## 1. Stos Technologiczny

Projekt został zrealizowany przy użyciu nowoczesnego stosu technologicznego opartego o język Java.

* **Język:** Java 21
* **Framework Główny:** Spring Boot 3.2.x
* **Baza Danych:** PostgreSQL
* **ORM:** Spring Data JPA / Hibernate
* **Autoryzacja:** Spring Security 6 + JSON Web Tokens (JWT)
* **Komunikacja Real-Time:** Spring WebSocket + STOMP
* **Zarządzanie Zależnościami:** Maven
* **Narzędzia:** Lombok, Dotenv, Jackson

## 2. Architektura i Konwencje

Aplikacja oparta jest na wysoce rozdzielonej architekturze warstwowej z podziałem w konwencji *Package by Feature*. 
Główną i strategiczną zmianą jest **Architektura Anemicznego Modelu Domenowego** skrzyżowana z potężnymi serwisami logiki (*GameEngine*). Decyzja ta pozwoliła na całkowite oddzielenie logiki gry od obiektów komunikujących się z bazą danych (JPA).

* `auth` - proces uwierzytelniania, walidacja zapytań rejestracyjno-logujących oraz wydawanie tokenów JWT.
* `common` - scentralizowana obsługa wyjątków wyłapywanych przez całą aplikację za pomocą konwencji standardowych DTO (`GlobalExceptionHandler`).
* `config` - konfiguracje dla komponentów (rozszerzony panel rygorystycznego CORS, WebSocket z Channel Interceptors, Security z autoryzacją nagłówkową).
* `game` - podstawowa domena odpowiedzialna za serce systemu. Oddziela modele persystencji (anemiczne *Entities*) od potężnego `GameEngine` w warstwie *Service*. Używa restrykcyjnych DTO (`GameResponseDTO`) zapobiegając wyciekaniom stanu bazy na klienta REST/STOMP.
* `player` - profilowanie historii jednostki i aktualizowanie wskaźników postępu użytkownika.
* `security` - konfiguracje bezpieczeństwa Stateless (całkowity brak sesji).

## 3. Schemat Bazy Danych i Struktura Gry

System działa na silniku relacyjnym. Modele zostały silnie zoptymalizowane do szybkich operacji dyskowych.

### Tabela: `players` (Moduł graczy)
Przechowuje profile zarejestrowanych osób.
* Wskaźniki statystyczne wspierane aktualizacjami po każdej zamkniętej partii: `gamesPlayed`, `gamesWon`.
* Współczynnik zwycięstw (`winRate`) estymowany na bazie adnotacji `@Formula`.
* Wrażliwy atrybut ukryty za szyfrem jednokierunkowym (BCrypt).

### Tabela: `games` (Single Table Inheritance)
Silnik gier zmapowany jest na ryczałtową strategię *Single Table* dla szybkiego dostępu, zarządzaną flagą dyskryminatora `game_type` (tryb `CLASSIC` lub `INFINITE`). 
* **Rewolucja Reprezentacji:** Cała tablica stanu rozgrywki (`board`) przetrzymywana jest w formie czystego, 9-znakowego wiersza `String` (np. `"XXO-O-X--"`) w celu oszczędności odczytów i parsowania dużych obiektów JSON.
* Odrębne kolekcjonowanie ułożenia historycznych ruchów graczy na poszczególnej osi (`moves_x`, `moves_o`). Stanowi ono klucz dla odczytów przesuwnych w trybie *Infinite*.
* **Współbieżność:** Optymalizacja procesów szukania przeciwników weryfikowana jest dzięki zapiętom transakcyjnym bazy używając `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Odporne na klonowanie sesji zabezpieczenie nakłada *Retry Mechanisms*, jeśli kilku graczy spróbuje obsadzić to samo lobby równolegle.

## 4. Bezpieczeństwo i Uwierzytelnianie

Architektura przeszła migrację w pełni w kierunku czystego, bezstanowego rozwiązania `Bearer JWT`, odrywając system od wrażliwych Ciasteczek (*Cookies*) uwydatnionych na ryzyko CSRF.

1. **REST API (Autoryzacja Nagłówkowa):** Przepuszczenia operacji chronionych wymagają jawnego uregulowania uwierzytelnienia przez wstrzyknięcie żądania HTTP z twardym nagłówkiem: `Authorization: Bearer <token>`. Do transferu bezpiecznego korzysta on z globalnej, elastycznej siatki w `WebConfig` uwzględniającej `Access-Control-Expose-Headers`.
2. **REST API (Token w Ciele):** W trosce o najwyższą sprawność warstwy *Frontend-to-Backend*, kontroler autoryzacyjny odpowiada płasko zwracając wydany klucz cyfrowy prosto w wyjściowej kalibracji Ciała (`Body`) obiektu JSON, zabezpieczając apkę przed usuniętymi nagłówkami przez środowiska proxy.
3. **STOMP (Channel Interceptors):** Konstrukcja Spring Boot WebSocket wyklucza bezpośrednie posłanie tokena HTTP headersem. Czystość połączenia uzyskaliśmy wyłapując specjalistyczny event `CONNECT` za pomocą rozbudowanego `ChannelInterceptor`, który odzyskuje z osnowy weryfikator `Bearer` a po certyfikacji, przypisuje profil `Principal` do osi transmisji na żywo.

## 5. Endpoints i System Komunikacji API

### REST API

| Metoda | Ścieżka (Endpoint) | Działanie | Autoryzacja |
|--------|---------------------|---------------|-------------|
| `POST` | `/api/v1/auth/register` | Zapis oraz wydanie cyfrowego klucza tokenizacji JWT w ciele zwrotnym (JSON). | Odkryty |
| `POST` | `/api/v1/auth/login` | Sprawdzenie poprawności układu identyfikującego i nadanie tokena z obiektem encji dla ramy *Frontend*. | Odkryty |
| `GET` | `/api/v1/players/me` | Wgląd w strukturalne rozszerzone dane zalogowanego klienta niezbędne do zarządzania profilem. | Wymagana |
| `POST` | `/api/v1/game/join` | Dopasowanie (Matchmaking). Transakcyjne złączenie z pierwszym wolnym lobby lub tworzenie dedykowanego w przypadku braku otwartego slotu. Implementuje retry w zderzeniach w modelu *Pessimistic*. | Wymagana |
| `POST` | `/api/v1/game/leave` | Poddanie partii i opuszczenie gniazda przydziału powodujące odgórne nadanie porażki serwerowej. | Wymagana |

### Komunikacje STOMP (WebSocket) -> Endpoint: `/ws-game`

* **IN:** `/app/game.move` - Kapsuła nasłuchu dla ruchu klientów. W ciele DTO znajduje się `gameId` i `position`. Trafia do niezależnego `GameEngine`, aby zweryfikować stan, unieważnić ruch w przypadku błędu (chronologia tur) i poddać aktualizacji na domyślny strumień powiadomieniowy.
* **OUT (Topic):** `/topic/game.{gameId}` - Miejsce odbiorcze zdarzeń dla strumieni rozgłoszeniowych. Udostępnia silnie stypizowany `GameResponseDTO`, kryjąc hasła, pełną historię meczów czy status i wygranych dla klienteli.
* **OUT (Queue):** `/user/queue/errors` - Unikalny wpis kierunkowy prywatnego błędu. Nieoczekiwane ruchy jak zły slot czy ruch przed własną turą nie zwracają czystych Exception'ów, a wysyłają do indywidualnego abonenta poprawny obiekt `GameErrorDTO` do wyrenderowania *Toasta* przez Reagujący Interfejs.

## 6. Realizacja Typów Mechanik Rozgrywek (`GameEngine`)

Usunięcie logicznego bałaganu pozwoliło w pełni osadzić proces w jednym obiekcie silnikowym (`GameEngine`) operacyjnym na ciągach znakowych (String):

1. **Classic Mode (`ClassicGameEntity`)**
Zwykły przebieg, dopasowanie pod standardową flagę zwycięstwa na liniach matematycznych (2D w indeksach liniowych). Gdy tablica (`---------`) zapełnia się całkowicie i nie rodzi rozstrzygnięcia – aplikacja rejestruje `DRAW` (remis).
2. **Infinite Mode (`InfiniteGameEntity`)**
Eksperymentalny format ożywający grę i zmuszający do niekończącego kalkulowania. Mechanika dopuszcza zrzut do tablicy `movesX / movesO`. Jeżeli gracz osiąga prób zdefiniowanych znakami > 3, stary wpis (czyli pierwszy zakolejkowany) przelatuje na znak wymazywany (`-`). Brak tutaj wystąpień zjawiska remisowania partii.

## 7. Skrypty Reakcyjne - Harmonogramowanie

*Klasa `GameTimeoutScheduler`* z wykorzystaniem `TaskScheduler`:
Zabezpiecza przed tak zwanym porzucaniem okien, czyli procesem zombie gier w tle. Moduł pilnuje stałości stanu `WAITING_FOR_OPPONENT` a w przypadku przeciągnięć wyczulonych limitów czasowych – nadsyła komendę likwidacji osieroconego wiersza. W statusie `IN_PROGRESS` gracz ociągający się (po wyjściu limitu czasu) zostanie zdegradowany automatycznie ogłoszeniem zwycięstwa gracza naprzeciw. Harmonogram korzysta z elastycznych struktur z instruktażem na podstawie wpisów `Date` mapowanych do czystego formatu Java 8 Time `Instant()`.

## 8. Uruchomienie

1. Wygeneruj paczkę poprzez instalację Maven, która jest obarczona całkowicie wyizolowanymi poprawnie pokrywającymi unit testami operacyjnymi (`MockMVC, AssertJ, Mockito`). Czysta instalacja:
```bash
./mvnw clean install
```
2. Skonfiguruj postgreSQL oraz uruchom na odgórnym łączu gniazda portalu 8080:
```bash
./mvnw spring-boot:run
```
*(Serwer posiada skonfigurowany system powiadomień Logów w `@Slf4j` monitorujący każdy etap cyklu życia gniazd WebSocket oraz ruchu autoryzacji).*
