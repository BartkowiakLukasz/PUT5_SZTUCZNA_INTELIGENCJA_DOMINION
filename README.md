# Dominion AI - ByczkensPlayer 🐂

Projekt sztucznej inteligencji do gry planszowej **Dominion** (abstrakcyjna gra strategiczna). Bot został zoptymalizowany pod kątem szybkiego przeszukiwania drzewa gry oraz zaawansowanej oceny sytuacji na planszy, a jego parametry zostały wyuczone za pomocą algorytmu genetycznego.

## 👨‍💻 Autorzy
* Łukasz Bartkowiak (160219)
* Michał Byczko (160141)

## 🧠 Architektura i Algorytmy

Nasz agent (`ByczkensPlayer`) opiera się na klasycznym podejściu do gier dwuosobowych o sumie zerowej, wzbogaconym o szereg optymalizacji:

1. **Negamax z Alpha-Beta Pruningiem:** Szybkie i zoptymalizowane przeszukiwanie drzewa gry, pozwalające na głębszą analizę wariantów.
2. **Iterative Deepening (Iteracyjne Pogłębianie):** Dynamiczne dostosowywanie głębokości przeszukiwania do dostępnego czasu. Bot nigdy nie przekracza limitu czasowego na ruch, niezależnie od stopnia skomplikowania sytuacji na planszy.
3. **Move Ordering (Sortowanie Ruchów):** Wstępna wycena i sortowanie ruchów przed wejściem w głąb drzewa. Priorytetyzacja klonowania pionków oraz bicia przeciwnika drastycznie poprawia skuteczność odcięć Alpha-Beta.

## ⚖️ Heurystyka Ewaluacji

Funkcja oceniająca stan planszy (tzw. ewaluacja) bierze pod uwagę znacznie więcej niż tylko liczbę pionków:
* **Fazy Gry (Fullness Threshold):** Wartość pionków zmienia się w zależności od tego, jak bardzo zapełniona jest plansza (początek gry vs. endgame).
* **Rogi i Krawędzie:** Ogromne premie za zajmowanie stabilnych pozycji (rogów), których przeciwnik nie może odbić.
* **Mobilność (Mobility):** Analiza liczby pustych pól przylegających do pionków gracza i przeciwnika. Promowanie ograniczania opcji ruchu rywala.
* **Strefy Śmierci (Death Zones):** Unikanie stawiania pionków na polach typu X i C wokół pustych rogów, co zapobiega oddawaniu kluczowych pozycji przeciwnikowi.
* **Struktura (Clustering):** Nagradzanie za tworzenie zwartych, trudnych do przejęcia grup pionków.

## 🧬 Uczenie Maszynowe (Algorytm Genetyczny)

Wszystkie wagi heurystyczne (12 różnych parametrów, m.in. wartości rogów, kar za strefy śmierci, mnożniki bicia) nie zostały dobrane ręcznie. Zamiast tego stworzyliśmy moduł **`Trainer.java`**.

Wykorzystuje on **Algorytm Genetyczny** do optymalizacji:
* Tworzy populację botów z losowymi mutacjami wag.
* Przeprowadza wielowątkowe turnieje (każdy bot gra ze zaktualizowanym mistrzem oraz zestawem innych botów testowych).
* Najlepsze jednostki są poddawane krzyżowaniu (crossover) i mutacjom (mutation), tworząc kolejne, silniejsze pokolenia.
* Plik `ByczkensWeights.java` zawiera ostateczne, wyuczone wagi z naszych nocnych sesji treningowych.
