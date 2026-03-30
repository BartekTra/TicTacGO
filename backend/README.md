# TicTacGO - Dokumentacja Techniczna Serwera (Backend)

TicTacGO to aplikacja typu klient-serwer implementująca grę w kółko i krzyżyk w trybie wieloosobowym czasu rzeczywistego. Poniższa dokumentacja opisuje architekturę, technologie, interfejsy API oraz warstwę mechaniki gry dla części backendowej.

## 1. Stos Technologiczny

Projekt został zrealizowany przy użyciu nowoczesnego stosu technologicznego opartego o język Java.

* **Język:** Java 21
* **Framework Główny:** Spring Boot 3.2.x
* **Baza Danych:** PostgreSQL
* **ORM:** Spring Data JPA / Hibernate
* **Autoryzacja:** Spring Security + JSON Web Tokens (JWT)
* **Komunikacja Real-Time:** Spring WebSocket + STOMP
* **Zarządzanie Zależnościami:** Maven
* **Narzędzia:** Lombok, Dotenv, Jackson

## 2. Architektura i Konwencje

Aplikacja oparta jest na nowoczesnej architekturze warstwowej z podziałem w konwencji *Package by Feature*. Dzieli to system aplikacji serwerowej na poniższe sekcje:

* `auth` - proces uwierzytelniania, walidacja zapytań rejestracyjno-logujących oraz wydawanie tokenów JWT.
* `common` - obsługa wyjątków wyłapywanych przez całą aplikację (`GlobalExceptionHandler`).
* `config` - konfiguracje dla komponentów (CORS, WebSocket, Security, procesy harmonogramu działań czasowych).
* `game` - podstawowa domena odpowiedzialna za serce systemu, zarządzająca partiami i ich przesyłem dla połączonych użytkowników przy wykorzystaniu mechanizmów brokerskich.
* `player` - profilowanie historii jednostki i aktualizowanie wskaźników postępu użytkownika w relacyjnej strukturze bazy.
* `security` - konfiguracje bezpieczeństwa do sprawnej inspekcji ścieżek dostępu.

## 3. Schemat Bazy Danych

System działa na silniku relacyjnym za wykorzystaniem dwóch spójnych zmapowanych encji głównych.

### Tabela: `players` (Moduł graczy)
Przechowuje cykl życia danych identyfikowalnych z profilem zarejestrowanej osoby.
* Pola pomocnicze takie jak unikalne `email` oraz `username`.
* Wskaźniki statystyczne wspierane aktualizacjami inkrementacyjnymi zdefiniowanymi poprzez cykl życia encji: `gamesPlayed`, `gamesWon`.
* Współczynnik zwycięstw (`winRate`) estymowany asynchronicznie poleceniem na poziomie zapytania natywnego bazy jako definicja adnotacyjna `@Formula`.
* Wrażliwy atrybut do sprawdzania dostępu przetrzymywany ze sprzętowym szyfrem hashowania bez składowania (BCrypt).

### Tabela: `games` (Moduł rozgrywek - dziedziczenie Single Table)
Skonfigurowana według zasad polimorfizmu z udostępnioną strukturą bazową poprzez abstrakcję `GameEntity` zachowaną w jednej tabeli poprzez wykorzystanie dyskryminatora `game_type` charakteryzującego odpowiedni stan klasy.
* Zapis pól siatki planszy (`board`) w zserializowanym na rzecz bazy modelu `JSON` optymalizując zapis przebiegu skrótową zawartością bloku informacyjnego i wymijając tworzenie w warstwie logiki niepotrzebnych sprzężeń tabel z relacjami zagranych pozycji rzędowych 1:N.
* Odrębne kolekcjonowanie dla poszczególnej grupy zagranych posunięć (`moves_x`, `moves_o`), wspierających zaawansowane układy trybów np. formatowania skrajności ruchu nieskończonego.
* Przechowywanie statusów na wypadek walidacji chronologicznej dla schedulera analizującego bezczynności (czas rozpoczęcia, opuszczenie itp.) do weryfikatora statusu `WAITING_FOR_OPPONENT`, `IN_PROGRESS`, `DRAW`, `FINISHED`.

## 4. Bezpieczeństwo i Autoryzacja

Architektura nie opiera się o tradycyjne zarządzanie zrównolegloną sesją pamięci HTTP. Ruch zabezpieczono mechanikami bezstanowymi JWT dla obydwu zdefiniowanych osnowach w komunikacjach z rest API.

1. Wysłanie danych pod kluczowe operatory weryfikujące generuje dla zlokalizowanych sesyjnych identyfikatorów z serializowany wpis jako token do nośnika `Cookie`. Format generuje flagę bezpieczną dla klienta ze specyfikacją flag miedzy innymi wpisanej konfiguracji blokującej wczesne uprowadzenia poprzez modyfikacje dla Javascript mianowicie atrybut: `HttpOnly`. 
2. Przepuszczenie przez obwód filtrujący w systemie wyłapuje specyfikę autoryzowanego powiązania dostarczając model operacyjny zidentyfikowanemu `Principal`.
3. Dla protokołu obarczonego brakiem naturalnej konstrukcji standardów dla uwierzytelniania połączeń websocket wprowadzono przechwycenie u wyciągającego odpowiedni model na interfejsie handshakingu z odczytanego kontekstu pod poświadczeniem przed otwarciem aktywnej transmisji podkanałowej użytkownika dla STOMP z mapowanego we wcześniejszym zaszłościach na żądanie logowania cookie-storage dla identyfikatora: `jwt`.

## 5. Endpoints i System Komunikacji API

Punktacja dla udostępnień wywoławczych dzieli się na wejściowe API formatowane REST, oraz połączenia gniazd (WebSockets) implementowanych dla ujednoliconego wsparcia na poziomie sub warstwy brokera wysyłkowego protokołu komunikatu.

### REST API

| Metoda | Ścieżka (Endpoint) | Działanie | Autoryzacja |
|--------|---------------------|---------------|-------------|
| `POST` | `/api/v1/auth/register` | Dokonuje walidacji i procedowania kreacji nowych struktur profili wpisując po potwierdzeniu nowe dane w token sesji. | Odkryty |
| `POST` | `/api/v1/auth/login` | Zatwierdzenie autoryzacyjne sesji - generacja identyfikatora wejściowego po poprawnym zaszyfrowaniu i udowodnieniu. | Odkryty |
| `GET` | `/api/v1/players/me` | Wgląd na strukturalne rozszerzone profile parametrów odzyskanego zalogowanego klienta z bazowymi składowymi z puli zwycięstw dla interfejsu klienta front. | Wymagana |
| `POST` | `/api/v1/game/join` | Dopasowanie (Matchmaking). Dołączanie wolnych ról do gotowego i czyszczonego z zewnątrz elementu na rzecz wdrożenia mechanizmu lub stworzenia osobnego pokoju, kiedy kolejki do sparowania zostały oczyszczone. Przenosi tryb jako zmienną argumentacji URL `mode` dla wyznaczenia struktury wywoławczej z domyślnym nadpisem rzutującym tryb standardowy. | Wymagana |
| `POST` | `/api/v1/game/leave` | Zamknięcie przypisania instancji partii gracza reagującej powiadomieniami o rezygnacyjnym zwinięciu gry z wpisem przesyłu porażek/zwycięstw w aktualizowaniu flag porzucającym widoki statusowe rywała.  | Wymagana |

### Komunikacje STOMP (WebSocket)

Nawiązywanie otwarcia sesji dokonywane asynchronicznym wstrzykiwaniem pod wejściowo udostępniony endpoint autoryzacji komunikacji: `/ws-game`
* `/app/game.move` - Wysłanie danych typu `MoveDTO` na docelowe przesłanie serwera obarczając logicznie zatrzymane miejsce ruchem przypisanym kontekstowi nadawcy. Operacja wywołujące wewnętrzną obsługę zmian na tablicach operujących rozgrywek dla zasady obydwu gier weryfikujących błędy (podrzucone następnie bezpośrednio kolejce subskrybującej błędne procedery) i udostępniając udany format. Wychwytuje mechanika rzucając wyjątkiem bądź ureguluje zmianą wejścia.
* `/topic/game.{gameId}` - Miejsce odbiorcze zdarzeń od brokera nadającej podgrupom w rejonie odgórnie założonego pokoju id na wprowadzony rzucony proces operacyjny. Nasłuch informujący o zmienionym przelotnym procesie zasobu `GameEntity` modyfikowanego na instancjach.
* `/user/queue/errors` - Unikalny wpis kierunkowego wysyłania sygnałów i błędów od przechwytującego modułu w przypadkach naruszenia procedur takich jak wymuszenia i obejścia mechaników poza swoją pozycją nadaną turą czy fałszowanie indeksów wymiarowych.

## 6. Realizacja Typów Instancji Rozgrywek

Architektura projektowa nadawała regule unikania duplikacji operacji. Utworzona generalna abstraktywna warstwa klasy integruje zachowania specyficznie modyfikowane pod tryb wybranej mechaniki:

1. **Classic Mode**
Rozgrywka w trybie bazującym na zasadach nadrzędnych dla standardowej mechaniki zajęcia osiem skrajności warunkujących dopasowanie zwycięstwa. Dokłada weryfikację sprawdzeń pod rzucające remisem bezwyjściowe sytuacje pełnych tablic.
2. **Infinite Mode**
Alternatywna nakładka specyficznego układu wymazywana po maksymalnej uregulowanej przetrzymywanej stałej (kolejek zagnieżdżonych na historię trójek). Modyfikuje wektor dodając zabezpieczenie chronologicznie najstarszego zapisanego wejścia u gracza wymuszając cykliczne przenoszenie siatki - zachowuje element ciągłego dążenia do ułożenia bez wymogów czyszczenia i deklarowania remisu ze stanowczych ułożeń do zwycięskich złożeń wzorów.

## 7. Skrypty Reakcyjne - Harmonogramowanie
Logiki na poziomach baz relacyjnych w zabezpieczeniu z bezużyteczności i wylogowaniem nagłym graczy (nie przesyłających wymogu zamknięcia), realizowane procesem zadaniowym `@Scheduled` cyklicznego uruchomienia u komponentu procesującego (Klasa `GameTimeoutScheduler`):
* Uwalnianie zasobów zablokowanych przetrzymujących zaproszenia od nadawcy z brakiem finalnej integracji. Zezwala to systemowi w czyszczeniach na zamykanie długo zaległych kolejek serwerowych, redukując problem zombie wpisów.
* Kończenie bezprawnej zwłoki oczekiwań (po nadzorowaniu braku ruchów obydwu wchodzących po przełożeniach instancyjnych limitowanych z użyciem czasowych zapisanych atrybucji na tury przychodzące). Zamyka powołując przegrywając stronę pasywną za nieaktywne podjęcie narzuconej walki przez Timeout. Zwróci informacje przeliczaniem punktowym po wykonaniu nadpisu na odpowiednią instancje subskrybowanych powiadomień rzucających zwycięstwa przeciwległej powołanej rywalizacji w grupie użytkowników uczestniczących pod rządzoną grupą.

## 8. Proces Uruchomienia i Zmienne Architekturalne

Całkowite uwarunkowania oparto wykorzystując właściwości zarządzanych pomniejszych zależności środowisk i konfiguracyjnych adnotacji rozszerzeń: `application.properties/yml`. Narzuca to uwarunkowany dostęp poprzez wprowadzony lokalny proces definicji chronione w postaci odnalezionego pliku środowiskowego: W katalogu głównym projektu (katalog `backend`) używano z modułem Dotenv definiowania wymaganych zmiennych wejścia portów DB dla lokalnie zbudowanej maszyny nasłuchującej połączenia psql.

**Kroki konfiguracyjno-kompilacyjne:**
Z pakietem lokalnym wykonujemy skrypt wprowadzający zależność, i odwołując do udostępnionego zarządcy powiązanych pakietów wykonujemy na skrypcie systemowym wprowadzającego w system główny instruktaż budowy i ostatecznego uruchamiającego wpisu do domyślnego wejścia dla podpiętych portów (8080).

```bash
# Pozycjonujemy się w folderze backend i kompilujemy na budowę instancji wyjściowej po pobraniach pakietów dla ułatwionej formy instalacyjnej i wyłączamy standardowe sprawdzające procesy unitowe z powłoki pominięcia:
./mvnw clean install -DskipTests

# Startujemy wyznaczoną grupę do zapalonego konteneru dla osadzenia w serwerze sprigbootowym na wyjściowym module web dla otwarcia operacji:
./mvnw spring-boot:run
```
